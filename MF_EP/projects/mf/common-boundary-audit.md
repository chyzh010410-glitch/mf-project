# common 边界审计

## 当前结论

`fertilizer-common` 当前承担了三类职责：

1. 真正通用基础能力：注解、常量、上下文、异常、通用返回结构、工具类。
2. 领域模型：所有数据库实体目前都放在 `entity`。
3. 接口适配模型：后台和客户端 DTO/VO 目前都放在 `dto`、`vo`。

这能运行，但 `common` 仍偏大。建议后续拆分时不要一次性搬空，而是按业务域逐步迁移。

## 当前分布

```text
annotation: 2
constant: 7
context: 1
dto: 28
entity: 33
exception: 1
util: 2
vo: 13
```

## 建议保留在 common 的内容

```text
annotation
constant
context
exception
util
vo/ResultVO.java
vo/PageVO.java
```

这些属于跨业务域基础能力，放在 common 是合理的。

## 建议后续迁出的内容

### 领域模型

当前 `entity` 下有 33 个实体，建议未来迁到独立 `fertilizer-domain` 模块，或先按业务域包迁移：

```text
order: OrderEntity, OrderItem, Payment, ShoppingCartItem
product: Product, ProductCategory, ProductDetail
user: User, SysUser, AdminRole, UserAddress, PointsRecord, MembershipLevel, VerificationCode
content: EncyclopediaArticle, EncyclopediaEntry, CommunityComment, CommunityLike, Favorite, BrowsingHistory
platform: ActivityEntity, Faq, Feedback, Message, PlatformConfig, SystemLog, FileUpload, UserUpload
fertilization: Fertilizer, Tree, FertilizationRule, FertilizationRecord
```

### DTO

`dto/admin` 和 `dto/client` 已经按入口端区分，但更适合跟随业务域移动：

```text
order/dto: OrderCreateDTO, OrderShipDTO
product/dto: ProductSaveDTO
user/dto: UserLoginDTO, UserRegisterDTO, PasswordResetDTO, AddressSaveDTO
content/dto: ArticleSaveDTO, EncyclopediaSaveDTO, CommentSaveDTO
platform/dto: ActivitySaveDTO, FaqSaveDTO, MessageSendDTO, PlatformConfigSaveDTO, FeedbackSubmitDTO, UploadReviewDTO, UploadSubmitDTO
fertilization/dto: FertilizerQueryDTO, TreeQueryDTO, FertilizationRuleDTO, FertilizationRecordDTO, RecommendRequestDTO, RecordQueryDTO
```

### VO

客户端展示 VO 建议随业务域迁移：

```text
order/vo: OrderVO, OrderCreateResultVO
product/vo: ProductVO, CartVO
user/vo: UserLoginResultVO
content/vo: EncyclopediaArticleVO, EncyclopediaEntryVO
platform/vo: HomePageVO
fertilization/vo: RecommendResultVO, StatsVO
```

## 不建议现在立刻做的事

1. 不建议立刻新增 Maven 模块 `fertilizer-domain` 并搬全部实体。
2. 不建议一次性迁移所有 DTO/VO，会产生大量 import 变化。
3. 不建议在没有测试覆盖前改实体包名，因为 MyBatis、序列化、前端字段依赖都可能受影响。

## 建议执行顺序

1. 先保持当前模块结构不变，只在新增代码中遵循业务域包结构。
2. 下次需要改某个业务域时，顺手迁移该域 DTO/VO。
3. 实体迁移最后做，并单独验证 MyBatis、JSON 序列化和接口兼容性。
4. 如果后续项目规模继续变大，再考虑新增 `fertilizer-domain` 模块。
