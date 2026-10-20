"""父子块切块引擎：结构化父块 + 递归子块。

- 父块：优先跟随文档标题层级（一个 section 一个父块），section 超长时递归切分
- 子块：在父块内部递归切分，用于向量检索
- 命中子块后可凭 parent_id / parent_content 还原完整上下文

语义切块与 LLM 切块属于后续增强策略，默认不参与流水线。
"""

from __future__ import annotations

from dataclasses import dataclass, field

from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.core.config import settings
from app.ingestion.structure import DocumentSection

# 中文优先的递归分隔符：段落 → 换行 → 句末 → 分句 → 空格 → 字符
_SEPARATORS = ["\n\n", "\n", "。", "！", "？", "；", "，", " ", ""]


@dataclass
class ChildChunk:
    document_id: int
    parent_id: str
    parent_index: int
    chunk_index: int
    heading_path: str
    content: str
    parent_content: str
    start: int

    @property
    def embedding_text(self) -> str:
        """向量化文本带上标题路径，提升“章节主题 + 正文”的召回。"""
        if self.heading_path:
            return f"{self.heading_path}\n{self.content}"
        return self.content


@dataclass
class ParentChunk:
    document_id: int
    parent_id: str
    parent_index: int
    heading_path: str
    content: str
    children: list[ChildChunk] = field(default_factory=list)


@dataclass
class ChunkBundle:
    parents: list[ParentChunk]
    children: list[ChildChunk]


def chunk_sections(document_id: int, sections: list[DocumentSection]) -> ChunkBundle:
    parent_splitter = _splitter(settings.parent_chunk_size, settings.parent_chunk_overlap)
    child_splitter = _splitter(settings.child_chunk_size, settings.child_chunk_overlap)

    parents: list[ParentChunk] = []
    children: list[ChildChunk] = []
    parent_index = 0
    chunk_index = 0

    for section in sections:
        text = section.text.strip()
        if not text:
            continue
        heading_path = " > ".join(section.heading_path)
        parent_texts = _split(parent_splitter, text, settings.parent_chunk_size)

        for parent_text in parent_texts:
            parent_text = parent_text.strip()
            if not parent_text:
                continue

            parent_id = f"{document_id}-{parent_index}"
            parent = ParentChunk(
                document_id=document_id,
                parent_id=parent_id,
                parent_index=parent_index,
                heading_path=heading_path,
                content=parent_text,
            )

            cursor = 0
            for child_text in _split(child_splitter, parent_text, settings.child_chunk_size):
                child_text = child_text.strip()
                if not child_text:
                    continue
                start = _locate(parent_text, child_text, cursor)
                cursor = start + 1
                child = ChildChunk(
                    document_id=document_id,
                    parent_id=parent_id,
                    parent_index=parent_index,
                    chunk_index=chunk_index,
                    heading_path=heading_path,
                    content=child_text,
                    parent_content=parent_text,
                    start=start,
                )
                parent.children.append(child)
                children.append(child)
                chunk_index += 1

            if parent.children:
                parents.append(parent)
                parent_index += 1

    return ChunkBundle(parents=parents, children=children)


def _splitter(size: int, overlap: int) -> RecursiveCharacterTextSplitter:
    size = max(1, size)
    return RecursiveCharacterTextSplitter(
        separators=_SEPARATORS,
        chunk_size=size,
        chunk_overlap=max(0, min(overlap, size - 1)),
        keep_separator=True,
        length_function=len,
        strip_whitespace=True,
    )


def _split(splitter: RecursiveCharacterTextSplitter, text: str, size: int) -> list[str]:
    """文本不超过阈值时保持原样，避免 splitter 额外改写内容。"""
    if len(text) <= size:
        return [text]
    return splitter.split_text(text)


def _locate(parent_text: str, child_text: str, cursor: int) -> int:
    """定位子块在父块中的起始偏移，用于命中后按位置截取上下文。"""
    start = parent_text.find(child_text, cursor)
    if start >= 0:
        return start
    start = parent_text.find(child_text)
    return max(start, 0)
