 秒杀商城项目，基于 Spring Cloud 微服务架构，采用主流技术栈实现高并发、高可用的秒杀业务场景。项目涵盖用户、商品、订单、购物车、支付、消息通知、活动管理、AI
  智能服务等核心模块。

  #### 软件架构
  基于 Spring Cloud 微服务 + MyBatis + MySQL，共 11 个子模块：

  lucky-seckill-mall
  ├── lucky-seckill-common     # 通用模块：公共工具类、统一返回体、异常处理、通用配置
  ├── lucky-seckill-gateway    # 网关模块：统一入口，负责路由转发、鉴权、限流
  ├── lucky-seckill-user       # 用户模块：注册、登录、认证鉴权（Sa-Token）、个人信息管理
  ├── lucky-seckill-goods      # 商品模块：商品管理、分类管理、库存管理、商品搜索
  ├── lucky-seckill-order      # 订单模块：订单创建、查询、状态流转、秒杀订单处理
  ├── lucky-seckill-cart       # 购物车模块：购物车增删改查、商品结算
  ├── lucky-seckill-shop       # 商店模块：店铺管理、店铺信息维护
  ├── lucky-seckill-pay        # 支付模块：支付订单管理、支付回调处理
  ├── lucky-seckill-notify     # 消息模块：消息推送、短信/邮件通知、站内信
  ├── lucky-seckill-activity   # 活动模块：秒杀活动管理、活动商品审核、秒杀商品上架
  └── lucky-seckill-ai         # AI 模块：基于 Spring AI 的智能服务

  主要技术栈：
  - JDK 21
  - Spring Boot 3.3.5
  - Spring Cloud 2023.0.3 + Spring Cloud Alibaba 2023.0.3.2
  - MyBatis 3.0.5
  - Sa-Token 1.45.0（认证鉴权）
  - Spring AI 1.0.0
  - Disruptor 3.4.4（高性能队列）
  - SpringDoc 2.6.0（接口文档）
  - MySQL

  #### 安装教程

  1.  克隆项目到本地
      git clone https://gitee.com/xxx/lucky-seckill-mall.git
  2.  安装 MySQL，修改各模块 application-dev.yml 中的数据库连接信息，执行各模块 src/main/resources/db/ 下的 SQL 脚本初始化表结构
  3.  配置 Nacos 注册中心/配置中心（或按需使用其他组件），修改对应配置地址
  4.  编译打包
      cd lucky-seckill-mall
      mvn clean install -DskipTests
  5.  按顺序启动：
      mvn spring-boot:run -pl lucky-seckill-gateway
      mvn spring-boot:run -pl lucky-seckill-user,lucky-seckill-goods,lucky-seckill-shop
      mvn spring-boot:run -pl lucky-seckill-cart,lucky-seckill-order,lucky-seckill-pay
      mvn spring-boot:run -pl lucky-seckill-notify,lucky-seckill-activity,lucky-seckill-ai

  #### 使用说明

  1.  所有请求统一走网关（Gateway），网关根据路由规则转发到对应微服务
  2.  接口文档使用 Swagger/SpringDoc，访问 http://{host}:{port}/swagger-ui.html 即可查看
  3.  秒杀流程：先在后管创建秒杀活动并上架商品，用户端方可参与秒杀
  4.  AI 模块需额外配置大模型 API Key，详见 lucky-seckill-ai 下的 application-dev.yml
  5.  建议配合 Nacos 作为注册中心和配置中心，Redis 作为缓存和分布式锁，保证秒杀场景的数据一致性


 亮点

  - 🔥 Redis + Lua 原子库存扣减：库存扣减与回滚封装在 Lua 脚本中，Redis 侧原子执行，彻底杜绝超卖少卖
  - ⚡ Disruptor 异步削峰：秒杀请求通过 Disruptor 环形队列异步落库，削峰填谷，吞吐量指数级提升
  - 🛡️Sentinel 双层流控：网关 + 服务双层流控熔断，超限即时降级返回 429，防止缓存击穿与雪崩
  - 🔐 Sa-Token 无侵入鉴权：基于 Redis 的统一会话，30 天长效 Token，支持并发登录与细粒度 RBAC 权限
  - 🔗 Seata AT 分布式事务：跨服务（订单 + 库存）操作强一致，异常自动回滚，数据零风险
  - 🧠 Spring AI 智能客服：DashScope 大模型 + pgvector 向量检索，支持 RAG 文档问答，秒级响应
  - 📨 RocketMQ 异步解耦：订单通知、消息推送异步投递，核心链路极致精简
  - 🚪 Gateway 统一网关：lb:// 负载均衡 + 10s 连接 / 120s 响应超时，一站式路由管理