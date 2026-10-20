"""启发式意图路由。摘自 lucky-knowledge-agent 的 IntentRouter._heuristic，去掉 LLM / ReAct。"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import StrEnum


class ExecutorType(StrEnum):
    CLARIFICATION = "clarification"
    KB = "kb"
    CHITCHAT = "chitchat"
    OUT_OF_SCOPE = "out_of_scope"


@dataclass
class IntentDecision:
    executor: ExecutorType
    confidence: float
    reason: str = ""
    missing_info: list[str] = field(default_factory=list)


class IntentRouter:
    @staticmethod
    def route(question: str) -> IntentDecision:
        normalized = (question or "").strip()
        if len(normalized) < 2:
            return IntentDecision(
                executor=ExecutorType.CLARIFICATION,
                confidence=0.95,
                reason="问题过短",
                missing_info=["请补充想了解的业务，例如发放单位申请、物资上架或限量申领"],
            )
        if normalized in {"它", "这个", "那个", "怎么办", "呢？", "呢"}:
            return IntentDecision(
                executor=ExecutorType.CLARIFICATION,
                confidence=0.9,
                reason="指代不明",
                missing_info=["请说明具体对象，例如哪个发放单位、哪件物资或哪场限量申领"],
            )
        chitchat_markers = ("你是谁", "你是什么", "你好", "介绍一下你自己", "你能做什么", "在吗")
        if any(marker in normalized for marker in chitchat_markers):
            return IntentDecision(
                executor=ExecutorType.CHITCHAT,
                confidence=0.92,
                reason="日常对话/身份介绍，无需检索知识库",
            )
        return IntentDecision(
            executor=ExecutorType.KB,
            confidence=0.7,
            reason="默认走知识库检索",
        )
