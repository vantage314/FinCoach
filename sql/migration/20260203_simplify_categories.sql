-- 资产分类简化迁移脚本
-- 将 5 类简化为 3 大类，并增加子类型字段

-- 1. 清空并重置为 3 大类
TRUNCATE TABLE asset_category;
INSERT INTO asset_category (id, name, form_type, icon_slug) VALUES 
(1, '现金', 'SIMPLE', 'wallet'),
(2, '金融投资', 'INVEST', 'chart-line'),
(3, '固定资产', 'PROPERTY', 'house');

-- 2. 资产明细表增加"子分类"字段
ALTER TABLE asset_item ADD COLUMN sub_type VARCHAR(50) DEFAULT NULL COMMENT '子类型: 股票/基金/债券 (仅投资类有效)';

-- 如果 asset_category 表不存在，先创建
-- CREATE TABLE IF NOT EXISTS asset_category (
--     id INT PRIMARY KEY AUTO_INCREMENT,
--     name VARCHAR(50) NOT NULL,
--     form_type VARCHAR(20) NOT NULL COMMENT 'SIMPLE/INVEST/PROPERTY',
--     icon_slug VARCHAR(50)
-- );
