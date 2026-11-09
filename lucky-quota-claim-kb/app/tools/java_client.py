from __future__ import annotations

import json
import logging
import urllib.error
import urllib.request

from app.core.config import settings

logger = logging.getLogger(__name__)


def _post(path: str, payload: dict | None = None) -> dict | None:
    base = settings.java_kb_base_url.rstrip("/")
    url = f"{base}{path}"
    body = json.dumps(payload or {}).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=settings.tool_http_timeout) as resp:
            raw = resp.read().decode("utf-8")
            parsed = json.loads(raw)
            if not parsed.get("success"):
                return None
            return parsed.get("data")
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        logger.warning("kb tool call failed path=%s err=%s", path, exc)
        return None


def goods_search(keyword: str, page_size: int = 6) -> dict | None:
    return _post("/internal/kb/tools/goods-search", {"keyword": keyword, "pageSize": page_size})


def shopping_guide() -> dict | None:
    return _post("/internal/kb/tools/shopping-guide", {})
