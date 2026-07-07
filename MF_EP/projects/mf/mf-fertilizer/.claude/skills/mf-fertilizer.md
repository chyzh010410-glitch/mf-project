---
name: mf-fertilizer
description: 开发苗丰施肥管控平台后端，基于 Java 17 + SpringBoot 3.2.5 + MyBatis-Plus + MySQL + Redis，包含管理后台和C端电商双平台，严格遵循指定技术栈、架构规范、业务需求、代码标准。
allowed-tools: Bash
argument-hint: "[项目开发指令]"
effort: high
---

## 一、固定技术栈（100% 严格遵守，不可替换）
1. **开发语言**：Java 17（LTS 长期支持版，强制使用）
2. **核心框架**：SpringBoot 3.x（适配 Java17）、Spring MVC
3. **持久层框架**：MyBatis-Plus 3.5.9
4. **数据库**：MySQL 8.0+
5. **构建工具**：Maven
6. **缓存中间件**：Redis
7. **登录认证**：JWT (jjwt 0.12.6)
8. **接口文档**：Knife4j 4.3.0 / Swagger
9. **工具库**：Lombok、Hutool 5.8.28、Spring Validation

## 二、必须集成的核心依赖
1. Lombok：简化实体类代码
2. Spring Validation：接口入参校验
3. Knife4j：自动生成可视化接口文档
4. Hutool：通用工具类（日期、计算、字符串处理）
5. JWT：无状态登录令牌生成与校验

## 三、项目标准包结构（Java17 项目）
com.mf.fertilizer
├── controller      # 接口层
│   ├── admin/      # 管理后台接口 (/admin/*)
│   └── client/     # C端接口 (/client/*)
├── service         # 业务接口
├── serviceImpl     # 业务实现（核心逻辑）
├── mapper          # MyBatis-Plus 数据访问（全部继承 BaseMapper，无 XML）
├── entity          # 数据库实体类（全部继承 BaseEntity）
├── dto             # 前端入参封装
│   ├── admin/      # 管理端 DTO
│   └── client/     # C端 DTO
├── vo              # 前端出参封装
│   └── client/     # C端 VO
├── config          # 配置类（CORS、JWT拦截器、MyBatis-Plus、Redis、Swagger、Jackson、GlobalExceptionHandler）
├── exception       # 全局异常、自定义异常 (BusinessException)
├── util            # 工具类 (JwtUtil、SeasonUtil)
└── constant        # 系统常量 (RedisKey、ResultCode、RoleEnum)

## 四、Redis 业务用途（必须实现）
1. 缓存登录用户 JWT 令牌，校验登录状态（管理端 `login:token:` 前缀，C端 `client:token:` 前缀）
2. 缓存常用树种、肥料基础数据列表（30分钟 TTL）
3. 缓存施肥智能推荐结果，避免重复计算（24小时 TTL）
4. 支持统计数据缓存优化

## 五、数据库设计规范（强制）

### 5.1 全部表清单（32张，按业务分组）

**核心施肥业务（5张）：**
`sys_user`、`tree`、`fertilizer`、`fertilization_record`、`fertilization_rule`

**C端用户（4张）：**
`user`、`user_address`、`verification_code`、`admin_role`

**电商交易（7张）：**
`product_category`、`product`、`product_detail`、`shopping_cart_item`、`order`、`order_item`、`payment`

**内容社区（7张）：**
`encyclopedia_entry`、`encyclopedia_article`、`user_upload`、`community_comment`、`community_like`、`favorite`、`browsing_history`

**平台管理（7张）：**
`platform_config`、`system_log`、`faq`、`feedback`、`activity`、`message`、`file_upload`

**积分会员（2张）：**
`membership_level`、`points_record`

### 5.2 表设计规范
1. 所有表必须包含：主键(雪花ID/BIGINT AUTO_INCREMENT)、create_time、update_time、deleted(逻辑删除)
2. 使用 MyBatis-Plus 逻辑删除（`@TableLogic`），不物理删除数据
3. 不使用数据库外键，关联由代码控制
4. 时间字段统一使用 `LocalDateTime` (Java17 标准)
5. 给查询高频字段添加索引
6. `BaseEntity` 为所有实体的抽象基类，提供 id、createTime、updateTime、deleted
7. 注意：`order` 是 MySQL 保留字，表名需用反引号

## 六、Java17 + 全局开发规范
1. **统一接口返回格式**：所有接口返回 `ResultVO<T>` (`{code, msg, data}`)
2. **全局异常处理器**：统一拦截 BusinessException、BindException、NoResourceFoundException、通用 Exception
3. **跨域配置**：支持前后端分离，允许所有来源/方法/请求头，支持凭据
4. **自动填充时间**：MyBatis-Plus MetaObjectHandler 自动赋值创建/更新时间
5. **分页查询**：所有列表接口默认分页，接受 `PageDTO`，返回 `PageVO<T>`
6. **VO/DTO 隔离**：禁止直接返回 entity 给前端
7. **接口参数校验**：使用 Spring Validation 注解 (`@Valid`)
8. **DI注入**：全部使用 `@RequiredArgsConstructor`，禁止 `@Autowired`
9. **代码风格**：遵循 Java17 规范，支持 var 关键字，代码简洁清晰
10. **Jackson 配置**：Long > 10^12 序列化为字符串防 JS 精度丢失

## 七、登录与权限（双通道 JWT）

### 7.1 管理端认证
- 路径：根级 `/login`、`/logout`
- 实体：`SysUser`（username、password/BCrypt、realName、role、status）
- Token 前缀：`login:token:<token>`
- 角色：ADMIN、OPERATOR

### 7.2 C端认证
- 路径：`/client/auth/*`
- 实体：`User`（username、password/MD5、nickname、avatar、points、membershipLevel）
- Token 前缀：`client:token:<token>`
- 注意：User 密码使用 MD5，应逐步迁移到 BCrypt

### 7.3 JWT 配置
- 算法：HMAC-SHA
- 密钥：`miaoFeiFertilizerJwtSecretKey2026!@#%`
- 有效期：7天
- JwtUtil 支持 `userType` claim（admin vs consumer）

## 八、全部业务功能

### 8.1 管理端 -- 核心施肥业务
1. 用户管理：登录、权限控制
2. 树木管理：增删改查、分页、按树种/状态/树龄筛选、树种缓存
3. 肥料管理：增删改查、分页、按名称/类型筛选、肥料列表缓存
4. 施肥记录：新增、删除、历史查询（按日期范围筛选）、数据统计（总记录/树木数/肥料类型数/总用量）
5. 施肥规则：增删改查、智能推荐（按树种+树龄范围+季节匹配规则，关联肥料信息，按优先级排序，24h缓存）

### 8.2 管理端 -- 平台管理（`/admin/*`）
6. 订单管理：订单列表、详情、发货、状态更新、统计数据

### 8.3 C端 -- 用户与交易（`/client/*`）
7. 用户认证：注册（验证码）、登录、登出、密码重置
8. 用户中心：个人资料、修改密码、多地址管理（CRUD+设默认）
9. 商品浏览：商品列表（筛选/排序/分页）、商品详情、分类列表
10. 购物车：添加、修改数量、删除、清空
11. 订单：下单、订单列表、详情、取消、确认收货

### 8.4 C端 -- 内容与社区（`/client/*`）
12. 首页聚合：Banner + 推荐商品 + 新品 + 推荐文章
13. 百科：词条列表、详情
14. 文章：列表、详情
15. 用户上传：上传树木图片+信息，管理员审核
16. 社区互动：评论（多态目标+嵌套回复）、点赞（唯一约束防重复）
17. 收藏：收藏/取消收藏
18. 浏览历史：记录浏览、清除历史

### 8.5 C端 -- 辅助功能（`/client/*`）
19. FAQ：常见问题列表
20. 意见反馈：提交反馈
21. 消息中心：消息列表、未读数、标记已读
22. 积分：积分余额、积分流水

## 九、扩展预留（未实现，预留结构）
1. 文件上传接口（FileUpload 实体已定义，schema 已有表）
2. 定时任务（施肥提醒）
3. 数据可视化统计接口
4. 以下模块的管理端控制器尚未实现：AdminRole、PlatformConfig、SystemLog、FileUpload、CommunityComment、CommunityLike、UserUpload、Payment、MembershipLevel、Encyclopedia、Article、Faq、Feedback、Message、Activity

## 十、Java17 强制要求
1. 项目 JDK 版本：Java 17
2. SpringBoot 版本：3.2.5
3. 日期 API：必须使用 `java.time` 包（LocalDateTime、LocalDate）
4. 支持 Java17 新特性，代码简洁规范
5. 编译、运行环境全部基于 Java17

## 十一、开发指令
收到指令后，**严格按照以上所有规范**，使用 Java 17 开发苗丰施肥管理后台系统。新增控制器时注意路径约定：管理端根级用于核心施肥业务，`/admin/*` 用于平台管理，`/client/*` 用于C端接口。
