import io

from docx import Document

from app.core.config import settings
from app.ingestion.chunker import chunk_sections
from app.ingestion.structure import DocumentSection, parse_sections
from app.rag.vector_store import StoredChunk
from app.services.ask_service import _build_answer, _dedupe_by_parent, _hit_context


def test_parse_markdown_keeps_heading_path() -> None:
    raw = "# 开店申请\n提交资料后等待审核。\n\n## 申请材料\n需要营业执照。\n"
    sections = parse_sections(raw.encode("utf-8"), file_type=4)

    assert [section.heading_path for section in sections] == [
        ["开店申请"],
        ["开店申请", "申请材料"],
    ]
    assert sections[1].text == "需要营业执照。"


def test_parse_plain_text_detects_numbered_headings() -> None:
    raw = "第一章 总则\n本规则适用于平台。\n1.1 申请条件\n需要有营业执照。\n"
    sections = parse_sections(raw.encode("utf-8"), file_type=5)

    assert sections[0].heading_path == ["第一章 总则"]
    assert sections[1].heading_path == ["第一章 总则", "1.1 申请条件"]


def test_parse_docx_heading_styles() -> None:
    document = Document()
    document.add_heading("秒杀活动", level=1)
    document.add_paragraph("每人限购一件。")
    document.add_heading("退款规则", level=2)
    document.add_paragraph("未支付订单自动释放名额。")
    buffer = io.BytesIO()
    document.save(buffer)

    sections = parse_sections(buffer.getvalue(), file_type=3)

    assert sections[0].heading_path == ["秒杀活动"]
    assert sections[1].heading_path == ["秒杀活动", "退款规则"]
    assert sections[1].text == "未支付订单自动释放名额。"


def test_parse_html_headings_and_paragraphs() -> None:
    raw = "<h1>支付规则</h1><p>余额不足时下单失败。</p><h2>退款</h2><p>原路退回。</p>"
    sections = parse_sections(raw.encode("utf-8"), file_type=6)

    assert [section.heading_path for section in sections] == [
        ["支付规则"],
        ["支付规则", "退款"],
    ]


def test_parent_child_sizes_and_offsets() -> None:
    body = "。".join(f"第{i}条规则说明" for i in range(400)) + "。"
    sections = [
        DocumentSection(["申领规则"], body),
        DocumentSection(["申领规则", "补充说明"], "短内容。"),
    ]

    bundle = chunk_sections(7, sections)

    assert bundle.parents
    assert bundle.children
    for parent in bundle.parents:
        assert len(parent.content) <= settings.parent_chunk_size
        assert parent.children
    for child in bundle.children:
        assert len(child.content) <= settings.child_chunk_size
        assert child.content in child.parent_content
        assert child.parent_content[child.start : child.start + len(child.content)] == child.content
        assert child.parent_id == f"{child.document_id}-{child.parent_index}"


def test_small_section_becomes_single_child() -> None:
    bundle = chunk_sections(1, [DocumentSection(["说明"], "一句话。")])

    assert len(bundle.parents) == 1
    assert len(bundle.children) == 1
    assert bundle.children[0].embedding_text == "说明\n一句话。"


def _hit() -> StoredChunk:
    parent = "前文背景说明。" * 40 + "命中子块内容。" + "后文补充说明。" * 20
    return StoredChunk(
        1,
        "申领规则",
        "seckill",
        "规则",
        3,
        "命中子块内容。",
        0.9,
        parent_id="1-0",
        child_start=parent.index("命中子块内容。"),
        parent_content=parent,
    )


def test_hit_context_returns_parent_window_around_child() -> None:
    hit = _hit()

    context = _hit_context(hit)

    assert "命中子块内容。" in context
    assert "前文背景说明。" in context
    assert len(context) <= 650
    assert context.startswith("…")


def test_dedupe_by_parent_keeps_best_hit() -> None:
    best = _hit()
    duplicate = _hit()
    duplicate.score = 0.5
    duplicate.chunk_index = 4

    hits = _dedupe_by_parent([best, duplicate])

    assert hits == [best]


def test_answer_uses_parent_context() -> None:
    answer = _build_answer([_hit()])

    assert "前文背景说明。" in answer
    assert "命中子块内容。" in answer
