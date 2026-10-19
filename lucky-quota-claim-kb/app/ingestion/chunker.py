"""滑动窗口切块：窗口 800 字符、步长 680（重叠 120），尽量在句末/换行处收尾。"""

from __future__ import annotations

import re

CHUNK_SIZE = 800
CHUNK_OVERLAP = 120
# 收尾边界：中文句末、换行，以及英文句末（句号后跟空白，避免切断 1.1 这类编号）
_BOUNDARY = re.compile(r"[。！？；!?;\n]|\.\s")
# 在窗口末尾往前最多回退多少字符寻找句子边界
_BOUNDARY_SEARCH = 120


def split_text(text: str) -> list[str]:
    if not text or not text.strip():
        return []

    text = text.strip()
    total = len(text)
    if total <= CHUNK_SIZE:
        return [text]

    chunks: list[str] = []
    start = 0
    while start < total:
        end = min(start + CHUNK_SIZE, total)

        # 尽量把切点对齐到句子/段落边界，避免把一句话从中间切断
        if end < total:
            floor = max(start + CHUNK_SIZE // 2, end - _BOUNDARY_SEARCH)
            last = None
            for match in _BOUNDARY.finditer(text, floor, end):
                last = match
            if last is not None:
                end = last.end()

        chunk = text[start:end].strip()
        if chunk:
            chunks.append(chunk)

        if end >= total:
            break
        # 下一窗口从当前切点回退 CHUNK_OVERLAP 个字符，形成滑动重叠
        start = max(end - CHUNK_OVERLAP, start + 1)

    return chunks
