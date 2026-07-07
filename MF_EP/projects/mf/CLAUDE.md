# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Workspace Overview

苗丰施肥管控平台 (MiaoFeng Fertilizer Management Platform) — a full-stack application with a management backend and a consumer-facing e-commerce platform for trees and fertilizers.

- `mf-fertilizer/` — Java 17 Spring Boot 3.2.5 backend
- `mf-frontend/` — Vue 3 admin SPA (partial: core CRUD pages done, admin sub-pages mostly placeholders)

## Backend: mf-fertilizer

### Build & Run

```bash
# Build all modules
cd mf-fertilizer && mvn clean package -DskipTests

# Run (the api module produces the executable jar)
java -jar fertilizer-api/target/fertilizer-api-1.0.0.jar

# The app starts on port 8080 with dev profile active
# Swagger UI: http://localhost:8080/doc.html
```

### Module Architecture

Three-tier Maven multi-module: `fertilizer-api` -> `fertilizer-core` -> `fertilizer-common`

| Module | Purpose |
|--------|---------|
| `fertilizer-common` | Shared models: 28 entities (extend `BaseEntity` with snowflake ID), 20 DTOs (in `dto/`, `dto/admin/`, `dto/client/`), 10 VOs (in `vo/`, `vo/client/`), constants, exceptions, utils (`JwtUtil`, `SeasonUtil`) |
| `fertilizer-core` | Business logic: 31 MyBatis-Plus mappers (extend `BaseMapper`, no XML), 31 service interfaces (extend `IService`), 31 service implementations |
| `fertilizer-api` | Web layer: `FertilizerApplication` entry point, 7 config classes, 18 REST controllers (5 root-level admin, 1 `/admin/*`, 11 `/client/*`, 1 experimental) |

### Key Conventions

- **Response format**: All endpoints return `ResultVO<T>` with `code`, `msg`, `data`
- **Pagination**: List endpoints accept `PageDTO` (page/size), return `PageVO<T>` (total/page/size/records)
- **Validation**: DTOs use Jakarta Validation annotations (`@NotBlank`, `@NotNull`), controllers use `@Valid`
- **DI**: `@RequiredArgsConstructor` on all classes, no `@Autowired`
- **Entities**: All extend `BaseEntity` -- snowflake ID primary key, `createTime`/`updateTime` auto-filled, `deleted` logical delete
- **Cache**: Redis for JWT tokens (`login:token:<token>` for admin, `client:token:<token>` for consumer), tree species list, fertilizer list, recommendation results (24h TTL)
- **Auth**: JWT (HMAC-SHA, 7-day expiry) + Redis token store. Interceptor checks Redis key existence. Admin paths use `login:token:` prefix, client paths use `client:token:` prefix. Excludes `/login`, `/client/auth`, Swagger paths, `/error`
- **Password hashing**: Admin `SysUser` uses BCrypt (Hutool); consumer `User` uses MD5 (Spring DigestUtils). **Note: inconsistency -- MD5 should be upgraded to BCrypt.**
- **DB**: 32 tables. No foreign keys; indexing on query fields; `utf8mb4`/InnoDB

### Tech Stack (fixed, do not substitute)

Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.9, MySQL 8.0+, Redis, JWT (jjwt 0.12.6), Knife4j 4.3.0, Hutool 5.8.28, Lombok

### Database Tables (32 total)

**Core fertilization (5 tables):** `sys_user`, `tree`, `fertilizer`, `fertilization_record`, `fertilization_rule`

**Consumer user (4 tables):** `user`, `user_address`, `verification_code`, `admin_role`

**E-commerce (7 tables):** `product_category`, `product`, `product_detail`, `shopping_cart_item`, `order`, `order_item`, `payment`

**Content & community (7 tables):** `encyclopedia_entry`, `encyclopedia_article`, `user_upload`, `community_comment`, `community_like`, `favorite`, `browsing_history`

**Platform management (7 tables):** `platform_config`, `system_log`, `faq`, `feedback`, `activity`, `message`, `file_upload`

**Loyalty (2 tables):** `membership_level`, `points_record`

Schema init: `fertilizer-api/src/main/resources/db/schema.sql` + `init-products.sql`

### API Endpoints Summary

**Admin -- root-level (5 controllers):**

| Prefix | Controller | Key Operations |
|--------|-----------|----------------|
| `/login`, `/logout` | `LoginController` | Login returns JWT token; logout invalidates |
| `/tree` | `TreeController` | CRUD + paginated search + `/tree/species` cached list |
| `/fertilizer` | `FertilizerController` | CRUD + paginated search + `/fertilizer/list` cached list |
| `/record` | `FertilizationRecordController` | CRUD + paginated search + `/record/stats` aggregation |
| `/rule` | `FertilizationRuleController` | CRUD + `/rule/recommend` -- matches rules by species, age, season |

**Admin -- `/admin/*` (1 controller):**

| Prefix | Controller | Key Operations |
|--------|-----------|----------------|
| `/admin/orders` | `AdminOrderController` | Order list, detail, ship, update status, statistics |

**Consumer -- `/client/*` (11 controllers):**

| Prefix | Controller | Key Operations |
|--------|-----------|----------------|
| `/client/auth` | `ClientAuthController` | Register, login, logout, verification code, password reset |
| `/client` | `ClientUserController` | Profile CRUD, password change, address CRUD (multi-address, set default) |
| `/client/products` | `ClientProductController` | Product list (filter/sort/paginate), detail, category list |
| `/client/cart` | `ClientCartController` | Cart CRUD, clear cart |
| `/client/orders` | `ClientOrderController` | Place order, order list, detail, cancel, confirm receipt |
| `/client/home` | `ClientHomeController` | Aggregated homepage (banners + recommended products + new arrivals + articles) |
| `/client/encyclopedia` | `ClientEncyclopediaController` | Encyclopedia entry list, detail |
| `/client/articles` | `ClientArticleController` | Article list, detail |
| `/client/faq` | `ClientFaqController` | FAQ list |
| `/client/feedback` | `ClientFeedbackController` | Submit feedback |
| `/client/favorites` | `ClientFavoriteController` | Favorite CRUD |
| `/client/history` | `ClientHistoryController` | Browsing history list, clear |
| `/client/messages` | `ClientMessageController` | Message list, unread count, mark read |
| `/client/points` | `ClientPointsController` | Points balance, points history |

### Database

- Host: `localhost:3306`, database: `fertilizer`, credentials: `root/123456`
- Redis: `localhost:6379`, password: `123456`
- Default admin: `admin/admin123`

### Project Skill File

Full backend specification at `mf-fertilizer/.claude/skills/mf-fertilizer.md` -- read it when making significant backend changes.

Frontend feature checklist at `mf-frontend/.claude/skills/苗丰施肥管控平台 - 用户客户端+系统管理员功能清单.md`.

## Frontend: mf-frontend

### Tech Stack (fixed, do not substitute)

Vue 3 (Composition API) + Vite + JavaScript + Element Plus + Axios + Vue Router + Pinia

### Commands

```bash
cd mf-frontend && npm install && npm run dev
# Dev server at http://localhost:5173
```

### Directory Structure

```
mf-frontend/
├── public/
├── src/
│   ├── api/          # Axios API modules (auth, tree, fertilizer, record, rule)
│   ├── assets/       # Static assets
│   ├── components/   # Shared components
│   ├── router/       # Vue Router config
│   ├── store/        # Pinia stores (auth)
│   ├── styles/       # Global styles + CSS variables (green theme)
│   ├── utils/        # Axios wrapper (request.js), request interceptors
│   └── views/        # Page components
│       ├── layout/   # Shell layout (sidebar + topbar)
│       ├── login/    # Login page
│       ├── fertilizer/  # Fertilizer CRUD
│       ├── tree/        # Tree CRUD
│       ├── record/      # Fertilization records
│       ├── rule/        # Fertilization rules
│       └── admin/       # Admin sub-pages (15 pages)
├── index.html
├── vite.config.js
└── package.json
```

### Key Conventions

- **Theme**: White + green (`#2d8c4a` primary, `#f0f9f4` background, `#1b3a2a` sidebar)
- **Layout**: Collapsible sidebar (220px -> 64px) + top nav + main content (classic admin shell)
- **HTTP**: `src/utils/request.js` wraps Axios -- baseURL `http://localhost:8080`, auto-attaches `Authorization: Bearer <token>`, handles 401 redirect to `/login`
- **Auth flow**: Login -> store token in Pinia + localStorage -> interceptor reads from store -> logout clears both
- **API modules**: One file per backend controller in `src/api/` (auth.js, tree.js, fertilizer.js, record.js, rule.js)
- **Pages**: All pages go through the Layout shell. `/login` is standalone.
- **Routing**: `router.beforeEach` checks token for protected routes, redirects to `/login` if absent

### Pages (19 total)

**Complete (5 pages):**

| Route | Component | Description |
|-------|-----------|-------------|
| `/login` | `views/login/index.vue` | Login with water-drop -> seedling CSS animation |
| `/fertilizer` | `views/fertilizer/index.vue` | Fertilizer CRUD (search + table + dialog + validation) |
| `/tree` | `views/tree/index.vue` | Tree CRUD (search + table + dialog + species filter) |
| `/record` | `views/record/index.vue` | Fertilization records (date filter + add + delete + stats) |
| `/rule` | `views/rule/index.vue` | Fertilization rules (CRUD + recommend dialog) |

**Complete -- admin (1 page):**

| Route | Component | Description |
|-------|-----------|-------------|
| `/admin/orders` | `views/admin/Orders.vue` | Order management (stats cards + status tabs + ship dialog + detail view) |

**Placeholder -- admin (14 pages):** All render a toolbar + empty table + placeholder text. Need full CRUD implementation.

| Route | Component | Description |
|-------|-----------|-------------|
| `/admin/products` | `views/admin/Products.vue` | Product list |
| `/admin/categories` | `views/admin/Categories.vue` | Product categories |
| `/admin/uploads` | `views/admin/Uploads.vue` | User upload review |
| `/admin/encyclopedia` | `views/admin/Encyclopedia.vue` | Encyclopedia management |
| `/admin/articles` | `views/admin/Articles.vue` | Article management |
| `/admin/comments` | `views/admin/Comments.vue` | Comment management |
| `/admin/users` | `views/admin/Users.vue` | User list |
| `/admin/feedbacks` | `views/admin/Feedbacks.vue` | Feedback handling |
| `/admin/admins` | `views/admin/Admins.vue` | Admin account management |
| `/admin/config` | `views/admin/Config.vue` | Platform settings |
| `/admin/faqs` | `views/admin/Faqs.vue` | FAQ management |
| `/admin/activities` | `views/admin/Activities.vue` | Activity management |
| `/admin/messages` | `views/admin/Messages.vue` | Message push |
| `/admin/logs` | `views/admin/Logs.vue` | System logs |

### Layout Shell

`views/layout/index.vue` -- collapsible dark-green sidebar with 4 main nav items + 5 admin groups (15 sub-items total), white top bar with breadcrumb/username/logout, active menu highlighting based on route path.

### Router

`src/router/index.js` defines 2 top-level routes (`/login` standalone, `/` layout with 19 children). Navigation guard checks token existence.
