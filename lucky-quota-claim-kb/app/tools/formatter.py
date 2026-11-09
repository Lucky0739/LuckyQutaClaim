from __future__ import annotations


def format_goods_search(data: dict) -> tuple[str, dict]:
    keyword = data.get("keyword") or ""
    total = int(data.get("total") or 0)
    items = list(data.get("items") or [])
    if not items:
        text = f"未找到与「{keyword}」相关的上架物资，可换个关键词或到「物资搜索」页浏览。"
        return text, {"name": "goods_search", "keyword": keyword, "total": total, "items": []}

    lines = [f"为您查到与「{keyword}」相关的物资（共 {total} 条，展示前 {len(items)} 条）：", ""]
    for index, item in enumerate(items, start=1):
        name = item.get("goodsName") or "未命名"
        price = item.get("goodsPrice")
        price_text = f"¥{price}" if price is not None else "价格见详情"
        category = item.get("categoryName") or ""
        cat_part = f" · {category}" if category else ""
        lines.append(f"{index}. {name}{cat_part} — {price_text}")
    lines.append("")
    lines.append("点击下方卡片可查看详情；如需政策说明可继续提问知识库。")
    payload = {
        "name": "goods_search",
        "keyword": keyword,
        "total": total,
        "items": items,
    }
    return "\n".join(lines), payload


def format_shopping_guide(data: dict) -> tuple[str, dict]:
    steps = list(data.get("steps") or [])
    ongoing = int(data.get("ongoingActivityCount") or 0)
    highlights = list(data.get("highlightGoods") or [])
    quota_path = data.get("quotaListPath") or "/seckill"

    lines = [
        "【智能申领导购】",
        "",
        f"当前进行中的限量场次：{ongoing} 个。",
        "",
        "建议申领步骤：",
    ]
    for index, step in enumerate(steps, start=1):
        lines.append(f"{index}. {step}")

    if highlights:
        lines.extend(["", "进行中场次中的部分物资："])
        for index, item in enumerate(highlights[:6], start=1):
            title = item.get("title") or "限量物资"
            activity = item.get("activityName") or ""
            stock = item.get("stock")
            stock_text = f"余量约 {stock}" if stock is not None else ""
            act_part = f"（{activity}）" if activity else ""
            lines.append(f"{index}. {title}{act_part} {stock_text}".strip())
    else:
        lines.extend(["", "暂无进行中的限量物资展示，可先浏览物资目录或稍后再试。"])

    lines.extend(["", f"进入限量申领专区：{quota_path}"])

    payload = {
        "name": "shopping_guide",
        "ongoingActivityCount": ongoing,
        "quotaListPath": quota_path,
        "steps": steps,
        "items": highlights,
    }
    return "\n".join(lines), payload
