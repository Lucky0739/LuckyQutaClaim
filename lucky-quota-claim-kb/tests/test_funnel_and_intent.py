from app.rag.funnel import KnowledgeFunnel, ambiguous_domains
from app.rag.intent_router import ExecutorType, IntentRouter
from app.rag.vector_store import StoredChunk
from app.schemas import DocumentCatalogItem
from app.services.ask_service import ANSWER_LEAD, _build_answer


def _catalog() -> list[DocumentCatalogItem]:
    return [
        DocumentCatalogItem(
            document_id=2,
            document_name="开店申请",
            knowledge_scope_code="shop",
            business_category="开店",
            keywords=["开店", "商家", "审核"],
        ),
        DocumentCatalogItem(
            document_id=3,
            document_name="发布与上架商品",
            knowledge_scope_code="goods",
            business_category="上架",
            keywords=["发布商品", "上架", "审核"],
        ),
        DocumentCatalogItem(
            document_id=4,
            document_name="秒杀活动规则",
            knowledge_scope_code="seckill",
            business_category="规则",
            keywords=["秒杀", "抢购"],
        ),
    ]


def test_short_question_is_clarification() -> None:
    assert IntentRouter.route("它").executor == ExecutorType.CLARIFICATION


def test_weather_question_is_out_of_scope() -> None:
    decision = IntentRouter.route("今天天气怎么样")
    assert decision.executor == ExecutorType.OUT_OF_SCOPE


def test_funnel_narrows_to_shop() -> None:
    outcome = KnowledgeFunnel(domain_top_k=1, topic_top_k=1).narrow(
        "如何申请开店",
        _catalog(),
        [],
    )
    assert outcome.document_ids == [2]
    assert outcome.domains == ["shop"]


def test_shadow_funnel_is_wider() -> None:
    comparison = KnowledgeFunnel(domain_top_k=1, topic_top_k=1).compare_shadow(
        "如何申请开店",
        _catalog(),
        [],
    )
    assert len(comparison.shadow.document_ids) >= len(comparison.primary.document_ids)


def test_related_question_is_not_unrelated() -> None:
    outcome = KnowledgeFunnel(domain_top_k=1, topic_top_k=1).narrow(
        "如何申请开店",
        _catalog(),
        [],
    )
    assert outcome.unrelated is False


def test_offtopic_question_is_unrelated() -> None:
    outcome = KnowledgeFunnel(domain_top_k=2, topic_top_k=3).narrow(
        "今天上海天气怎么样",
        _catalog(),
        [],
    )
    assert outcome.unrelated is True
    assert ambiguous_domains(outcome.domain_scores) == []


def test_answer_uses_numbered_excerpts() -> None:
    hits = [
        StoredChunk(1, "申领规则", "seckill", "规则", 0, "  名额确认后进入待发放。  \n\n超时释放。", 0.8),
        StoredChunk(1, "申领规则", "seckill", "规则", 1, "名额确认后进入待发放。\n超时释放。", 0.7),
        StoredChunk(2, "发放单位", "shop", "申请", 0, "提交申请后等待审核。" + "说明" * 400, 0.6),
    ]
    answer = _build_answer(hits)
    assert answer.startswith(ANSWER_LEAD)
    assert "1. 申领规则\n名额确认后进入待发放。" in answer
    assert answer.count("1. 申领规则") == 1
    assert "2. 发放单位" in answer
    assert answer.endswith("。") or answer.endswith("…")


def test_tied_domains_need_clarification() -> None:
    tied = ambiguous_domains({"shop": 2, "goods": 2, "seckill": 1})
    assert tied == ["goods", "shop"]
