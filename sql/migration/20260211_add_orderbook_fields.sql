-- ============================================================
-- 盘口字段升级: 买卖五档 + PE-TTM
-- 日期: 2026-02-11
-- ============================================================

ALTER TABLE market_security 
ADD COLUMN bid1_price DECIMAL(18,2), ADD COLUMN bid1_vol INT,
ADD COLUMN bid2_price DECIMAL(18,2), ADD COLUMN bid2_vol INT,
ADD COLUMN bid3_price DECIMAL(18,2), ADD COLUMN bid3_vol INT,
ADD COLUMN bid4_price DECIMAL(18,2), ADD COLUMN bid4_vol INT,
ADD COLUMN bid5_price DECIMAL(18,2), ADD COLUMN bid5_vol INT,
ADD COLUMN ask1_price DECIMAL(18,2), ADD COLUMN ask1_vol INT,
ADD COLUMN ask2_price DECIMAL(18,2), ADD COLUMN ask2_vol INT,
ADD COLUMN ask3_price DECIMAL(18,2), ADD COLUMN ask3_vol INT,
ADD COLUMN ask4_price DECIMAL(18,2), ADD COLUMN ask4_vol INT,
ADD COLUMN ask5_price DECIMAL(18,2), ADD COLUMN ask5_vol INT,
ADD COLUMN pe_ttm DECIMAL(18,2);
