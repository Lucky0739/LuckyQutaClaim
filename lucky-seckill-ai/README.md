# AI模块 - 官方客服聊天功能

## 模块说明

本模块为秒杀商城提供AI官方客服聊天功能，基于Spring AI和阿里云百练大模型实现。AI客服"小幸运"能够回答用户关于订单、商品、支付、配送、退换货等各类购物问题。

## 技术栈

- Spring Boot 3.3.5
- Java 21
- Spring AI 1.0.0
- Spring AI Alibaba 1.0.0.2
- 阿里云百练（DashScope）大模型

## 主要功能

### 1. 同步聊天接口
- **接口路径**: `GET /ai/customer/chat`
- **参数**:
  - `message`: 用户消息（必填）
  - `chatId`: 会话ID（可选）
- **返回**: JSON格式的统一响应

### 2. 流式聊天接口（SSE）
- **接口路径**: `GET /ai/customer/chat/stream`
- **参数**:
  - `message`: 用户消息（必填）
  - `chatId`: 会话ID（可选）
- **返回**: Server-Sent Events流

### 3. SSE聊天接口
- **接口路径**: `GET /ai/customer/chat/sse`
- **参数**:
  - `message`: 用户消息（必填）
  - `chatId`: 会话ID（可选）
- **返回**: 标准SSE格式数据

## 知识库功能

### 知识库文档
知识库位于 `src/main/resources/document/` 目录，包含：

1. **秒杀商城客服知识库.md** - 完整的客服知识库
   - 平台介绍
   - 订单问题（查询、修改、取消等）
   - 商品问题（正品保证、退换货等）
   - 支付问题（支持的支付方式、失败处理等）
   - 配送问题（发货时间、物流查询等）
   - 退换货问题（政策、流程、退款等）
   - 会员与积分（等级、积分规则等）
   - 优惠活动（秒杀、优惠券等）
   - 账户安全

2. **秒杀商城常见问题FAQ.md** - 快速问答
   - 关于秒杀活动
   - 关于订单处理
   - 关于支付问题
   - 关于退换货
   - 关于会员积分
   - 关于配送

### 知识库加载机制
- 系统启动时自动加载知识库文档
- 每次请求时会将知识库内容注入到AI提示词中
- 确保AI客服的回答基于最新的知识库内容

## AI客服角色设定

**小幸运** - 幸运秒杀商城官方智能客服

### 服务原则
- 使用友好、专业的语气
- 使用"您"称呼用户
- 回答简洁明了，突出重点
- 无法回答时引导联系人工客服（400-888-8888）

### 禁止行为
- 不索要用户密码、验证码等敏感信息
- 不承诺超出平台政策的事情
- 不处理复杂退款、赔偿等需人工核实的问题
- 不透露系统内部信息

## 配置说明


## 启动方式

1. **设置环境变量**:
```bash
export DASHSCOPE_API_KEY=your-api-key
```

2. **编译项目**:
```bash
mvn clean compile
```

3. **启动服务**:
```bash
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8123`

## API文档

启动服务后访问：
- Swagger UI: http://localhost:8123/swagger-ui.html
- Knife4j: http://localhost:8123/doc.html

## 项目结构

```
lucky-seckill-ai/
├── src/main/java/com/lucky/seckill/ai/
│   ├── AiApplication.java              # 启动类
│   ├── config/                         # 配置类
│   │   ├── AiConfig.java
│   │   └── CorsConfig.java
│   ├── controller/                      # 控制器
│   │   ├── CustomerServiceController.java
│   │   └── HealthController.java
│   ├── service/                        # 服务层
│   │   ├── CustomerService.java
│   │   └── impl/
│   │       ├── CustomerServiceImpl.java
│   │       └── KnowledgeBaseService.java
│   └── model/                          # 数据模型
│       ├── request/
│       └── response/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── log4j2-spring.xml
│   └── document/
│       ├── 秒杀商城客服知识库.md
│       └── 秒杀商城常见问题FAQ.md
└── pom.xml
```

## 维护知识库

### 添加新知识
1. 编辑 `src/main/resources/document/秒杀商城客服知识库.md`
2. 添加新的FAQ到 `src/main/resources/document/秒杀商城常见问题FAQ.md`
3. 重启服务以加载更新后的知识库

### 知识库格式
- 使用Markdown格式编写
- 保持结构清晰，便于AI理解
- 定期更新以保持内容时效性

## 注意事项

1. 请确保已正确配置阿里云百练API Key
2. 模型默认为 `qwen-plus`，可根据需要修改
3. 服务端口默认为 `8123`，可在配置文件中修改
4. 需要启用Nacos服务发现时，确保Nacos服务已启动
5. 知识库会在服务启动时自动加载
6. 如需更新知识库，需要重启服务

## 联系与支持

- **客服热线**: 400-888-8888（工作时间 9:00-21:00）
- **在线客服**: APP内"我的-客服中心"
- **技术支持**: 联系开发团队