"""知识库内部接口：索引、删除、问答与 SSE 流式问答。"""

from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse

from app.rag.vector_store import VectorStore
from app.schemas import AskRequest, AskResponse, DeleteRequest, IndexRequest, IndexResponse
from app.services.ask_service import KnowledgeAskService
from app.services.indexer import DocumentIndexer

router = APIRouter()

indexer = DocumentIndexer()
ask_service = KnowledgeAskService()
store = VectorStore()


@router.get("/health")
def health() -> dict[str, object]:
    try:
        chunks = store.chunk_count()
        milvus_ok = True
    except Exception as exc:  # noqa: BLE001
        chunks = 0
        milvus_ok = False
        return {"status": "degraded", "milvus": False, "error": str(exc), "chunks": chunks}
    return {"status": "ok", "milvus": milvus_ok, "chunks": chunks}


@router.post("/internal/kb/index", response_model=IndexResponse)
def index_document(request: IndexRequest) -> IndexResponse:
    try:
        return indexer.index(request)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/internal/kb/delete")
def delete_document(request: DeleteRequest) -> dict[str, bool]:
    indexer.delete(request.document_id)
    return {"ok": True}


@router.post("/internal/kb/ask", response_model=AskResponse)
def ask(request: AskRequest) -> AskResponse:
    return ask_service.ask(request)


@router.post("/internal/kb/stream")
def stream(request: AskRequest) -> StreamingResponse:
    return StreamingResponse(
        ask_service.iter_sse(request),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
