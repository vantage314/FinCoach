-- 扩展市场证券表字段 (2026-02-06)
ALTER TABLE market_security 
ADD COLUMN market_cap VARCHAR(20) COMMENT '市值 (e.g. 2.5万亿)',
ADD COLUMN pe_ratio DECIMAL(10,2) COMMENT '市盈率',
ADD COLUMN volume VARCHAR(20) COMMENT '成交量',
ADD COLUMN high52w DECIMAL(10,2) COMMENT '52周最高',
ADD COLUMN low52w DECIMAL(10,2) COMMENT '52周最低';

-- 这里的 sector 字段之前定义 Security 时已有，但如果数据库没加可以补上
-- ADD COLUMN sector VARCHAR(50) COMMENT '所属板块 (e.g. 白酒)';
