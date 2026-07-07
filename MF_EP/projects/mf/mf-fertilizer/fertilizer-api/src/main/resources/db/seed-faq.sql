USE fertilizer;

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '如何下单购买商品？',
       '进入商品详情页后选择购买数量，点击“立即购买”或先加入购物车，再在结算页选择收货地址并提交订单。',
       'order', 10, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '如何下单购买商品？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '订单提交后可以取消吗？',
       '待付款订单通常可以在订单详情中取消；如果订单已发货，建议先联系客服或等待收货后按售后流程处理。',
       'order', 20, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '订单提交后可以取消吗？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '在哪里查看我的订单？',
       '登录后进入“我的订单”页面，可以查看全部订单以及待付款、待发货、已发货、已完成等不同状态的订单。',
       'order', 30, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '在哪里查看我的订单？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '如何确认收货？',
       '当订单状态为“已发货”时，进入订单详情页，确认商品无误后点击“确认收货”。',
       'order', 40, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '如何确认收货？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '如何选择适合的树苗？',
       '可以根据种植环境、光照、水分条件和养护经验选择树苗。新手建议优先选择适应性强、养护难度低的品种。',
       'product', 10, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '如何选择适合的树苗？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '肥料商品应该怎么选？',
       '选择肥料时可关注肥料类型、养分含量、适用作物和使用周期。若不确定，可先查看商品说明或使用智能客服咨询。',
       'product', 20, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '肥料商品应该怎么选？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '商品显示缺货还能购买吗？',
       '缺货商品暂时不能下单。可以稍后再查看库存，或选择同类型的其他商品。',
       'product', 30, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '商品显示缺货还能购买吗？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '如何添加或修改收货地址？',
       '登录后进入个人中心或结算页的地址区域，可以新增、选择或设置默认收货地址。',
       'account', 10, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '如何添加或修改收货地址？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '忘记密码怎么办？',
       '可以在登录页或账户安全页面通过手机号验证码重置密码。请确保手机号可以正常接收验证码。',
       'account', 20, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '忘记密码怎么办？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '积分有什么用？',
       '积分可用于记录用户成长和平台互动情况。后续如开启兑换、等级权益等功能，将以平台规则为准。',
       'account', 30, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '积分有什么用？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '新买的树苗到货后应该怎么处理？',
       '收到树苗后先检查根系和枝叶状态，适当缓苗，避免立即暴晒或大量施肥。根据品种说明逐步恢复正常养护。',
       'care', 10, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '新买的树苗到货后应该怎么处理？' AND deleted = 0);

INSERT INTO faq (question, answer, category, sort_order, is_published, view_count, deleted)
SELECT '施肥时需要注意什么？',
       '施肥应遵循薄肥勤施原则，避免高浓度肥料直接接触根系。不同生长期和不同品种的施肥频率应适当调整。',
       'care', 20, 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = '施肥时需要注意什么？' AND deleted = 0);
