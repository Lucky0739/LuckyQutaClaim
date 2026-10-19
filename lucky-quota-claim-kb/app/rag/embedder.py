"""文本向量：配置密钥后走 DashScope text-embedding-v4，否则用本地哈希向量（维度同样 1536）。"""

from __future__ import annotations

import hashlib
import logging
import math
import time

import httpx

from app.core.config import settings
from app.core.text import tokenize

logger = logging.getLogger(__name__)

EMBEDDING_DIM = settings.embedding_dim
_MAX_RETRIES = 5


def embed_texts(texts: list[str]) -> list[list[float]]:
    if not texts:
        return []
    if settings.embedding_api_key.strip():
        return _remote_embed(texts)
    return [_hashed_embedding(text) for text in texts]


def embed_query(text: str) -> list[float]:
    return embed_texts([text])[0]


def _remote_embed(texts: list[str]) -> list[list[float]]:
    url = settings.embedding_base_url.rstrip("/") + "/embeddings"
    headers = {"Authorization": f"Bearer {settings.embedding_api_key}"}
    batch_size = max(1, settings.embedding_batch_size)
    vectors: list[list[float]] = []
    with httpx.Client(timeout=120.0) as client:
        for start in range(0, len(texts), batch_size):
            batch = texts[start : start + batch_size]
            data = _request_batch(client, url, headers, batch, start)
            vectors.extend(
                item["embedding"] for item in sorted(data, key=lambda row: row["index"])
            )
    return [_pad(vector) for vector in vectors]


def _request_batch(
    client: httpx.Client,
    url: str,
    headers: dict[str, str],
    batch: list[str],
    offset: int,
) -> list[dict]:
    payload = {
        "model": settings.embedding_model,
        "input": batch,
        "dimensions": EMBEDDING_DIM,
        "encoding_format": "float",
    }
    last_error: Exception | None = None
    for attempt in range(_MAX_RETRIES):
        try:
            response = client.post(url, headers=headers, json=payload)
            if response.status_code == 429:
                last_error = httpx.HTTPStatusError(
                    "向量模型触发限流", request=response.request, response=response
                )
                time.sleep(min(2 ** attempt, 20))
                continue
            response.raise_for_status()
            return response.json()["data"]
        except httpx.HTTPError as ex:
            last_error = ex
            time.sleep(min(2 ** attempt, 20))
    logger.error("调用向量模型失败 model=%s offset=%s", settings.embedding_model, offset)
    raise RuntimeError(f"向量模型调用失败: {last_error}") from last_error


def _hashed_embedding(text: str) -> list[float]:
    vec = [0.0] * EMBEDDING_DIM
    for token in tokenize(text) or {"_"}:
        digest = hashlib.md5(token.encode("utf-8")).digest()
        index = int.from_bytes(digest[:4], "big") % EMBEDDING_DIM
        sign = 1.0 if digest[4] % 2 == 0 else -1.0
        vec[index] += sign
    return _normalize(vec)


def _pad(vector: list[float]) -> list[float]:
    if len(vector) == EMBEDDING_DIM:
        return vector
    if len(vector) > EMBEDDING_DIM:
        return vector[:EMBEDDING_DIM]
    return vector + [0.0] * (EMBEDDING_DIM - len(vector))


def _normalize(vector: list[float]) -> list[float]:
    norm = math.sqrt(sum(value * value for value in vector)) or 1.0
    return [value / norm for value in vector]
