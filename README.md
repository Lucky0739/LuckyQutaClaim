# lucky_seckill_mall

#### 介绍

lucky_seckill_mall 是一个基于 Spring Cloud Alibaba 微服务架构的**秒杀商城**项目。项目采用前后端分离架构，实现了从用户认证、商品浏览、购物车、订单生成到秒杀抢购、支付回调、消息通知的完整电商业务流程，并集成 AI 智能客服功能。

#### 软件架构

本项目基于主流微服务技术栈构建，架构层次清晰：

```
┌─────────────────────────────────────────────────────────────┐
│                      Nginx / 前端客户端                        │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Cloud Gateway (8888)                     │
│           路由转发 · 限流熔断 (Sentinel) · 鉴权 (Sa-Token)      │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┬──────────────┐
        ▼             ▼             ▼              ▼
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│  user     │  │  goods    │  │  order    │  │  cart     │
│  用户服务  │  │  商品服务  │  │  订单服务  │  │ 购物车服务 │
└───────────┘  └───────────┘  └───────────┘  └───────────┘
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│  shop     │  │  pay      │  │  notify   │  │ activity  │
│  商店服务  │  │  支付服务  │  │  消息服务  │  │ 秒杀活动   │
└───────────┘  └───────────┘  └───────────┘  └───────────┘
┌───────────┐  ┌───────────┐
│   ai      │  │  common   │
│  AI客服   │  │  公共模块  │
└───────────┘  └───────────┘

中间件层：
┌─────────────────────────────────────────────────────────────┐
│  Nacos (注册中心/配置中心) · Sentinel (限流熔断) · Seata (分布式事务) │
│  RocketMQ (消息队列) · Redis (缓存) · MySQL (关系型数据库)         │
│  PostgreSQL + pgvector (向量存储，AI模块)                        │
└─────────────────────────────────────────────────────────────┘
```

**模块说明：**

| 模块 | 说明 |
|------|------|
| `lucky-seckill-common` | 公共模块：工具类、统一返回、异常处理、配置类、POJO、拦截器 |
| `lucky-seckill-gateway` | 网关模块：统一入口（端口 8888），路由转发，Sentinel 限流，Sa-Token 鉴权 |
| `lucky-seckill-user` | 用户模块：注册登录、短信验证码、用户信息管理 |
| `lucky-seckill-goods` | 商品模块：商品管理、分类管理、SKU 规格 |
| `lucky-seckill-order` | 订单模块：订单创建、状态流转 |
| `lucky-seckill-cart` | 购物车模块：购物车增删改查 |
| `lucky-seckill-shop` | 商店模块：店铺管理 |
| `lucky-seckill-pay` | 支付模块：支付下单、回调处理 |
| `lucky-seckill-notify` | 消息模块：消息推送与通知 |
| `lucky-seckill-activity` | 秒杀活动模块：秒杀活动管理、秒杀下单、库存预热、失败订单处理 |
| `lucky-seckill-ai` | AI 模块：基于 Spring AI + 阿里云 DashScope 大模型的智能客服，支持 RAG 向量检索 |

**核心依赖（技术选型）：**

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 3.3.5 | 基础框架 |
| Spring Cloud | 2023.0.3 | 微服务治理 |
| Spring Cloud Alibaba | 2023.0.3.2 | 阿里云微服务全家桶 |
| Nacos | - | 服务注册发现 & 配置中心 |
| Sentinel | - | 流量控制 & 熔断降级 |
| Gateway | - | API 网关 |
| OpenFeign | - | 服务间 RPC 调用 |
| Sa-Token | 1.45.0 | 权限认证框架（Token 鉴权） |
| Seata | - | 分布式事务 |
| RocketMQ | 2.3.5 | 消息队列（异步解耦） |
| MyBatis | 3.0.5 | ORM 持久层框架 |
| MySQL | - | 关系型数据库 |
| Redis | - | 缓存中间件（Lettuce 连接池） |
| Disruptor | 3.4.4 | 高性能内存队列 |
| Log4j2 | - | 日志框架 |
| Hutool | 5.8.31 | Java 工具类库 |
| PageHelper | 2.1.0 | MyBatis 分页插件 |
| Lombok | 1.18.42 | 简化代码注解 |
| Spring AI | 1.0.0 | AI 集成框架 |
| DashScope SDK | 2.19.1 | 阿里云灵积大模型服务 |
| pgvector | - | PostgreSQL 向量扩展（AI RAG） |
| Knife4j | 4.4.0 | API 接口文档 |
| 腾讯云 COS | 5.6.227 | 对象存储（图片/文件） |
| 阿里云 PNVS | - | 短信认证服务 |

#### 安装教程

##### 1. 环境准备

确保本地已安装以下软件：

| 软件 | 推荐版本 | 用途 |
|------|----------|------|
| JDK | 21+ | Java 运行环境 |
| Maven | 3.9+ | 项目构建管理 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 7.0+ | 缓存中间件 |
| Nacos | 2.3+ | 服务注册与配置中心 |
| RocketMQ | 5.0+ | 消息队列 |
| PostgreSQL | 15+（可选） | AI 模块向量存储数据库 |

##### 2. 启动中间件服务

依次启动以下中间件，确保均可正常访问：

```bash
# 启动 Nacos（默认地址 127.0.0.1:8848）
# 单机模式启动：sh startup.sh -m standalone

# 启动 Redis（默认端口 6379）

# 启动 RocketMQ NameServer 和 Broker

# （AI 模块需要）启动 PostgreSQL，并安装 pgvector 扩展
```

##### 3. 初始化数据库

执行以下 SQL 脚本创建数据库并初始化表结构：

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS seckill_mall DEFAULT CHARACTER SET utf8mb4;

USE seckill_mall;

-- 导入各模块 resources/db/ 目录下的 SQL 建表脚本
-- lucky-seckill-activity/src/main/resources/db/*.sql
```

##### 4. 修改配置文件

编辑各模块 `application-dev.yml` 中的连接信息：

- **MySQL**：修改 `spring.datasource.url`、`username`、`password`
- **Redis**：修改 `spring.data.redis.host`、`port`、`password`
- **Nacos**：确认 `spring.cloud.nacos.discovery.server-addr` 地址正确
- **Sentinel**：确认 `spring.cloud.sentinel.transport.dashboard` 地址正确
- **阿里云短信**：替换 `aliyun.pnvs` 下的 `access-key-id`、`access-key-secret`

##### 5. 编译与启动

```bash
# 克隆项目
git clone <your-repo-url>
cd lucky-seckill-mall

# 编译全部模块（跳过测试）
mvn clean install -DskipTests

# 按顺序启动各服务（或使用 IDE 运行各模块的 *Application.java）
# 1. 启动网关
cd lucky-seckill-gateway && mvn spring-boot:run

# 2. 启动业务模块（无严格顺序，建议先启动 common）
cd lucky-seckill-user && mvn spring-boot:run
cd lucky-seckill-goods && mvn spring-boot:run
# ... 依次启动其余模块
```

#### 使用说明

##### 1. 访问入口

- **网关统一入口**：`http://localhost:8888`
- **Sentinel 控制台**：`http://127.0.0.1:8858`
- **Nacos 控制台**：`http://127.0.0.1:8848/nacos`
- **Knife4j 接口文档**（各服务）：`http://localhost:{port}/doc.html`

##### 2. 接口前缀说明

| 路径前缀 | 目标服务 | 示例 |
|----------|----------|------|
| `/user/**` | 用户服务 | `/user/login`、`/user/code/send` |
| `/shop/**` | 商店服务 | `/shop/list` |
| `/goods/**` | 商品服务 | `/goods/detail` |
| `/category/**` | 商品分类 | `/category/list` |
| `/cart/**` | 购物车服务 | `/cart/add` |
| `/order/**` | 订单服务 | `/order/create` |
| `/seckill/**` | 秒杀服务 | `/seckill/execute` |
| `/activity/**` | 活动服务 | `/activity/list` |
| `/pay/**` | 支付服务 | `/pay/callback` |
| `/notify/**` | 消息服务 | `/notify/send` |
| `/ai/**` | AI 客服 | `/ai/chat` |
| `/banner/**` | 公共接口 | `/banner/list` |

##### 3. 用户认证

系统使用 Sa-Token 进行权限认证，Token 通过 `Authorization: Bearer {token}` 请求头传递。
登录接口获取 Token 后，后续请求携带 Token 即可完成身份认证。

##### 4. 秒杀流程

```
用户登录 → 查看秒杀活动 → 活动预热库存到 Redis
    → 秒杀开始 → 用户下单 → Disruptor 异步排队
    → Redis 预减库存 → RocketMQ 异步创建订单
    → 支付回调 → 订单状态更新 → 消息通知
```

#### 参与贡献

1. Fork 本仓库
2. 新建 `Feat_xxx` 分支
3. 提交代码
4. 新建 Pull Request
