USE fertilizer;

INSERT INTO platform_config (config_key, config_value, config_group, description, value_type, deleted)
VALUES
('site_name', '苗肥商城', 'general', '平台名称，后续可用于页面标题和站点展示', 'string', 0),
('customer_service_phone', '400-888-6666', 'general', '平台客服电话', 'string', 0),
('nav_product_label', '商品商城', 'general', 'C端顶部导航栏商品入口名称', 'string', 0),
('nav_encyclopedia_label', '树木百科', 'general', 'C端顶部导航栏百科入口名称', 'string', 0),
('activity_banner_enabled', 'true', 'activity', '是否在C端首页展示活动Banner', 'boolean', 0),
('home_recommend_product_limit', '8', 'general', 'C端首页推荐商品展示数量，最大20', 'int', 0),
('home_new_product_limit', '8', 'general', 'C端首页新品商品展示数量，最大20', 'int', 0),
('home_recommend_article_limit', '4', 'general', 'C端首页科普推荐文章展示数量，最大20', 'int', 0),
('points_order_rate', '1', 'points', '下单积分比例预留配置，当前暂未接入积分业务', 'int', 0),
('payment_timeout_minutes', '1', 'payment', '待支付订单超时时间（分钟），超时后不允许支付并由定时任务取消', 'int', 0),
('payment_enabled', 'false', 'payment', '支付功能开关，关闭时C端不允许发起支付', 'boolean', 0)
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    config_group = VALUES(config_group),
    description = VALUES(description),
    value_type = VALUES(value_type),
    deleted = 0;
