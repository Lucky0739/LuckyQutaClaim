from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class DocumentCatalogItem(BaseModel):
    document_id: int
    document_name: str
    knowledge_scope_code: str = "default"
    business_category: str = "general"
    keywords: list[str] = Field(default_factory=list)


class IndexRequest(CamelModel):
    document_id: int
    document_name: str
    knowledge_scope_code: str = "default"
    business_category: str = "general"
    bucket_name: str
    object_name: str
    file_type: int


class IndexResponse(CamelModel):
    document_id: int
    chunk_count: int


class DeleteRequest(CamelModel):
    document_id: int


class AskRequest(CamelModel):
    question: str
    chat_id: str | None = None


class KnowledgeReference(CamelModel):
    document_id: int
    title: str
    domain: str
    topic: str
    snippet: str
    score: int


class AskResponse(CamelModel):
    executor: str
    confidence: float
    reason: str
    missing_info: list[str] = Field(default_factory=list)
    answer: str
    clarification_questions: list[str] = Field(default_factory=list)
    funnel: dict = Field(default_factory=dict)
    shadow_funnel: dict = Field(default_factory=dict)
    references: list[KnowledgeReference] = Field(default_factory=list)
    tool_name: str = ""
    tool_payload: dict = Field(default_factory=dict)
