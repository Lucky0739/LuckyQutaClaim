# lucky-quota-claim-kb

商城知识库的 **Python 侧**。Java 负责上传（MinIO）和 SSE 对话入口，Python 负责清洗、结构感知的父子块切块、向量写入 **Milvus 3.0.x**（pymilvus 3.0 `MilvusClient`），以及检索。

链路：

1. 管理员 `POST /kb/document/upload` → Java 存 MinIO + MySQL（`index_status=待离线索引`），接口立即返回
2. **离线索引阶段**（后台线程 + 定时兜底）：Java 调 Python `POST /internal/kb/index`：下载对象 → 结构解析 → 清洗 → 父子块切块 → embedding → Milvus `kb_chunk_v2`
3. 用户 `POST /kb/stream` → Java 转发 Python SSE（Tool 路由 → 知识库 RAG / 物资查询 / 申领导购）

## 目录结构

```
app/
├── main.py              # FastAPI 入口，挂载路由
├── api/routes.py        # HTTP 接口：/health、/internal/kb/*
├── core/                # 跨模块基础能力
│   ├── config.py        # 环境变量与默认配置
│   ├── sse.py           # SSE 事件序列化
│   └── text.py          # 中文分词（整词 + 汉字二元组）
├── schemas/             # 请求/响应模型
├── ingestion/           # 入库流水线
│   ├── cleaner.py       # 文档清洗
│   ├── structure.py     # 结构感知解析（DOCX/HTML/Markdown/PDF 标题层级）
│   └── chunker.py       # 结构化父块 + 递归子块
├── rag/                 # 检索增强
│   ├── intent_router.py # 意图路由
│   ├── tool_router.py   # Agent Tool 路由（物资查询、申领导购）
│   ├── funnel.py        # 三级知识漏斗 + 影子路由
│   ├── embedder.py      # embedding（OpenAI / 本地哈希）
│   └── vector_store.py  # Milvus 读写
├── tools/               # Agent Tools（调 Java 内部接口）
│   ├── java_client.py
│   └── formatter.py
└── services/            # 业务编排
    ├── indexer.py       # 下载 → 清洗 → 切块 → 向量写入
    └── ask_service.py   # 问答 / SSE 流
```

## Agent Tools

| Tool | 触发示例 | Java 接口 |
|------|----------|-----------|
| `goods_search` | 搜索/查询物资、推荐商品 | `POST /internal/kb/tools/goods-search` |
| `shopping_guide` | 怎么申领、导购、限量活动 | `POST /internal/kb/tools/shopping-guide` |

Python 通过 `JAVA_KB_BASE_URL`（默认 `http://127.0.0.1:8081`）调用；物资/场次数据经用户服务转发网关 `kb.tool.gateway-base-url`。

## 父子块切块

- **解析**：DOCX 按 `Heading 1~6` 样式，HTML 按 `h1~h6`，Markdown 按 `#`，纯文本/PDF 按中文章节编号启发式识别标题，保留标题路径（如 `秒杀活动 > 退款规则`）。识别不到标题时整篇作为一个 section。
- **父块**：一个 section 对应一个父块；超过 `PARENT_CHUNK_SIZE` 时用递归切块二次拆分。父块提供完整上下文。
- **子块**：在父块内部递归切到 `CHILD_CHUNK_SIZE`，用于向量检索；embedding 文本会带上标题路径提升召回。
- **检索**：先召回 `RETRIEVAL_TOP_K` 个子块，按 `parent_id` 去重后，以命中子块的位置为中心取父块上下文生成答案和引用，避免同一段落重复出现。
- 语义切块、LLM 切块属于后续增强策略，当前默认不参与流水线。

默认参数（可通过环境变量覆盖）：父块 1600/重叠 100，子块 320/重叠 60，召回 8。

> **升级注意**：集合名已从 `kb_chunk` 变为 `kb_chunk_v2`。升级后需调用 `POST /kb/document/reindex` 重新索引全部文档；确认新集合数据正常后再删除旧集合。

## 启动

先起中间件（含 MinIO `:9000`、Milvus `:19530`），再：

```bash
bash scripts/start-kb.sh
```

升级 Milvus 后向量需要重建：清空 Milvus 数据卷并启动 3.0.x 后，由管理员调用 `POST /kb/document/reindex`，从 MinIO 原始文档重新索引（详见仓库根目录 README / `docker/README.md`）。

若 MySQL 数据卷是旧的、没有 `kb_document` 表：

```bash
docker exec -i lucky-quta-claim-mysql mysql -uroot -pyang2004 'lucky-quta-claim' \
  < sql/kb_document.sql
```

可选：把 `.env.example` 复制为 `.env` 并配置 `EMBEDDING_API_KEY`，默认使用 DashScope（阿里云百炼）的 `text-embedding-v4`，输出 1536 维向量。未配置密钥时使用 1536 维本地哈希向量，便于先跑通。

```dotenv
EMBEDDING_API_KEY=sk-xxxxxxxx
EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
EMBEDDING_MODEL=text-embedding-v4
EMBEDDING_DIM=1536
EMBEDDING_BATCH_SIZE=10
```

注意：`qwen3.8-flash` 是对话模型，不支持 embedding 接口，向量化请使用 `text-embedding-v4`。更换向量模型或维度后，需要调用 `POST /kb/document/reindex` 重新生成全部向量。
