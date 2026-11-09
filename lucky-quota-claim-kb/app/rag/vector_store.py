from __future__ import annotations

from dataclasses import dataclass
from threading import Lock

from pymilvus import DataType, MilvusClient

from app.core.config import settings
from app.ingestion.chunker import ChildChunk
from app.schemas import DocumentCatalogItem

_OUTPUT_FIELDS = [
    "document_id",
    "document_name",
    "knowledge_scope_code",
    "business_category",
    "chunk_index",
    "parent_id",
    "parent_index",
    "child_start",
    "heading_path",
    "content",
    "parent_content",
]

# 多个 VectorStore 实例共享同一个集合，建集合阶段需要串行化
_ENSURE_LOCK = Lock()


@dataclass
class StoredChunk:
    document_id: int
    document_name: str
    knowledge_scope_code: str
    business_category: str
    chunk_index: int
    content: str
    score: float
    parent_id: str = ""
    parent_index: int = 0
    child_start: int = 0
    heading_path: str = ""
    parent_content: str = ""


class VectorStore:
    def __init__(self) -> None:
        self._client: MilvusClient | None = None

    def replace_document(
        self,
        document_id: int,
        document_name: str,
        knowledge_scope_code: str,
        business_category: str,
        children: list[ChildChunk],
        embeddings: list[list[float]],
    ) -> int:
        """写入子块；父块内容随子块冗余存储，命中后无需二次查询即可还原上下文。

        单集合方案以少量存储冗余换取检索链路简单、无跨集合关联失败。父块内容
        只在子块行里重复，单行大小仍远低于 Milvus 限制。
        """
        client, name = self._ensure_collection()
        self.delete_document(document_id)
        rows = [
            {
                "document_id": int(document_id),
                "chunk_index": int(child.chunk_index),
                "parent_id": (child.parent_id or "")[:128],
                "parent_index": int(child.parent_index),
                "child_start": int(child.start),
                "heading_path": (child.heading_path or "")[:500],
                "document_name": (document_name or "")[:255],
                "knowledge_scope_code": (knowledge_scope_code or "default")[:64],
                "business_category": (business_category or "general")[:128],
                "content": child.content[:2000],
                "parent_content": (child.parent_content or "")[:8000],
                "embedding": embedding,
            }
            for child, embedding in zip(children, embeddings, strict=True)
        ]
        if rows:
            client.insert(collection_name=name, data=rows)
            client.flush(collection_name=name)
        return len(children)

    def delete_document(self, document_id: int) -> None:
        client, name = self._ensure_collection()
        client.delete(collection_name=name, filter=f"document_id == {int(document_id)}")
        client.flush(collection_name=name)

    # 目录从 Milvus 构建
    def catalog(self) -> list[DocumentCatalogItem]:
        client, name = self._ensure_collection()
        rows = client.query(
            collection_name=name,
            filter="document_id >= 0",
            output_fields=[
                "document_id",
                "document_name",
                "knowledge_scope_code",
                "business_category",
            ],
            limit=16384,
        )
        seen: dict[int, DocumentCatalogItem] = {}
        for row in rows:
            document_id = int(row["document_id"])
            if document_id in seen:
                continue
            name = str(row.get("document_name") or "")
            scope = str(row.get("knowledge_scope_code") or "default")
            category = str(row.get("business_category") or "general")
            seen[document_id] = DocumentCatalogItem(
                document_id=document_id,
                document_name=name,
                knowledge_scope_code=scope,
                business_category=category,
                keywords=[name, scope, category],
            )
        return list(seen.values())

    def search(
        self,
        query_embedding: list[float],
        document_ids: list[int],
        top_k: int = 8,
    ) -> list[StoredChunk]:
        client, name = self._ensure_collection()
        condition = ""
        if document_ids:
            ids = ",".join(str(int(item)) for item in document_ids)
            condition = f"document_id in [{ids}]"
        hits = client.search(
            collection_name=name,
            data=[query_embedding],
            anns_field="embedding",
            search_params={"metric_type": "COSINE", "params": {"ef": 64}},
            limit=max(1, top_k),
            filter=condition,
            output_fields=_OUTPUT_FIELDS,
        )
        result: list[StoredChunk] = []
        for hit in hits[0] if hits else []:
            entity = hit["entity"]
            result.append(
                StoredChunk(
                    document_id=int(entity.get("document_id")),
                    document_name=str(entity.get("document_name") or ""),
                    knowledge_scope_code=str(entity.get("knowledge_scope_code") or "default"),
                    business_category=str(entity.get("business_category") or "general"),
                    chunk_index=int(entity.get("chunk_index") or 0),
                    content=str(entity.get("content") or ""),
                    score=float(hit["distance"] or 0),
                    parent_id=str(entity.get("parent_id") or ""),
                    parent_index=int(entity.get("parent_index") or 0),
                    child_start=int(entity.get("child_start") or 0),
                    heading_path=str(entity.get("heading_path") or ""),
                    parent_content=str(entity.get("parent_content") or ""),
                )
            )
        return result

    def chunk_count(self) -> int:
        client, name = self._ensure_collection()
        rows = client.query(
            collection_name=name,
            filter="document_id >= 0",
            output_fields=["count(*)"],
        )
        return int(rows[0]["count(*)"] if rows else 0)

    def _ensure_collection(self) -> tuple[MilvusClient, str]:
        with _ENSURE_LOCK:
            if self._client is None:
                self._client = MilvusClient(
                    uri=f"http://{settings.milvus_host}:{settings.milvus_port}"
                )
            client = self._client
            name = settings.milvus_collection
            if not client.has_collection(collection_name=name):
                schema = client.create_schema(auto_id=True, enable_dynamic_field=False)
                schema.add_field(
                    field_name="id",
                    datatype=DataType.INT64,
                    is_primary=True,
                    auto_id=True,
                )
                schema.add_field(field_name="document_id", datatype=DataType.INT64)
                schema.add_field(field_name="chunk_index", datatype=DataType.INT64)
                schema.add_field(field_name="parent_index", datatype=DataType.INT64)
                schema.add_field(field_name="child_start", datatype=DataType.INT64)
                schema.add_field(
                    field_name="parent_id",
                    datatype=DataType.VARCHAR,
                    max_length=128,
                )
                schema.add_field(
                    field_name="heading_path",
                    datatype=DataType.VARCHAR,
                    max_length=2048,
                )
                schema.add_field(
                    field_name="document_name",
                    datatype=DataType.VARCHAR,
                    max_length=1024,
                )
                schema.add_field(
                    field_name="knowledge_scope_code",
                    datatype=DataType.VARCHAR,
                    max_length=64,
                )
                schema.add_field(
                    field_name="business_category",
                    datatype=DataType.VARCHAR,
                    max_length=128,
                )
                schema.add_field(
                    field_name="content",
                    datatype=DataType.VARCHAR,
                    max_length=8192,
                )
                schema.add_field(
                    field_name="parent_content",
                    datatype=DataType.VARCHAR,
                    max_length=32768,
                )
                schema.add_field(
                    field_name="embedding",
                    datatype=DataType.FLOAT_VECTOR,
                    dim=settings.embedding_dim,
                )

                index_params = client.prepare_index_params()
                index_params.add_index(
                    field_name="embedding",
                    index_type="HNSW",
                    metric_type="COSINE",
                    params={"M": 8, "efConstruction": 64},
                )
                client.create_collection(
                    collection_name=name,
                    schema=schema,
                    index_params=index_params,
                )
            client.load_collection(collection_name=name)
            return client, name
