USE fertilizer;

INSERT INTO activity (title, description, cover_image, type, rule_json, start_time, end_time, status, is_banner, sort_order, deleted)
SELECT '夏季养护肥料专场',
       '精选通用肥、有机肥和缓释肥，适合夏季果树、花卉和庭院绿植日常养护。',
       '',
       'discount',
       '{"discountRate":0.88,"scope":"fertilizer","note":"部分肥料商品限时八八折"}',
       '2026-06-01 00:00:00',
       '2026-08-31 23:59:59',
       'active',
       1,
       100,
       0
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '夏季养护肥料专场' AND deleted = 0);

INSERT INTO activity (title, description, cover_image, type, rule_json, start_time, end_time, status, is_banner, sort_order, deleted)
SELECT '新手种植入门礼包',
       '面向新用户的树苗与基础养护用品组合活动，帮助用户更快完成第一次种植。',
       '',
       'new',
       '{"package":"starter","benefit":"树苗与基础养护用品组合推荐","limit":"每位用户限参与一次"}',
       '2026-06-10 00:00:00',
       '2026-07-31 23:59:59',
       'active',
       1,
       90,
       0
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '新手种植入门礼包' AND deleted = 0);

INSERT INTO activity (title, description, cover_image, type, rule_json, start_time, end_time, status, is_banner, sort_order, deleted)
SELECT '周末限时秒杀',
       '每周末开放少量热门肥料和园艺工具秒杀库存，数量有限，售完即止。',
       '',
       'seckill',
       '{"time":"每周六至周日","stock":"限量库存","note":"以活动页实际库存为准"}',
       '2026-06-18 00:00:00',
       '2026-07-20 23:59:59',
       'active',
       0,
       80,
       0
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '周末限时秒杀' AND deleted = 0);

INSERT INTO activity (title, description, cover_image, type, rule_json, start_time, end_time, status, is_banner, sort_order, deleted)
SELECT '庭院果树养护季',
       '围绕果树补肥、修剪和病虫害预防的专题活动，推荐适合庭院用户的养护商品。',
       '',
       'discount',
       '{"topic":"fruit_tree_care","benefit":"专题商品优惠","season":"summer"}',
       '2026-06-15 00:00:00',
       '2026-09-15 23:59:59',
       'active',
       0,
       70,
       0
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '庭院果树养护季' AND deleted = 0);

INSERT INTO activity (title, description, cover_image, type, rule_json, start_time, end_time, status, is_banner, sort_order, deleted)
SELECT '老客户复购福利',
       '针对近期有购买记录的用户提供复购提醒和养护用品优惠，适合定期补肥补货。',
       '',
       'discount',
       '{"target":"returning_users","benefit":"复购优惠","note":"具体权益以后续业务规则为准"}',
       '2026-06-18 00:00:00',
       '2026-08-18 23:59:59',
       'active',
       0,
       60,
       0
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '老客户复购福利' AND deleted = 0);
