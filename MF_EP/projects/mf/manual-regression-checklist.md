# Manual Regression Checklist

Use this checklist after small user/admin polish changes. Mark P0/P1 first.

## P0: Must Pass

- C端未登录访问 `/cart`, `/checkout`, `/orders`, `/user/profile` should redirect to login.
- C端登录、注册、退出 should complete without blank pages or stuck buttons.
- C端首页 should load banners, recommended products, new products, and recommended articles.
- C端商品列表 and 商品详情 should load platform and merchant products.
- C端购物车、结算、下单 should create an order successfully.
- C端支付页 should show “支付暂未开放” when `payment_enabled=false`.
- C端支付页 should simulate success when `payment_enabled=true`, then refresh order status.
- 管理端登录、退出 and main menu navigation should work.
- 管理端商品新增、编辑、删除、上下架、推荐、新品切换 should refresh list and not break C端列表.
- 管理端订单列表、详情、发货、退款、取消 should respect current order status.

## P1: Core Behavior

- C端顶部导航 should use `nav_product_label` and `nav_encyclopedia_label`.
- C端首页 should hide activity Banner when `activity_banner_enabled=false`.
- C端首页 recommendation counts should follow `home_recommend_product_limit`, `home_new_product_limit`, and `home_recommend_article_limit`.
- C端商品 search, category, type, sort, min price, and max price filters should not reuse wrong cached results.
- C端 images should display for full URLs, `/uploads/...`, and `uploads/...`.
- C端 FAQ should show published FAQ only.
- C端活动页 should show active activities with correct time display.
- C端反馈提交 should show a success or error message.
- C端订单列表 and detail should show Chinese status labels consistently.
- 管理端平台设置 should save known configs and keep arbitrary config editing available.
- 管理端商品管理 should show status, stock, recommend, new, and merchant-related fields clearly.
- 管理端商家审核通过、拒绝、禁用 should update status and refresh list.
- 管理端 FAQ新增 should default to published when that is the intended product behavior.
- 管理端活动 active/ended/banner fields should be clear after save.

## P2: Polish

- Empty lists should show an empty state rather than undefined/null.
- Buttons should show loading or feedback when an async operation runs.
- Image previews should not break layout when the image URL is missing.
- Long config values, product names, and descriptions should use tooltip/overflow behavior.
- Build warnings should be recorded if they are pre-existing and non-blocking.

## Validation Commands

```powershell
cd mf-fertilizer
mvn -pl fertilizer-api -am test -DskipTests

cd ../mf-frontend-client
npm run build

cd ../mf-frontend
npm run build
```

