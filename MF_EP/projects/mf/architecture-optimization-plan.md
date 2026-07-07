# 单体架构优化详情列表

## 背景

当前项目后端属于多模块分层单体架构：

```text
mf-fertilizer
├── fertilizer-common    # 公共实体、DTO、VO、常量、异常、工具
├── fertilizer-core      # Mapper、Service、ServiceImpl
└── fertilizer-api       # Controller、配置、切面、定时任务、启动入口
```

整体方向不是立刻拆微服务，而是先把单体内部边界整理清楚，让代码更容易维护、测试、扩展和排查问题。

本次建议优先优化以下 7 项：

1. AI 调用配置化与服务抽象
2. 订单模块业务收口
3. 按业务域整理包结构
4. 明确事务边界
5. 订单状态机化
6. Redis 与缓存使用统一封装
7. 权限模型统一

---

## 1. AI 调用配置化与服务抽象

### 当前问题

Java 后端通过 `AiClient` 调用 Python FastAPI AI 服务，但服务地址目前写死在代码中：

```java
private static final String AI_BASE = "http://localhost:5000/api";
```

这种方式在本地开发可以跑，但后续如果部署到服务器、Docker、测试环境、生产环境，修改地址就需要改代码、重新打包。

### 优化目标

把 AI 服务调用从 Controller 中解耦出来，并把 AI 服务地址、超时时间、开关等配置放到 `application.yml`。

### 建议结构

```text
fertilizer-api
└── config
    └── AiProperties.java

fertilizer-core
└── service
    └── AiService.java

fertilizer-core
└── serviceImpl
    └── PythonAiServiceImpl.java

fertilizer-api
└── client
    └── AiClient.java
```

### 配置示例

```yaml
mf:
  ai:
    enabled: true
    base-url: http://localhost:5000/api
    connect-timeout-ms: 5000
    read-timeout-ms: 120000
```

### 具体改法

1. 新增 `AiProperties`，用 `@ConfigurationProperties(prefix = "mf.ai")` 读取配置。
2. `AiClient` 不再写死地址，改为从 `AiProperties` 获取。
3. 新增 `AiService` 接口，Controller 不直接依赖 `AiClient`。
4. `AdminAiController` 和 `ClientAiController` 只调用 `AiService`。
5. 增加 AI 服务不可用时的统一降级返回。

### 收益

- 部署环境切换更方便。
- Controller 更干净。
- 后续替换 DeepSeek、替换 Python 服务、增加重试和熔断时影响更小。

---

## 2. 订单模块业务收口

### 当前问题

订单业务通常会涉及多个动作：

```text
创建订单
扣减库存
生成订单项
支付记录
取消订单
超时关闭
发货
确认收货
积分变更
消息通知
```

如果这些逻辑散落在 Controller、ServiceImpl、Task 中，后续很容易出现重复逻辑和状态不一致。

### 优化目标

把订单核心流程收口到统一的订单应用服务中，由它协调库存、订单项、支付、积分、消息等服务。

### 建议结构

```text
fertilizer-core
└── service
    └── OrderApplicationService.java

fertilizer-core
└── serviceImpl
    └── OrderApplicationServiceImpl.java
```

### 建议职责

```text
OrderApplicationService
├── createOrder()          # 创建订单
├── cancelOrder()          # 取消订单
├── payOrder()             # 支付成功处理
├── shipOrder()            # 后台发货
├── completeOrder()        # 确认收货
└── closeTimeoutOrders()   # 超时关闭订单
```

### Controller 职责调整

```text
ClientOrderController
└── 只处理 C 端请求入参、用户身份、响应包装

AdminOrderController
└── 只处理后台管理请求、管理员权限、响应包装

OrderApplicationService
└── 负责真正的订单业务流程编排
```

### 收益

- 下单、取消、支付、发货逻辑更集中。
- 管理端和 C 端可以复用同一套订单规则。
- 更容易加事务和测试。

---

## 3. 按业务域整理包结构

### 当前问题

现在代码主要按技术层分包：

```text
controller
service
serviceImpl
mapper
entity
dto
vo
```

这种结构在项目早期很清晰，但业务增多后，查一个业务要在多个目录之间来回跳。

### 优化目标

逐步从纯技术分层，演进到“技术分层 + 业务域聚合”的结构。

### 推荐优先整理的业务域

```text
order       # 订单、订单项、支付、库存扣减、超时关闭
product     # 商品、分类、商品详情、库存
user        # 用户、地址、会员、积分
content     # 百科、文章、评论、点赞、收藏、浏览记录
platform    # FAQ、反馈、活动、消息、配置、日志
ai          # AI 客服、文章草稿、百科草稿、知识库重建
```

### 第一阶段建议

先不要大规模搬所有文件，优先整理最核心、最容易出问题的两个域：

```text
order
product
```

可以先在包名上做聚合，例如：

```text
com.mf.fertilizer.order
com.mf.fertilizer.product
com.mf.fertilizer.user
com.mf.fertilizer.content
```

### 注意事项

1. 不建议一次性移动全部文件，容易引发大量 import 变化。
2. 优先从新增服务开始采用业务域包结构。
3. 老代码可以逐步迁移，不要为了目录漂亮影响稳定性。

### 收益

- 查业务逻辑更快。
- 后续拆分模块或服务时更自然。
- 代码边界更接近真实业务。

---

## 4. 明确事务边界

### 当前问题

电商类业务里，订单、库存、支付、积分、消息之间有强一致性要求。如果事务边界不清楚，可能出现：

```text
订单创建了，但订单项没创建
库存扣了，但订单创建失败
订单取消了，但库存没退
支付成功了，但订单状态没变
积分发放重复
```

### 优化目标

把关键业务流程放在明确的事务方法中，由应用服务统一控制。

### 建议加事务的方法

```java
@Transactional(rollbackFor = Exception.class)
public OrderVO createOrder(OrderCreateDTO dto) {
    // 校验商品
    // 扣减库存
    // 创建订单
    // 创建订单项
    // 创建支付记录
    // 事务提交后再发送通知
}
```

### 重点流程

```text
createOrder          创建订单
cancelOrder          取消订单
payOrder             支付成功
shipOrder            发货
completeOrder        确认收货
closeTimeoutOrders   超时关闭订单
grantPoints          发放积分
```

### 异步任务注意

消息通知、系统消息、日志记录可以异步执行，但不要破坏主事务。

推荐方式：

```text
主事务提交成功
-> 发布领域事件或调用异步通知
-> NotificationService 异步处理消息
```

### 收益

- 数据一致性更强。
- 出错时可以整体回滚。
- 更容易定位订单异常。

---

## 5. 订单状态机化

### 当前问题

如果订单状态判断散落在多个 Controller 或 Service 中，后续容易出现非法流转，例如：

```text
已取消订单继续支付
未支付订单直接发货
已完成订单再次取消
已退款订单继续确认收货
```

### 优化目标

集中管理订单状态和状态流转规则。

### 建议状态

```text
PENDING_PAYMENT   待支付
PAID              已支付
SHIPPED           已发货
COMPLETED         已完成
CANCELLED         已取消
REFUNDED          已退款
```

### 建议流转

```text
PENDING_PAYMENT -> PAID
PENDING_PAYMENT -> CANCELLED
PAID            -> SHIPPED
PAID            -> REFUNDED
SHIPPED         -> COMPLETED
SHIPPED         -> REFUNDED
```

### 建议结构

```text
fertilizer-common
└── constant
    └── OrderStatus.java

fertilizer-core
└── service
    └── OrderStatusService.java

fertilizer-core
└── serviceImpl
    └── OrderStatusServiceImpl.java
```

### 示例方法

```java
public void checkCanPay(OrderEntity order);

public void checkCanCancel(OrderEntity order);

public void checkCanShip(OrderEntity order);

public void checkCanComplete(OrderEntity order);

public OrderStatus nextStatus(OrderStatus current, OrderEvent event);
```

### 收益

- 订单状态规则集中。
- 防止非法操作。
- 后续新增退款、售后、部分发货时更容易扩展。

---

## 6. Redis 与缓存使用统一封装

### 当前问题

项目中 Redis 可能同时承担：

```text
缓存
库存扣减
分布式锁
验证码
登录态辅助
定时任务防重复执行
```

如果到处直接操作 `RedisTemplate`，容易出现 key 命名混乱、过期时间不一致、锁释放不规范等问题。

### 优化目标

统一 Redis key、统一 Redis 操作入口、统一缓存策略。

### 建议结构

```text
fertilizer-common
└── constant
    └── RedisKey.java

fertilizer-core
└── service
    ├── CacheService.java
    ├── StockService.java
    └── DistributedLockService.java

fertilizer-core
└── serviceImpl
    ├── CacheServiceImpl.java
    ├── StockServiceImpl.java
    └── RedisDistributedLockServiceImpl.java
```

### RedisKey 建议

```text
product:detail:{id}
product:stock:{id}
order:lock:{orderId}
order:timeout:task:lock
verification:code:{phone}
user:token:{userId}
```

### 分布式锁建议

封装统一方法：

```java
boolean tryLock(String key, String value, Duration expire);

void unlock(String key, String value);
```

释放锁时必须校验 value，避免误删其他线程或其他实例的锁。

### 缓存建议

1. 查询类接口优先使用 Spring Cache。
2. 库存、锁、验证码这类强业务 Redis 操作用专门服务封装。
3. 所有 key 统一从 `RedisKey` 生成。
4. 所有过期时间集中管理，避免魔法数字散落。

### 收益

- Redis 使用更安全。
- 缓存策略更容易统一调整。
- 后续排查缓存问题更快。

---

## 7. 权限模型统一

### 当前问题

项目有管理端和 C 端两类入口：

```text
admin/*
client/*
```

如果权限判断分散在各个 Controller 或拦截器细节中，后续容易出现越权访问、重复判断、权限规则不清晰等问题。

### 优化目标

统一认证、统一用户上下文、统一权限注解。

### 建议权限类型

```text
PUBLIC        无需登录
CLIENT        C 端用户
ADMIN         后台管理员
SUPER_ADMIN   超级管理员
```

### 建议结构

```text
fertilizer-common
└── annotation
    └── RequireRole.java

fertilizer-common
└── constant
    └── RoleEnum.java

fertilizer-api
└── config
    └── JwtInterceptorConfig.java

fertilizer-api
└── interceptor
    └── AuthInterceptor.java
```

### 注解示例

```java
@RequireRole(RoleEnum.ADMIN)
@GetMapping("/admin/orders")
public ResultVO<?> listOrders() {
    // ...
}
```

### 拦截逻辑建议

```text
1. 判断接口是否公开
2. 解析 JWT
3. 写入 UserContext
4. 判断角色是否满足接口要求
5. 请求完成后清理 UserContext
```

### 权限边界建议

```text
PUBLIC:
  浏览商品、百科、文章、FAQ

CLIENT:
  下单、购物车、收藏、评论、个人中心、消息

ADMIN:
  商品管理、订单管理、文章管理、百科管理、反馈处理

SUPER_ADMIN:
  管理员管理、平台配置、敏感日志查看
```

### 收益

- 权限规则更清楚。
- Controller 不需要反复写身份判断。
- 降低越权访问风险。

---

## 建议实施顺序

### 第一阶段：低风险基础优化

1. AI 调用配置化与服务抽象
2. Redis key 和缓存封装
3. 权限模型梳理

这些优化对核心业务流程影响较小，适合作为第一批调整。

### 第二阶段：订单核心流程优化

1. 订单模块业务收口
2. 明确事务边界
3. 订单状态机化

这部分会影响下单、支付、取消、发货等核心流程，改动前需要先梳理现有接口和数据库字段。

### 第三阶段：包结构按业务域演进

1. 先整理 `order` 域。
2. 再整理 `product` 域。
3. 最后逐步整理 `user`、`content`、`platform`、`ai`。

目录迁移建议小步进行，每次迁移一个业务域，保证能编译、能运行、能回归。

---

## 推荐验收标准

### 代码层面

```text
Controller 中不直接写复杂业务流程
AI 服务地址不再硬编码
订单状态流转规则集中管理
Redis key 不再散落拼接
权限判断不再分散在多个 Controller 中
```

### 功能层面

```text
普通商品查询正常
创建订单正常
取消订单正常
后台发货正常
超时订单关闭正常
AI 客服正常
AI 服务不可用时 Java 后端能返回友好错误
```

### 稳定性层面

```text
下单失败时不会残留半成品订单
库存扣减和订单创建保持一致
非法订单状态流转会被拦截
Redis 锁不会误释放
未登录用户不能访问需要登录的接口
普通用户不能访问后台接口
```

---

## 总结

这次优化的核心不是把项目拆成更多服务，而是先把单体内部整理成更有边界的结构：

```text
接口层更薄
业务层更集中
事务边界更明确
状态流转更可控
缓存和权限更统一
业务域更清晰
```

这样做完之后，这个项目仍然是单体，但会从“能跑的单体”升级为“更容易维护和扩展的模块化单体”。
