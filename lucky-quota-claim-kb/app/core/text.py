"""文本分词工具：整词 + 汉字二元组，避免中文无空格时整句变成一个 token。"""

from __future__ import annotations

import re


def tokenize(text: str) -> set[str]:
    parts = re.findall(r"[\w一-鿿]+", text.lower())
    tokens: set[str] = set()
    for part in parts:
        if len(part) >= 2:
            tokens.add(part)
        for index in range(len(part) - 1):
            a, b = part[index], part[index + 1]
            if "一" <= a <= "鿿" and "一" <= b <= "鿿":
                tokens.add(part[index : index + 2])
    return tokens
