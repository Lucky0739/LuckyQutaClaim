"""三级知识漏斗：领域 → 主题 → 文档；影子路由更宽阈值，只观测不改主检索。"""

from __future__ import annotations

from dataclasses import dataclass, field

from app.core.text import tokenize
from app.schemas import DocumentCatalogItem


@dataclass
class FunnelLayerResult:
    layer: str
    candidates: list[str]
    selected: list[str]


@dataclass
class FunnelOutcome:
    document_ids: list[int]
    domains: list[str]
    topics: list[str]
    layers: list[FunnelLayerResult] = field(default_factory=list)
    domain_scores: dict[str, int] = field(default_factory=dict)
    unrelated: bool = False

    def to_event(self) -> dict[str, object]:
        return {
            "documentIds": self.document_ids,
            "domains": self.domains,
            "topics": self.topics,
            "unrelated": self.unrelated,
            "layers": [
                {
                    "layer": layer.layer,
                    "candidates": layer.candidates,
                    "selected": layer.selected,
                }
                for layer in self.layers
            ],
        }


@dataclass
class ShadowFunnelComparison:
    primary: FunnelOutcome
    shadow: FunnelOutcome

    def to_event(self) -> dict[str, object]:
        primary_ids = set(self.primary.document_ids)
        shadow_ids = set(self.shadow.document_ids)
        overlap = len(primary_ids & shadow_ids)
        union = len(primary_ids | shadow_ids) or 1
        return {
            "primary": self.primary.to_event(),
            "shadow": self.shadow.to_event(),
            "overlapCount": overlap,
            "jaccard": round(overlap / union, 4),
            "wouldChangeRetrieval": primary_ids != shadow_ids,
        }


class KnowledgeFunnel:
    def __init__(self, domain_top_k: int = 2, topic_top_k: int = 3):
        self.domain_top_k = domain_top_k
        self.topic_top_k = topic_top_k

    def narrow(
        self,
        question: str,
        catalog: list[DocumentCatalogItem],
        requested_document_ids: list[int],
        *,
        shadow: bool = False,
    ) -> FunnelOutcome:
        pool = self._pool(catalog, requested_document_ids)
        if not pool:
            return FunnelOutcome(document_ids=[], domains=[], topics=[], layers=[])

        tokens = tokenize(question)
        domain_scores = _score_domains(pool, tokens)
        domain_top_k = max(self.domain_top_k, 3) if shadow else self.domain_top_k
        selected_domains = _top_keys(domain_scores, domain_top_k, min_score=0 if shadow else 1)
        unrelated = (not shadow) and max(domain_scores.values(), default=0) < 1
        if not selected_domains:
            selected_domains = sorted({item.knowledge_scope_code or "default" for item in pool})

        domain_pool = [
            item
            for item in pool
            if (item.knowledge_scope_code or "default") in selected_domains
        ]
        topic_scores = _score_topics(domain_pool, tokens)
        topic_top_k = max(self.topic_top_k, 5) if shadow else self.topic_top_k
        selected_topics = _top_keys(topic_scores, topic_top_k, min_score=0 if shadow else 1)
        if not selected_topics:
            selected_topics = sorted({item.business_category or "general" for item in domain_pool})

        document_ids = [
            item.document_id
            for item in domain_pool
            if (item.business_category or "general") in selected_topics
        ]
        if not document_ids:
            document_ids = [item.document_id for item in domain_pool]

        return FunnelOutcome(
            document_ids=document_ids,
            domains=selected_domains,
            topics=selected_topics,
            domain_scores=domain_scores,
            unrelated=unrelated,
            layers=[
                FunnelLayerResult("domain", sorted(domain_scores.keys()), selected_domains),
                FunnelLayerResult("topic", sorted(topic_scores.keys()), selected_topics),
                FunnelLayerResult(
                    "document",
                    [str(item.document_id) for item in domain_pool],
                    [str(value) for value in document_ids],
                ),
            ],
        )

    def compare_shadow(
        self,
        question: str,
        catalog: list[DocumentCatalogItem],
        requested_document_ids: list[int],
    ) -> ShadowFunnelComparison:
        primary = self.narrow(question, catalog, requested_document_ids, shadow=False)
        shadow = self.narrow(question, catalog, requested_document_ids, shadow=True)
        return ShadowFunnelComparison(primary=primary, shadow=shadow)

    @staticmethod
    def _pool(
        catalog: list[DocumentCatalogItem],
        requested_document_ids: list[int],
    ) -> list[DocumentCatalogItem]:
        if not catalog:
            return []
        if not requested_document_ids:
            return catalog
        allowed = set(requested_document_ids)
        return [item for item in catalog if item.document_id in allowed]


def ambiguous_domains(domain_scores: dict[str, int]) -> list[str]:
    """并列最高分且 ≥ 1 的领域视为歧义。"""
    if not domain_scores:
        return []
    top = max(domain_scores.values())
    if top < 1:
        return []
    tied = sorted(key for key, score in domain_scores.items() if score == top)
    return tied if len(tied) >= 2 else []


def _label(value: str | None, default: str) -> str:
    normalized = (value or "").strip()
    return normalized if normalized else default


def _score_domains(pool: list[DocumentCatalogItem], tokens: set[str]) -> dict[str, int]:
    scores: dict[str, int] = {}
    for item in pool:
        label = _label(item.knowledge_scope_code, "default")
        blob = " ".join([label, item.document_name, *item.keywords])
        score = len(tokens & tokenize(blob.replace("/", " ").replace("_", " ")))
        scores[label] = max(scores.get(label, 0), score)
    return scores


def _score_topics(pool: list[DocumentCatalogItem], tokens: set[str]) -> dict[str, int]:
    scores: dict[str, int] = {}
    for item in pool:
        label = _label(item.business_category, "general")
        blob = " ".join([label, item.document_name, *item.keywords])
        score = len(tokens & tokenize(blob.replace("/", " ").replace("_", " ")))
        scores[label] = max(scores.get(label, 0), score)
    return scores


def _top_keys(scores: dict[str, int], top_k: int, min_score: int) -> list[str]:
    ordered = sorted(scores.items(), key=lambda item: item[1], reverse=True)
    return [key for key, score in ordered if score >= min_score][:top_k]
