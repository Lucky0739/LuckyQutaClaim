# Lucky Seckill Mall

## Project Introduction

Lucky Seckill Mall is a high-performance e-commerce platform built on a microservices architecture. Its core highlights lie in the implementation of a **high-concurrency Seckill system** and the integration of **AI Intelligent Customer Service**. The project utilizes the Spring Cloud ecosystem, split into multiple independent services such as User, Goods, Order, Payment, Seckill, Activity, and AI, ensuring system high availability and scalability.

## Core Features

### 1. High-Concurrency Seckill System
- **Stock Warm-up**: Through the `SeckillWarmUpScheduler`, the system periodically scans upcoming Seckill activities to synchronize inventory from the database to Redis, preventing the database from being overwhelmed instantly.
- **Atomic Deduction**: Uses Lua scripts (`seckill_deduct_stock.lua`) to execute inventory deduction within Redis, ensuring inventory is not oversold under high concurrency.
- **Async Order**: Seckill requests are processed asynchronously via RocketMQ (`SeckillOrderProducer` -> `SeckillOrderConsumer`), significantly improving throughput.
- **Dead Letter Queue (DLQ)**: Messages that fail processing are automatically routed to a dead letter queue (`SeckillOrderDlqConsumer`), supporting retries and manual intervention.

### 2. AI Intelligent Customer Service
- **RAG (Retrieval-Augmented Generation)**: Based on Spring AI and the Alibaba DashScope large model, integrated with a PGVector vector database.
- **Knowledge Base Management**: Automatically reads and chunks loads knowledge base documents, providing precise shopping consultation support to users through vector similarity matching.

### 3. Complete Business Process
- **User & Security**: Implements permission control based on Sa-Token (RBAC), supporting user registration/login and role management (Regular User/Merchant/Admin).
- **Goods & Shop**: Product management, categories, favorites, shop management, and review systems.
- **Transaction Chain**: Cart -> Order -> Payment (Alipay Integration) -> Shipping -> Receipt -> Review.
- **Notification System**: In-site message notifications (Order status, System announcements).

## Technology Stack

- **Core Frameworks**: Spring Boot 3.x, Spring Cloud Gateway
- **Data Storage**: MySQL (Main Data), Redis (Cache/Distributed Lock/Seckill Inventory), PGVector (AI Vector Database)
- **Message Queue**: Apache RocketMQ (Asynchronous Decoupling, Peak Shaving/Valley Filling)
- **AI Capabilities**: Spring AI, DashScope (Tongyi Qianwen), Embedding Models
- **Traffic Protection**: Alibaba Sentinel (API Gateway Rate Limiting)
- **Object Storage**: Tencent Cloud COS
- **Security Authentication**: Sa-Token

## Project Structure

The project adopts a Maven multi-module structure:

*   **lucky-seckill-common**: Common dependency module, containing entity classes, utility classes, general configuration, MQ constant definitions, Lua scripts, etc.
*   **lucky-seckill-gateway**: API Gateway, responsible for routing forwarding, identity verification, and Sentinel rate limiting.
*   **lucky-seckill-user**: User service (User info, roles, address, balance, verification codes).
*   **lucky-seckill-goods**: Goods service (Product details, categories, favorite rankings, search with Elasticsearch integration).
*   **lucky-seckill-shop**: Shop service (Shop info, shop favorites, merchant entry audit).
*   **lucky-seckill-cart**: Shopping cart service.
*   **lucky-seckill-order**: Order service (Order process, payment callbacks, expiration closure, Seata distributed transaction demo).
*   **lucky-seckill-pay**: Payment service (Alipay Sandbox/Formal payment, Balance payment).
*   **lucky-seckill-notify**: Notification Center (Message storage and push).
*   **lucky-seckill-activity**: **Core Module**. Contains Seckill activity management, Redis inventory operations, MQ consumers, and warm-up schedulers.
*   **lucky-seckill-ai**: **AI Module**. Official customer service chat interface, knowledge base loading and management.

## Key Code Logic Analysis

### 1. Seckill Inventory Warm-up (`SeckillWarmUpScheduler`)
The system loads Seckill product inventory from the DB to Redis within a certain time window before the activity starts (via scheduled task scanning).
```java
// Pseudo-code logic
public void scanActivityStatus() {
    // 1. Scan activities starting soon (10 mins before start)
    // 2. Call warmUpActivityStock to write inventory to Redis
}
```

### 2. Lua Script Ensures Atomicity
To avoid concurrency issues during Redis inventory operations, a Lua script is defined:
- **Deduct Inventory**: Verify user purchase quantity and frequency limits -> Deduct inventory -> Record purchased flag.
- **Rollback Inventory**: Release inventory when downstream order creation fails.

### 3. Message Queue Asynchronization
After a user clicks Seckill, the request returns directly as "Queuing", and the actual order creation is executed after `SeckillOrderConsumer` consumes the message.
- **Success**: Create Order -> Deduct Redis Inventory -> Send Payment Notification.
- **Failure**: Enter DLQ (Dead Letter Queue).

## Quick Start

1.  **Environment Preparation**:
    *   JDK 17+
    *   MySQL 8.0
    *   Redis 6+
    *   RocketMQ
    *   PGVector (For AI Module)
    *   Recommended: Prepare Nacos or Configuration Center

2.  **Build Project**:
    ```bash
    mvn clean install -DskipTests
    ```

3.  **Run Microservices**:
    Start `lucky-seckill-common` (Common Dependencies) first, then start other services in dependency order (typically Gateway -> User -> Goods -> Shop -> Cart -> Notify -> Activity -> Order -> Pay).

## Acknowledgements

Thanks to JetBrains for providing development tool support.

## License

This project is for learning and communication purposes only.