"""Tool 路由：物资查询 vs 申领导购（优先于默认 RAG）。"""

from __future__ import annotations

import re
from dataclasses import dataclass
from enum import StrEnum


class ToolType(StrEnum):
    GOODS_SEARCH = "goods_search"
    SHOPPING_GUIDE = "shopping_guide"


@dataclass
class ToolDecision:
    tool: ToolType | None
    confidence: float
    keyword: str = ""
    reason: str = ""


_GOODS_MARKERS = (
    "物资",
    "商品",
    "查询",
    "搜索",
    "找",
    "有没有",
    "有哪些",
    "列出",
    "推荐",
    "价格",
    "库存",
    "上架",
    "卖",
)

_GUIDE_MARKERS = (
    "导购",
    "怎么申领",
    "如何申领",
    "限量",
    "秒杀",
    "抢",
    "申领流程",
    "流程",
    "步骤",
    "在哪申领",
    "哪里抢",
    "怎么抢",
    "活动",
    "场次",
)

_GUIDE_STRONG = ("导购", "怎么申领", "如何申领", "申领流程", "在哪申领", "怎么抢")


def _extract_keyword(question: str) -> str:
    text = (question or "").strip()
    for prefix in (
        "帮我查",
        "帮我搜索",
        "搜索一下",
        "查询",
        "查找",
        "找一下",
        "有没有",
        "有哪些",
        "列出",
    ):
        if text.startswith(prefix):
            text = text[len(prefix) :].strip()
    text = re.sub(r"[？?。！!，,；;]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text[:64]


class ToolRouter:
    @staticmethod
    def route(question: str) -> ToolDecision:
        normalized = (question or "").strip()
        if len(normalized) < 2:
            return ToolDecision(tool=None, confidence=0.0)

        has_goods = any(m in normalized for m in _GOODS_MARKERS)
        has_guide = any(m in normalized for m in _GUIDE_MARKERS)
        strong_guide = any(m in normalized for m in _GUIDE_STRONG)

        if strong_guide or (has_guide and not has_goods):
            return ToolDecision(
                tool=ToolType.SHOPPING_GUIDE,
                confidence=0.88 if strong_guide else 0.78,
                reason="命中申领导购/流程类意图",
            )

        if has_goods:
            keyword = _extract_keyword(normalized) or normalized
            return ToolDecision(
                tool=ToolType.GOODS_SEARCH,
                confidence=0.85,
                keyword=keyword,
                reason="命中物资查询类意图",
            )

        return ToolDecision(tool=None, confidence=0.0)
