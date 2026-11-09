from __future__ import annotations

import logging
import re
from collections.abc import Iterator

from app.core.config import settings
from app.core.sse import sse_event
from app.rag.embedder import embed_query
from app.rag.funnel import KnowledgeFunnel, ambiguous_domains
from app.rag.intent_router import ExecutorType, IntentRouter
from app.rag.tool_router import ToolDecision, ToolRouter, ToolType
from app.rag.vector_store import StoredChunk, VectorStore
from app.tools import formatter as tool_formatter
from app.tools import java_client as tool_java_client
from app.schemas import AskRequest, AskResponse, KnowledgeReference

logger = logging.getLogger(__name__)

NO_EVIDENCE = "未找到相关说明。请换个说法，或请管理员先上传相关知识文档。"
OUT_OF_SCOPE_ANSWER = (
    "这个问题与当前知识库（限量物资申领相关业务）无关。\n"
    "我不会用库内文档拼凑或自行编造答案。\n"
    "请换一个与平台业务相关的问题，例如发放单位申请、物资上架或限量申领规则。"
)
CHITCHAT_ANSWER = (
    "我是限量物资申领的智能客服。\n"
    "可查知识库规则，也可用工具：\n"
    "· 物资查询（如「搜索口罩」）\n"
    "· 申领导购（如「怎么限量申领」）\n"
    "具体政策以已上传文档为准。"
)
EMPTY_LIBRARY_ANSWER = "知识库还没有文档。请管理员在「知识库」中上传后再提问。"
ANSWER_LEAD = "根据知识库，相关说明如下："
DOMAIN_LABELS = {
    "user": "账号登录",
    "shop": "开店/商家",
    "goods": "商品上架",
    "seckill": "秒杀活动",
    "order": "订单物流",
    "pay": "支付余额",
    "notify": "消息通知",
}
# 低于此分数的命中不展示，避免弱相关片段被当成答案
MIN_VECTOR_SCORE = 0.28
TOTAL_MAX_CHARS = 1200
CHUNK_MAX_CHARS = 500
# 命中子块后，向前后取父块上下文窗口
CONTEXT_BEFORE = 150
RETRIEVAL_TOP_K = settings.retrieval_top_k


class KnowledgeAskService:
    def __init__(self) -> None:
        self.store = VectorStore()
        self.funnel = KnowledgeFunnel()

    def ask(self, request: AskRequest) -> AskResponse:
        events: dict[str, object] = {}
        answer_parts: list[str] = []
        for raw in self.iter_sse(request):
            event_name, payload = _parse_sse(raw)
            events[event_name] = payload
            if event_name == "text" and isinstance(payload, dict):
                answer_parts.append(str(payload.get("content") or ""))
        return AskResponse(
            executor=str((events.get("route") or {}).get("executor") or "kb")
            if isinstance(events.get("route"), dict)
            else "kb",
            confidence=float((events.get("route") or {}).get("confidence") or 0)
            if isinstance(events.get("route"), dict)
            else 0.0,
            reason=str((events.get("route") or {}).get("reason") or "")
            if isinstance(events.get("route"), dict)
            else "",
            missing_info=list((events.get("route") or {}).get("missingInfo") or [])
            if isinstance(events.get("route"), dict)
            else [],
            answer="".join(answer_parts) or NO_EVIDENCE,
            clarification_questions=list((events.get("route") or {}).get("clarificationQuestions") or [])
            if isinstance(events.get("route"), dict)
            else [],
            funnel=events.get("funnel") if isinstance(events.get("funnel"), dict) else {},
            shadow_funnel=events.get("shadow_funnel")
            if isinstance(events.get("shadow_funnel"), dict)
            else {},
            references=[
                KnowledgeReference.model_validate(item)
                for item in (events.get("references") or {}).get("items") or []
            ]
            if isinstance(events.get("references"), dict)
            else [],
            tool_name=str((events.get("tool") or {}).get("name") or "")
            if isinstance(events.get("tool"), dict)
            else "",
            tool_payload=events.get("tool") if isinstance(events.get("tool"), dict) else {},
        )

    def iter_sse(self, request: AskRequest) -> Iterator[str]:
        yield sse_event("status", {"stage": "routing"})
        decision = IntentRouter.route(request.question)
        yield sse_event(
            "route",
            {
                "executor": decision.executor.value,
                "confidence": decision.confidence,
                "reason": decision.reason,
                "missingInfo": decision.missing_info,
            },
        )
        if decision.executor == ExecutorType.CHITCHAT:
            yield from _stream_text(CHITCHAT_ANSWER)
            yield sse_event("done", {})
            return
        if decision.executor == ExecutorType.CLARIFICATION:
            questions = [item if item.endswith("？") else f"{item}？" for item in decision.missing_info]
            answer = "这个问题信息不够，请先补充：\n" + "\n".join(f"{index}. {q}" for index, q in enumerate(questions, start=1))
            yield from _stream_text(answer)
            yield sse_event("done", {})
            return
        if decision.executor == ExecutorType.OUT_OF_SCOPE:
            yield from _stream_text(OUT_OF_SCOPE_ANSWER)
            yield sse_event("done", {})
            return

        tool_decision = ToolRouter.route(request.question)
        if tool_decision.tool is not None:
            yield from self._run_tool(tool_decision)
            return

        # 和向量检索衔接
        catalog = self.store.catalog()
        if not catalog:
            yield from _stream_text(EMPTY_LIBRARY_ANSWER)
            yield sse_event("done", {})
            return

        yield sse_event("status", {"stage": "funnel_routing"})
        comparison = self.funnel.compare_shadow(request.question, catalog, [])
        primary = comparison.primary
        logger.info(
            "shadow funnel jaccard=%s wouldChange=%s",
            comparison.to_event()["jaccard"],
            comparison.to_event()["wouldChangeRetrieval"],
        )
        yield sse_event("funnel", primary.to_event())
        yield sse_event("shadow_funnel", comparison.to_event())

        #  # 领域歧义 → 反问
        tied = ambiguous_domains(primary.domain_scores)
        if tied:
            labels = [DOMAIN_LABELS.get(code, code) for code in tied]
            questions = [f"您想问的是「{label}」吗？" for label in labels]
            answer = "这个问题可能涉及多个业务，请先选一个方向：\n" + "\n".join(
                f"{index}. {item}" for index, item in enumerate(questions, start=1)
            )
            yield sse_event(
                "route",
                {
                    "executor": ExecutorType.CLARIFICATION.value,
                    "confidence": 0.86,
                    "reason": "多个业务领域得分并列，需要消歧",
                    "missingInfo": labels,
                    "clarificationQuestions": questions,
                },
            )
            yield from _stream_text(answer)
            yield sse_event("done", {})
            return

        if primary.unrelated:
            yield sse_event(
                "route",
                {
                    "executor": ExecutorType.OUT_OF_SCOPE.value,
                    "confidence": 0.9,
                    "reason": "漏斗判定与业务领域无关键词关联，跳过检索",
                    "missingInfo": ["请重新输入与知识库相关的问题"],
                },
            )
            yield from _stream_text(OUT_OF_SCOPE_ANSWER)
            yield sse_event("done", {})
            return

        # 用漏斗选出的 document_ids 过滤向量检索
        yield sse_event("status", {"stage": "retrieving"})
        hits = self.store.search(
            embed_query(request.question), primary.document_ids, top_k=RETRIEVAL_TOP_K
        )
        hits = [hit for hit in hits if hit.score >= MIN_VECTOR_SCORE]
        # 同一个父块可能命中多个子块，只保留得分最高的一个，避免答案重复
        hits = _dedupe_by_parent(hits)
        if not hits:
            yield from _stream_text(NO_EVIDENCE)
            yield sse_event("done", {})
            return

        answer = _build_answer(hits)
        yield sse_event("status", {"stage": "generating"})
        yield from _stream_text(answer)
        yield sse_event(
            "references",
            {
                "items": [
                    {
                        "documentId": hit.document_id,
                        "title": hit.document_name,
                        "domain": hit.knowledge_scope_code,
                        "topic": hit.business_category,
                        "snippet": _normalize_excerpt(_hit_context(hit))[:160],
                        "score": int(round(hit.score * 100)),
                    }
                    for hit in hits
                ]
            },
        )
        yield sse_event("done", {})

    def _run_tool(self, decision: ToolDecision) -> Iterator[str]:
        yield sse_event("status", {"stage": "tool"})
        tool_name = decision.tool.value if decision.tool else ""
        yield sse_event(
            "route",
            {
                "executor": "tool",
                "tool": tool_name,
                "confidence": decision.confidence,
                "reason": decision.reason,
                "missingInfo": [],
            },
        )
        if decision.tool == ToolType.GOODS_SEARCH:
            raw = tool_java_client.goods_search(decision.keyword) or {}
            answer, payload = tool_formatter.format_goods_search(raw)
        elif decision.tool == ToolType.SHOPPING_GUIDE:
            raw = tool_java_client.shopping_guide() or {}
            answer, payload = tool_formatter.format_shopping_guide(raw)
        else:
            answer, payload = "暂不支持该工具。", {"name": tool_name}
        yield sse_event("tool", payload)
        yield sse_event("status", {"stage": "generating"})
        yield from _stream_text(answer)
        yield sse_event("done", {})


def _dedupe_by_parent(hits: list[StoredChunk]) -> list[StoredChunk]:
    """按父块去重：同一父块只保留排名最高的子块命中。"""
    seen: set[str] = set()
    result: list[StoredChunk] = []
    for hit in hits:
        key = hit.parent_id or f"{hit.document_id}:{hit.chunk_index}"
        if key in seen:
            continue
        seen.add(key)
        result.append(hit)
    return result


def _hit_context(hit: StoredChunk) -> str:
    """命中子块后回取父块上下文：以子块位置为中心截取窗口。"""
    parent = (hit.parent_content or "").strip()
    if not parent:
        return hit.content

    start = max(0, hit.child_start - CONTEXT_BEFORE)
    end = min(len(parent), start + CONTEXT_BEFORE + CHUNK_MAX_CHARS)
    excerpt = parent[start:end]
    return f"…{excerpt}" if start > 0 else excerpt


def _build_answer(hits: list[StoredChunk]) -> str:
    used = 0
    parts: list[str] = []
    seen: set[str] = set()
    for hit in hits:
        if used >= TOTAL_MAX_CHARS:
            break
        body = _normalize_excerpt(_hit_context(hit))
        if not body or body in seen:
            continue
        room = min(CHUNK_MAX_CHARS, TOTAL_MAX_CHARS - used)
        body = _clip_excerpt(body, room)
        if not body:
            continue
        seen.add(body)
        title = (hit.document_name or "知识文档").strip() or "知识文档"
        block = f"{len(parts) + 1}. {title}\n{body}"
        parts.append(block)
        used += len(block)
    if not parts:
        return NO_EVIDENCE
    return f"{ANSWER_LEAD}\n\n" + "\n\n".join(parts)


def _normalize_excerpt(text: str) -> str:
    text = text.replace("\r\n", "\n").replace("\r", "\n").replace("\u3000", " ")
    lines = [re.sub(r"[ \t]+", " ", line).strip() for line in text.split("\n")]
    return "\n".join(line for line in lines if line)


def _clip_excerpt(text: str, limit: int) -> str:
    if len(text) <= limit:
        return text
    if limit < 2:
        return ""
    window = text[:limit]
    for separator in ("。", "！", "？", "\n", "；"):
        index = window.rfind(separator)
        if index >= limit // 2:
            return window[: index + 1].rstrip()
    return window.rstrip() + "…"


def _stream_text(answer: str) -> Iterator[str]:
    step = 24
    for index in range(0, len(answer), step):
        yield sse_event("text", {"content": answer[index : index + step]})


def _parse_sse(raw: str) -> tuple[str, object]:
    event_name = "message"
    data_line = ""
    for line in raw.strip().splitlines():
        if line.startswith("event:"):
            event_name = line[6:].strip()
        elif line.startswith("data:"):
            data_line = line[5:].strip()
    if not data_line:
        return event_name, {}
    try:
        import json

        return event_name, json.loads(data_line)
    except json.JSONDecodeError:
        return event_name, data_line
