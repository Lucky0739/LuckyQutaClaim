

# Lucky Seckill Mall (幸运秒杀商城)

## 项目简介

Lucky Seckill Mall 是一个基于微服务架构构建的高性能电商平台，核心亮点在于**高并发秒杀系统**的实现与**AI智能客服**的集成。项目采用 Spring Cloud 生态，拆分为用户、商品、订单、支付、秒杀、活动、AI 等多个独立服务，确保系统的高可用性与可扩展性。

## 核心特性

### 1. 高并发秒杀系统 (High-Concurrency Seckill)
- **库存预热 (Stock Warm-up)**: 通过 `SeckillWarmUpScheduler` 定时扫描即将开始的秒杀活动，将数据库中的库存同步至 Redis，防止数据库被瞬间击垮。
- **原子性扣减 (Atomic Deduction)**: 利用 Lua 脚本 (`seckill_deduct_stock.lua`) 在 Redis 中执行库存扣减，保证在高并发下库存不超卖。
- **异步下单 (Async Order)**: 秒杀请求通过 RocketMQ 异步处理 (`SeckillOrderProducer` -> `SeckillOrderConsumer`)，大幅提高吞吐量。
- **死信队列 (DLQ)**: 对于处理失败的消息，自动转入死信队列 (`SeckillOrderDlqConsumer`)，支持重试和人工介入处理。

### 2. AI 智能客服 (AI Customer Service)
- **RAG 检索增强生成**: 基于 Spring AI 和阿里 DashScope 大模型，集成了 PGVector 向量数据库。
- **知识库管理**: 自动读取并分块加载知识库文档，通过向量相似度匹配，为用户提供精准的购物咨询支持。

### 3. 完整的业务流程
- **用户与安全**: 基于 Sa-Token 实现权限控制 (RBAC)，支持用户注册登录、角色管理 (普通用户/商家/管理员)。
- **商品与店铺**: 商品管理、分类、收藏、店铺管理、评价体系。
- **交易链路**: 购物车 -> 下单 -> 支付 (对接支付宝) -> 发货 -> 收货 -> 评价。
- **通知系统**: 站内信通知 (订单状态、系统公告)。

## 技术栈

- **核心框架**: Spring Boot 3.x, Spring Cloud Gateway
- **数据存储**: MySQL (主数据), Redis (缓存/分布式锁/秒杀库存), PGVector (AI 向量库)
- **消息队列**: Apache RocketMQ (异步解耦、削峰填谷)
- **AI 能力**: Spring AI, DashScope (通义千问), Embedding Models
- **流量防护**: Alibaba Sentinel (API 网关限流)
- **对象存储**: 腾讯云 COS
- **安全认证**: Sa-Token

## 项目结构

项目采用 Maven 多模块结构：

*   **lucky-seckill-common**: 公共依赖模块，包含实体类、工具类、通用配置、MQ常量定义、Lua脚本等。
*   **lucky-seckill-gateway**: API 网关，负责路由转发、身份校验及 Sentinel 限流。
*   **lucky-seckill-user**: 用户服务 (用户信息、角色、地址、余额、验证码)。
*   **lucky-seckill-goods**: 商品服务 (商品详情、分类、收藏排行、搜索 Elasticsearch 集成)。
*   **lucky-seckill-shop**: 店铺服务 (店铺信息、店铺收藏、商家入驻审核)。
*   **lucky-seckill-cart**: 购物车服务。
*   **lucky-seckill-order**: 订单服务 (订单流程、支付回调、过期关闭、Seata 分布式事务演示)。
*   **lucky-seckill-pay**: 支付服务 (支付宝沙箱/正式支付、余额支付)。
*   **lucky-seckill-notify**: 通知中心 (消息存储与推送)。
*   **lucky-seckill-activity**: **核心模块**。包含秒杀活动管理、Redis库存操作、MQ消费者、预热调度器。
*   **lucky-seckill-ai**: **AI模块**。官方客服聊天接口、知识库加载与管理。

## 关键代码逻辑解析

### 1. 秒杀库存预热 (`SeckillWarmUpScheduler`)
系统在活动开始前一定时间窗口（通过定时任务扫描），将秒杀商品的库存从 DB 加载到 Redis。
```java
// 伪代码逻辑
public void scanActivityStatus() {
    // 1. 扫描即将开始的活动 (开始前10分钟)
    // 2. 调用 warmUpActivityStock 将库存写入 Redis
}
```

### 2. Lua 脚本保证原子性
Redis 操作库存时，为避免并发问题，定义了 Lua 脚本：
- **扣减库存**: 校验用户购买数量、频率限制 -> 扣减库存 -> 记录已购标志。
- **回滚库存**: 下游订单创建失败时，释放库存。

### 3. 消息队列异步化
用户点击秒杀后，请求直接返回 "排队中"，实际订单创建由 `SeckillOrderConsumer` 消费消息后执行。
- **成功**: 创建订单 -> 扣减 Redis 库存 -> 发送支付通知。
- **失败**: 进入 DLQ (死信队列)。

## 快速启动

1.  **环境准备**:
    *   JDK 17+
    *   MySQL 8.0
    *   Redis 6+
    *   RocketMQ
    *   PGVector (用于 AI 模块)
    *   建议准备 Nacos 或配置文件中心

2.  **构建项目**:
    ```bash
    mvn clean install -DskipTests
    ```

3.  **运行微服务**:
    依次启动 `lucky-seckill-common` (公共依赖)，然后按依赖顺序启动其他服务（通常 Gateway -> User -> Goods -> Shop -> Cart -> Notify -> Activity -> Order -> Pay）。

## 鸣谢

感谢 JetBrains 提供的开发工具支持。

## License

本项目仅供学习交流使用。