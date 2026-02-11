-- ============================================================
-- Step 1: market_security 表结构升级 - 增加详细行情字段
-- 日期: 2026-02-11
-- ============================================================

-- 1. 新增今开价格
ALTER TABLE market_security ADD COLUMN open_price DECIMAL(15,4) DEFAULT NULL COMMENT '今开价格';

-- 2. 新增最高价格
ALTER TABLE market_security ADD COLUMN high_price DECIMAL(15,4) DEFAULT NULL COMMENT '最高价格';

-- 3. 新增最低价格
ALTER TABLE market_security ADD COLUMN low_price DECIMAL(15,4) DEFAULT NULL COMMENT '最低价格';

-- 4. 新增成交额
ALTER TABLE market_security ADD COLUMN turnover DECIMAL(20,2) DEFAULT NULL COMMENT '成交额(元)';

-- 5. 修改 volume 字段为 BIGINT
ALTER TABLE market_security MODIFY COLUMN volume BIGINT DEFAULT NULL COMMENT '成交量(股)';

-- 6. 修改 market_cap 字段为 DECIMAL
ALTER TABLE market_security MODIFY COLUMN market_cap DECIMAL(20,2) DEFAULT NULL COMMENT '总市值(元)';
