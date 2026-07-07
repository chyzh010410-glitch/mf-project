SET NAMES utf8mb4;

-- Seed products for merchant account `cyz` in the local development database.
-- Merchant id is looked up by username so the script stays usable if ids differ.

SET @merchant_id := (SELECT id FROM merchant WHERE username = 'cyz' LIMIT 1);

UPDATE product SET
    name = 'cyz精品生物有机肥 40kg',
    product_type = 'fertilizer',
    merchant_id = @merchant_id,
    category_id = 2,
    brand = 'cyz农资',
    price = 72.00,
    original_price = 82.00,
    stock = 260,
    unit = 'bag',
    status = 1,
    is_recommend = 1,
    is_new = 1,
    sort_order = 101,
    description = '有机质含量高，适合苗木、果树、蔬菜基肥使用，改善土壤团粒结构。',
    min_purchase = 1,
    freight = 12.00
WHERE id = 31;

UPDATE product SET
    name = 'cyz果树专用复合肥 25kg',
    product_type = 'fertilizer',
    merchant_id = @merchant_id,
    category_id = 3,
    brand = 'cyz农资',
    price = 96.00,
    original_price = 108.00,
    stock = 180,
    unit = 'bag',
    status = 1,
    is_recommend = 1,
    is_new = 0,
    sort_order = 102,
    description = '氮磷钾均衡配比，适合苹果、桃、柑橘等果树追肥。',
    min_purchase = 1,
    freight = 10.00
WHERE id = 32;

UPDATE product SET
    name = 'cyz高钾水溶肥 10kg',
    product_type = 'fertilizer',
    merchant_id = @merchant_id,
    category_id = 5,
    brand = 'cyz农资',
    price = 168.00,
    original_price = 198.00,
    stock = 120,
    unit = 'box',
    status = 1,
    is_recommend = 0,
    is_new = 1,
    sort_order = 103,
    description = '全水溶配方，适合滴灌和叶面喷施，促进果实膨大和上色。',
    min_purchase = 1,
    freight = 8.00
WHERE id = 33;

UPDATE product SET
    name = 'cyz缓释尿素 50kg',
    product_type = 'fertilizer',
    merchant_id = @merchant_id,
    category_id = 4,
    brand = 'cyz农资',
    price = 126.00,
    original_price = 139.00,
    stock = 220,
    unit = 'bag',
    status = 1,
    is_recommend = 0,
    is_new = 0,
    sort_order = 104,
    description = '缓释型氮肥，肥效稳定，适合苗木生长期追肥。',
    min_purchase = 1,
    freight = 18.00
WHERE id = 34;

UPDATE product SET
    name = 'cyz红富士苹果苗 2年生',
    product_type = 'tree',
    merchant_id = @merchant_id,
    category_id = 8,
    brand = 'cyz苗圃',
    price = 18.00,
    original_price = 22.00,
    stock = 1500,
    unit = 'piece',
    status = 1,
    is_recommend = 1,
    is_new = 1,
    sort_order = 201,
    description = '嫁接红富士苹果苗，根系完整，适合果园规模种植。',
    min_purchase = 10,
    freight = 20.00
WHERE id = 35;

UPDATE product SET
    name = 'cyz桂花苗 3年生',
    product_type = 'tree',
    merchant_id = @merchant_id,
    category_id = 9,
    brand = 'cyz苗圃',
    price = 38.00,
    original_price = 48.00,
    stock = 600,
    unit = 'piece',
    status = 1,
    is_recommend = 0,
    is_new = 0,
    sort_order = 202,
    description = '三年生桂花苗，冠形匀称，适合庭院和绿化工程。',
    min_purchase = 5,
    freight = 28.00
WHERE id = 36;

UPDATE product SET
    name = 'cyz砂糖橘苗 1年生',
    product_type = 'tree',
    merchant_id = @merchant_id,
    category_id = 8,
    brand = 'cyz苗圃',
    price = 9.90,
    original_price = 12.00,
    stock = 2400,
    unit = 'piece',
    status = 1,
    is_recommend = 1,
    is_new = 0,
    sort_order = 203,
    description = '一年生砂糖橘容器苗，成活率高，适合南方果园。',
    min_purchase = 20,
    freight = 18.00
WHERE id = 37;

UPDATE product SET
    name = 'cyz银杏苗 2年生',
    product_type = 'tree',
    merchant_id = @merchant_id,
    category_id = 9,
    brand = 'cyz苗圃',
    price = 21.00,
    original_price = 26.00,
    stock = 900,
    unit = 'piece',
    status = 1,
    is_recommend = 0,
    is_new = 1,
    sort_order = 204,
    description = '两年生银杏苗，树干通直，适合道路绿化和庭院栽植。',
    min_purchase = 10,
    freight = 22.00
WHERE id = 38;

DELETE FROM product_detail WHERE product_id BETWEEN 31 AND 38;

INSERT INTO product_detail (product_id, detail_type, attrs_json)
VALUES
(31, 'fertilizer', JSON_OBJECT('养分含量','有机质≥45%','适用作物','苗木、果树、蔬菜','施用方法','沟施、穴施')),
(32, 'fertilizer', JSON_OBJECT('养分含量','N-P-K 15-15-15','适用作物','果树','施用方法','基肥、追肥')),
(33, 'fertilizer', JSON_OBJECT('养分含量','高钾配方+微量元素','适用作物','果树、经济作物','施用方法','滴灌、喷施')),
(34, 'fertilizer', JSON_OBJECT('养分含量','N≥46%','适用作物','苗木、大田作物','施用方法','追肥')),
(35, 'tree', JSON_OBJECT('品种','红富士','树龄','2年','株高','1.2-1.5m','成活率','≥95%')),
(36, 'tree', JSON_OBJECT('品种','桂花','树龄','3年','株高','80-100cm','用途','庭院、绿化')),
(37, 'tree', JSON_OBJECT('品种','砂糖橘','树龄','1年','株高','40-60cm','用途','果园栽培')),
(38, 'tree', JSON_OBJECT('品种','银杏','树龄','2年','株高','1.5-1.8m','用途','行道树、庭院'));
