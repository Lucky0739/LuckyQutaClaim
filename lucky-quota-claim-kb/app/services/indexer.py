from __future__ import annotations

import logging

from minio import Minio

from app.core.config import settings
from app.ingestion.chunker import chunk_sections
from app.ingestion.cleaner import clean_text
from app.ingestion.structure import DocumentSection, parse_sections
from app.rag.embedder import embed_texts
from app.rag.vector_store import VectorStore
from app.schemas import IndexRequest, IndexResponse

logger = logging.getLogger(__name__)


class DocumentIndexer:
    def __init__(self) -> None:
        self.minio = Minio(
            settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure,
        )
        self.store = VectorStore()

    def index(self, request: IndexRequest) -> IndexResponse:
        raw = self._download(request.bucket_name, request.object_name)
        sections = parse_sections(raw, request.file_type)
        cleaned = [
            DocumentSection(section.heading_path, clean_text(section.text))
            for section in sections
        ]
        cleaned = [section for section in cleaned if section.text]
        bundle = chunk_sections(request.document_id, cleaned)
        if not bundle.children:
            raise ValueError("清洗后文本为空，无法切块")

        embeddings = embed_texts([child.embedding_text for child in bundle.children])
        count = self.store.replace_document(
            request.document_id,
            request.document_name,
            request.knowledge_scope_code,
            request.business_category,
            bundle.children,
            embeddings,
        )
        logger.info(
            "indexed document_id=%s parents=%s children=%s",
            request.document_id,
            len(bundle.parents),
            count,
        )
        return IndexResponse(document_id=request.document_id, chunk_count=count)

    def delete(self, document_id: int) -> None:
        self.store.delete_document(document_id)

    def _download(self, bucket: str, object_name: str) -> bytes:
        response = self.minio.get_object(bucket, object_name)
        try:
            return response.read()
        finally:
            response.close()
            response.release_conn()
