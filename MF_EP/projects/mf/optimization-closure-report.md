# 架构优化阶段性收尾报告

## 结论

截至本轮收尾审计，除 AI 调用配置化按约定暂缓外，当前单体架构优化主线已经基本完成，可以进入阶段性收工状态。

本轮优化后的项目仍然是单体，但已经从单纯技术分层，推进到更清晰的业务域分层单体：

```text
fertilizer-common       通用能力、常量、上下文、异常、返回结构、基础工具
fertilizer-core         业务域、应用服务、领域服务、基础设施封装
fertilizer-api          Controller、鉴权拦截、AOP、定时任务、请求适配
mf-frontend             管理端前端
mf-frontend-client      C 端前端
mf-ai                   AI 服务，保持独立
```

## 已完成

### 1. Redis key 与缓存封装

- 统一整理 `RedisKey`。
- 引入并使用 `CacheService`、`StockService`、`DistributedLockService` 等封装。
- 定时任务分布式锁通过统一服务入口处理。

### 2. 权限与用户上下文

- 增加 `UserContext`。
- 增加角色枚举与权限注解。
- JWT 拦截逻辑写入当前用户上下文。
- Controller 不再反复解析当前用户身份。

### 3. 订单业务收口

- 订单 C 端和管理端入口下沉到 `OrderApplicationService`。
- 订单创建、取消、发货、确认收货、超时关闭等流程集中编排。
- 订单状态规则集中到订单状态服务。
- 补充订单状态服务测试。

### 4. 业务域分包

后端已经围绕以下业务域逐步整理：

```text
order
product
user
content
platform
fertilization
infra
ai
```

Controller、应用服务、服务、Mapper、DTO/VO/Entity 已经大幅按业务域聚合。

### 5. Controller 瘦身

- 管理端和 C 端主要 Controller 已改为调用应用服务。
- 非 AI Controller 未发现直接 CRUD、Mapper、旧 Service 业务泄漏。
- 当前保留的基础设施例外：
  - `OperationLogAspect` 写操作日志。
  - `GlobalExceptionHandler` 统一异常返回。
  - `JwtInterceptorConfig` 鉴权上下文处理。
  - `OrderTimeoutTask` 定时任务编排。

### 6. common 边界优化

- `fertilizer-common` 已向通用能力收敛。
- 通用分页 `PageVO` 增加便利工厂方法，减少分页重复代码。
- 补充 `PageVOTest`。
- 实体、DTO、VO 已按业务域迁移了主要部分。
- 不建议继续一次性强迁所有残余模型，避免 MyBatis、JSON 序列化和接口兼容风险。

### 7. 前端 API 边界

- 管理端和 C 端页面大部分已改为调用 `src/api/*` 封装。
- 非 AI 视图层直接 `request` 已清理。
- 当前视图层直接 `request` 只剩 AI 页面，符合“AI 暂不优化、保持硬编码接口”的约定。

### 8. 前端重复展示逻辑

- 订单状态集中到 `orderStatus`。
- 金额格式化集中到 `formatCurrency` / `formatAmount`。
- 管理端、C 端金额旧写法已清理。
- 扫描只剩格式化工具自身使用 `toFixed(2)`。

## 暂缓项

### AI 调用配置化

按当前约定暂不优化 AI 调用配置：

- 不把 AI 地址配置迁入 `application.yml`。
- 不做 AI client 配置化。
- 不做 AI 降级/熔断/超时策略扩展。
- AI 仍保持独立模块。
- 前端 AI 页面保留直接硬编码调用方式，后续重做 AI 时再统一调整。

## 当前验证结果

最近一次收尾审计已执行：

```text
后端: mvn test
结果: BUILD SUCCESS
测试: 14 passed, 0 failed

管理端: npm run build
结果: success

C 端: npm run build
结果: success
```

前端构建仍有既有 warning：

```text
VueUse PURE annotation warning
chunk size larger than 500 kB warning
```

这些 warning 不影响构建通过，也不是本轮优化引入的功能错误。

## 当前扫描结果

### 后端

- 未发现旧 `com.mf.fertilizer.service.*`、`serviceImpl.*`、`mapper.*` 包引用残留。
- 非 AI Controller 未发现直接 CRUD/分页/Mapper 操作。
- `ResultVO.fail` 主要集中在全局异常处理，属于合理边界。

### 前端

- `src/api/*` 内使用 `request` 属于正常 API 封装。
- 视图层直接 `request` 只剩 AI 页面。
- 金额旧写法扫描只剩 `format.js` 工具内部。
- 订单状态映射集中在 `orderStatus`。

## 后续可选优化

这些不是当前阶段必须项，可以后续按需做：

1. 时间格式化工具统一。
2. 前端表格分页参数进一步封装。
3. 管理端部分表单校验文案统一。
4. `dist`、`target` 构建产物是否纳入版本管理的策略确认。
5. 未来单独重做 AI 模块：
   - AI 配置化。
   - AI 服务抽象。
   - AI 降级策略。
   - 前端 AI API 封装。

## 阶段性验收建议

当前可以按如下口径验收：

```text
非 AI 架构主线: 已完成
后端分层边界: 已完成
业务域聚合: 已完成主要域
Controller 瘦身: 已完成
订单应用服务收口: 已完成
Redis/权限/上下文统一: 已完成
前端非 AI API 边界: 已完成
金额/订单状态重复逻辑: 已完成
AI 配置优化: 暂缓
```

建议下一步不再继续扩大重构面，先进入人工功能回归或提交前整理阶段。
