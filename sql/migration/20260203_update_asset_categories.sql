-- 资产分类字典更新
-- 更新分类名称和描述，使其更符合国人习惯

-- 1: 现金类 -> 现金/存款
UPDATE asset_category SET name = '现金/存款', description = '银行卡、支付宝、微信余额、现金' WHERE id = 1;

-- 2: 固收 -> 投资理财 (涵盖股票、基金、债券、黄金)
UPDATE asset_category SET name = '投资理财', description = '股票、基金、债券、黄金、理财产品' WHERE id = 2;

-- 3: 权益 -> 固定资产 (房、车)
UPDATE asset_category SET name = '固定资产', description = '房产、车辆、车位' WHERE id = 3;

-- 4: 保障 -> 商业保险
UPDATE asset_category SET name = '商业保险', description = '重疾险、寿险、年金险' WHERE id = 4;

-- 5: 其他
UPDATE asset_category SET name = '其他资产', description = '借出款项、收藏品等' WHERE id = 5;

-- 如果 asset_category 表不存在，需要先创建
-- CREATE TABLE IF NOT EXISTS asset_category (
--     id INT PRIMARY KEY AUTO_INCREMENT,
--     name VARCHAR(50) NOT NULL,
--     description VARCHAR(200)
-- );

-- 如果需要初始化数据（表为空时）
-- INSERT INTO asset_category (id, name, description) VALUES
-- (1, '现金/存款', '银行卡、支付宝、微信余额、现金'),
-- (2, '投资理财', '股票、基金、债券、黄金、理财产品'),
-- (3, '固定资产', '房产、车辆、车位'),
-- (4, '商业保险', '重疾险、寿险、年金险'),
-- (5, '其他资产', '借出款项、收藏品等');
