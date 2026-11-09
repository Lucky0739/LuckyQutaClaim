# lucky-quta-claim

#### 介绍

限量高并发物资申领平台。用户可以浏览物资、参与限时限量申领、支付订单，并通过知识库客服查询规则和实时场次。发放单位负责物资上架和场次投放，管理员负责审核与知识库文档。

核心链路是 Redis Lua 预扣名额、RocketMQ 异步落库、延迟消息取消未支付订单，以及知识库问答（父子块切块、三级漏斗、Milvus 检索、qwen3-rerank 重排序）。

#### 软件架构

```
前端 (Vue 3)
    │
    ▼
Spring Cloud Gateway :8888
    │
    ├── 用户服务 :8081     登录、消息、知识库入口
    ├── 活动服务 :8095     物资、发放单位、限量申领
    └── 订单服务 :8084     申领单、余额支付、支付宝沙箱
    │
    ▼
Nacos · Seata · RocketMQ · Redis · MySQL
MinIO · Milvus · 知识库 Python 服务 (FastAPI)
```

| 启动类 | 端口 | 说明 |
|--------|------|------|
| `UserApplication` | 8081 | 用户、消息、知识库 |
| `ActivityApplication` | 8095 | 物资、发放单位、限量申领 |
| `OrderApplication` | 8084 | 申领单与支付 |
| `GatewayApplication` | 8888 | 网关 |
| 知识库 `app.main` | 8000 | 切块、检索、重排序 |

Maven 模块：`lucky-quota-claim-common`、`lucky-quota-claim-user`、`lucky-quota-claim-activity`、`lucky-quota-claim-order`、`lucky-quota-claim-gateway`。前端在 `lucky-quota-claim-front`，知识库在 `lucky-quota-claim-kb`。

技术栈：Java 21、Spring Boot 3.3、Spring Cloud Alibaba、Nacos、Gateway、OpenFeign、Sa-Token、Seata、RocketMQ、Redis、MySQL、MyBatis-Plus、MinIO、Milvus、FastAPI、Vue 3、Docker Compose。

#### 安装教程

1. 安装 JDK 21、Maven 3.9+、Node.js 18+、Python 3.11+、Docker。
2. 启动中间件：

```bash
bash docker/start-middleware.sh
```

默认拉起 MySQL（`127.0.0.1:3306`，库 `lucky-quta-claim`）、Redis（`6380`）、Nacos（`8848`）、RocketMQ、Seata（`8091`）、MinIO（`9000`）、Milvus（`19530`）。本地开发账号见 `docker/README.md`。空库再导入 `sql/schema.sql`。需要 Elasticsearch 时加 `--with-es`。

3. 配置密钥。复制 `lucky-quota-claim-kb/.env.example` 为 `lucky-quota-claim-kb/.env`，填写向量模型与 `RERANK_API_KEY`。短信和对象存储密钥放在根目录 `application-secret.yml`（不要提交）。未配短信时，验证码打印在用户服务日志里。
4. 编译并启动：

```bash
mvn clean install -DskipTests
bash scripts/start-backend.sh
bash scripts/start-kb.sh
cd lucky-quota-claim-front && npm install && npm run dev
```

也可以执行 `bash scripts/dev-up.sh --with-apps`，同时拉起中间件和 Java 服务。

#### 使用说明

1. 前端开发地址以 Vite 输出为准，接口统一走网关 `http://localhost:8888`。Nacos 控制台为 `http://127.0.0.1:8848/nacos`，各服务接口文档为 `http://localhost:{port}/doc.html`。
2. 登录后，请求头携带 `Authorization: Bearer {token}`。用户、商家、管理员由 Sa-Token 按角色拦截。
3. 限量申领：校验场次 → Redis Lua 预扣名额 → MQ 异步落库 → 乐观锁扣库存 → 镜像到订单表。发送延迟消息取消未支付订单并释放名额。失败归还名额，可重试消息进入死信，终态失败可人工重投。
4. 支付：余额支付和支付宝沙箱。Seata 将余额、订单状态和物资库存放在同一全局事务中。过期或已取消的订单不会被支付回调改成已支付。
5. 知识库：管理员上传文档到 MinIO。Python 服务做结构化切块、递归切块、语义切块，缺少结构的长文档才走 LLM 切块。问答先做意图分流，文档问题经三级漏斗和 Milvus 检索后，用 qwen3-rerank 重排序。查物资和申领流程走实时接口。

#### 参与贡献

1. Fork 本仓库
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request

#### 特技

1. 秒杀全链路：Redis Lua 预扣名额，MQ 异步落库；乐观锁扣库存，唯一索引防重复申领。失败归还名额，重试进死信，终态入库可人工重投。
2. 延迟消息关单：下单后按单发送 RocketMQ 延迟消息，到期取消未支付订单并释放名额，不扫表。
3. 跨服务支付：Seata AT 同时维护余额、订单状态和物资库存，任一步失败一起回滚。
4. 知识库：父子块切块、三级漏斗、影子漏斗观测、qwen3-rerank 重排序。无关问题直接拒答，实时问题走业务接口，避免脱离依据编造答案。
