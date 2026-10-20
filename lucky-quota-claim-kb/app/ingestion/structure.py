"""结构感知解析：把文档拆成带标题路径的段落（DocumentSection）。

切块前必须先保留层级信息：DOCX 用标题样式，HTML 用 h1~h6，
Markdown 用 #，纯文本/PDF 用常见的中文章节编号启发式识别。
识别不到标题时整篇作为一个 section，由递归切块兜底。
"""

from __future__ import annotations

import io
import re
from dataclasses import dataclass

from bs4 import BeautifulSoup
from docx import Document as DocxDocument
from docx.table import Table
from docx.text.paragraph import Paragraph
from pypdf import PdfReader

_HEADING_STYLE = re.compile(r"^(?:Heading|标题)\s*([1-6])$", re.IGNORECASE)
_MARKDOWN_HEADING = re.compile(r"^(#{1,6})\s+(.+)$")
_CHAPTER_HEADING = re.compile(r"^第[一二三四五六七八九十百零〇\d]+[章节篇部分]\s*[、.．]?\s*(.*)$")
_CN_NUMBER_HEADING = re.compile(r"^([一二三四五六七八九十]+)、\s*(.+)$")
_ARABIC_NUMBER_HEADING = re.compile(r"^(\d+(?:\.\d+){0,3})[、.．]\s*(.+)$")
_SENTENCE_END = ("。", "！", "？", "；", "，", ".", ",", ";", "!", "?")
_MAX_PLAIN_HEADING_LEN = 30


@dataclass
class DocumentSection:
    """带标题路径的文档段落。heading_path 为空表示文档开头/无结构内容。"""

    heading_path: list[str]
    text: str


def parse_sections(content: bytes, file_type: int) -> list[DocumentSection]:
    """按 file_type 解析文档：1=PDF，2=旧版 DOC，3=DOCX，6=HTML，其余按文本。"""
    if file_type == 1:
        sections = _from_pdf(content)
    elif file_type == 2:
        raise ValueError("旧版 DOC 暂不支持，请转换为 DOCX 后上传")
    elif file_type == 3:
        sections = _from_docx(content)
    elif file_type == 6:
        sections = _from_html(content)
    else:
        sections = _from_text(content.decode("utf-8", errors="ignore"))
    return [section for section in sections if section.text.strip()]


# ==================== DOCX ====================


def _from_docx(content: bytes) -> list[DocumentSection]:
    document = DocxDocument(io.BytesIO(content))
    sections: list[DocumentSection] = []
    heading_path: list[str] = []
    body: list[str] = []

    def flush() -> None:
        text = "\n".join(body).strip()
        if text:
            sections.append(DocumentSection(list(heading_path), text))
        body.clear()

    for kind, block in _iter_docx_blocks(document):
        if kind == "table":
            body.extend(_table_lines(block))
            continue
        text = block.text.strip()
        if not text:
            continue
        level = _docx_heading_level(block)
        if level is None:
            body.append(text)
            continue
        flush()
        _push_heading(heading_path, level, text)

    flush()
    return sections


def _iter_docx_blocks(document):
    """按正文顺序遍历段落和表格，避免表格内容被整体挪到文末。"""
    for child in document.element.body.iterchildren():
        tag = child.tag.rsplit("}", 1)[-1]
        if tag == "p":
            yield "paragraph", Paragraph(child, document)
        elif tag == "tbl":
            yield "table", Table(child, document)


def _docx_heading_level(paragraph: Paragraph) -> int | None:
    style_name = getattr(getattr(paragraph, "style", None), "name", "") or ""
    match = _HEADING_STYLE.match(style_name.strip())
    return int(match.group(1)) if match else None


def _table_lines(table: Table) -> list[str]:
    lines: list[str] = []
    for row in table.rows:
        cells = [cell.text.strip() for cell in row.cells]
        line = " | ".join(cell for cell in cells if cell)
        if line:
            lines.append(line)
    return lines


# ==================== HTML ====================


def _from_html(content: bytes) -> list[DocumentSection]:
    soup = BeautifulSoup(content.decode("utf-8", errors="ignore"), "html.parser")
    sections: list[DocumentSection] = []
    heading_path: list[str] = []
    body: list[str] = []

    def flush() -> None:
        text = "\n".join(body).strip()
        if text:
            sections.append(DocumentSection(list(heading_path), text))
        body.clear()

    for element in soup.find_all(["h1", "h2", "h3", "h4", "h5", "h6", "p", "li", "tr"]):
        text = element.get_text(" ", strip=True)
        if not text:
            continue
        if element.name.startswith("h"):
            flush()
            _push_heading(heading_path, int(element.name[1]), text)
        else:
            body.append(text)

    flush()
    return sections


# ==================== 纯文本 / Markdown / PDF ====================


def _from_pdf(content: bytes) -> list[DocumentSection]:
    text = "\n".join(page.extract_text() or "" for page in PdfReader(io.BytesIO(content)).pages)
    return _from_text(text)


def _from_text(text: str) -> list[DocumentSection]:
    sections: list[DocumentSection] = []
    heading_path: list[str] = []
    body: list[str] = []

    def flush() -> None:
        joined = "\n".join(body).strip()
        if joined:
            sections.append(DocumentSection(list(heading_path), joined))
        body.clear()

    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    for line in normalized.split("\n"):
        stripped = line.strip()
        if not stripped:
            continue
        heading = _detect_text_heading(stripped)
        if heading is None:
            body.append(stripped)
            continue
        flush()
        _push_heading(heading_path, heading[0], heading[1])

    flush()
    return sections


def _detect_text_heading(line: str) -> tuple[int, str] | None:
    match = _MARKDOWN_HEADING.match(line)
    if match:
        return len(match.group(1)), match.group(2).strip()

    if len(line) > _MAX_PLAIN_HEADING_LEN or line.endswith(_SENTENCE_END):
        return None

    match = _CHAPTER_HEADING.match(line)
    if match:
        return 1, line

    match = _CN_NUMBER_HEADING.match(line)
    if match:
        return 2, line

    match = _ARABIC_NUMBER_HEADING.match(line)
    if match:
        # 1. → 二级标题，1.1 → 三级标题，以此类推
        level = min(6, 1 + len(match.group(1).split(".")))
        return level, line

    return None


def _push_heading(heading_path: list[str], level: int, text: str) -> None:
    del heading_path[max(0, level - 1) :]
    heading_path.append(text)
