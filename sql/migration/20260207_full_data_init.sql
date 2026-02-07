/*
 Navicat/DBeaver/MySQL Workbench 通用脚本
 作用：重置所有数据，生成 100+ 条仿真行情，包含 F10、公告、财报
 修复：弃用存储过程，改用 CTE 递归生成，避免 DELIMITER 报错
*/

-- 1. 环境重置
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清空表
TRUNCATE TABLE market_security;
TRUNCATE TABLE company_profile;
TRUNCATE TABLE financial_news;
DROP TABLE IF EXISTS financial_report;
DROP TABLE IF EXISTS company_notice;

-- 2. 重建补充表结构
CREATE TABLE IF NOT EXISTS financial_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    report_name VARCHAR(50) COMMENT '报告期',
    revenue VARCHAR(50) COMMENT '营收',
    revenue_growth VARCHAR(20) COMMENT '营收同比',
    net_profit VARCHAR(50) COMMENT '净利润',
    profit_growth VARCHAR(20) COMMENT '利润同比',
    eps VARCHAR(20) COMMENT '每股收益',
    roe VARCHAR(20) COMMENT 'ROE',
    INDEX idx_code (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS company_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(20) COMMENT '类型',
    publish_date DATE,
    link VARCHAR(255) COMMENT '链接',
    INDEX idx_code (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 插入 10 条 "真实头部数据" (精细化)
INSERT INTO market_security (id, name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
(1, '宁德时代', '300750', 'STOCK', 180.50, 3.20, 'R5', '新能源', '动力电池全球第一'),
(2, '贵州茅台', '600519', 'STOCK', 1750.00, -0.80, 'R4', '消费', '高端白酒龙头'),
(3, '腾讯控股', '00700', 'STOCK', 350.00, 1.50, 'R4', '互联网', '社交与游戏巨头'),
(4, '纳指100ETF', '513100', 'FUND', 1.45, 2.30, 'R5', '科技', '跟踪美股科技股走势'),
(5, '沪深300ETF', '510300', 'FUND', 3.85, 1.20, 'R3', '核心资产', '一键买入中国核心资产'),
(6, '国债2401', '019701', 'BOND', 102.50, 0.05, 'R1', '国债', '2024年第一期记账式国债'),
(7, '中芯国际', '688981', 'STOCK', 48.50, 0.50, 'R5', '半导体', '中国芯片制造核心'),
(8, '迈瑞医疗', '300760', 'STOCK', 290.00, -1.20, 'R3', '医疗', '医疗器械龙头'),
(9, '招商银行', '600036', 'STOCK', 32.50, 0.80, 'R3', '金融', '零售银行之王'),
(10, '黄金ETF', '518880', 'FUND', 4.50, 0.90, 'R3', '贵金属', '紧跟黄金现货价格');

-- 4. 批量生成 90 条仿真数据 (使用 CTE 递归，无需存储过程)
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector)
WITH RECURSIVE seq AS (
    SELECT 11 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100
)
SELECT 
    CASE 
        WHEN n <= 40 THEN CONCAT('仿真科技', n)
        WHEN n <= 70 THEN CONCAT('成长基金', n)
        ELSE CONCAT('国债26', n)
    END as name,
    CONCAT('600', LPAD(n, 3, '0')) as code,
    CASE 
        WHEN n <= 40 THEN 'STOCK'
        WHEN n <= 70 THEN 'FUND'
        ELSE 'BOND'
    END as type,
    CASE 
        WHEN n <= 40 THEN ROUND(10 + (RAND() * 100), 2)
        WHEN n <= 70 THEN ROUND(1 + (RAND() * 5), 3)
        ELSE ROUND(100 + (RAND() * 5), 2)
    END as current_price,
    ROUND((RAND() * 10) - 5, 2) as change_percent, -- -5% 到 +5%
    ELT(FLOOR(1 + (RAND() * 5)), 'R1', 'R2', 'R3', 'R4', 'R5') as risk_level,
    ELT(FLOOR(1 + (RAND() * 5)), '科技', '消费', '金融', '工业', '医药') as sector
FROM seq;

-- 5. 插入 F10 真实资料
INSERT INTO company_profile (stock_code, company_name, establishment_date, listing_date, chairman, business_scope, website) VALUES
('300750', '宁德时代新能源科技股份有限公司', '2011-12-16', '2018-06-11', '曾毓群', '专注于新能源汽车动力电池系统、储能系统的研发、生产和销售。', 'www.catl.com'),
('600519', '贵州茅台酒股份有限公司', '1999-11-20', '2001-08-27', '丁雄军', '茅台酒及系列酒的生产与销售；饮料、食品、包装材料的生产、销售。', 'www.moutaichina.com'),
('00700', '腾讯控股有限公司', '1998-11-11', '2004-06-16', '马化腾', '社交平台、数字内容、广告、金融科技及企业服务。', 'www.tencent.com'),
('688981', '中芯国际集成电路制造有限公司', '2000-04-03', '2020-07-16', '刘训峰', '提供0.35微米到14纳米不同技术节点的晶圆代工与技术服务。', 'www.smics.com');

-- 6. 批量生成剩余 F10 资料 (补全 100 条)
INSERT IGNORE INTO company_profile (stock_code, company_name, establishment_date, business_scope)
SELECT 
    code, 
    CONCAT(name, '股份有限公司'), 
    '2015-01-01',
    CONCAT('本公司主要从事', sector, '领域的技术研发与服务，是行业内的领先企业。此处为系统自动生成的仿真F10数据，用于界面展示测试。')
FROM market_security 
WHERE id > 10;

-- 7. 插入 财务报表 (真实)
INSERT INTO financial_report (stock_code, report_name, revenue, revenue_growth, net_profit, profit_growth, eps, roe) VALUES
('300750', '2025年报', '4009亿', '+22.0%', '441亿', '+43.5%', '10.08', '24.5%'),
('300750', '2024年报', '3285亿', '+18.5%', '307亿', '+20.1%', '7.50', '20.1%'),
('600519', '2025年报', '1505亿', '+17.0%', '747亿', '+19.0%', '59.49', '30.2%');

-- 8. 批量生成 财务报表 (仿真)
INSERT INTO financial_report (stock_code, report_name, revenue, revenue_growth, net_profit, eps)
SELECT 
    code, 
    '2025年报', 
    CONCAT(FLOOR(10 + RAND()*500), '亿'), 
    CONCAT('+', FLOOR(RAND()*50), '%'), 
    CONCAT(FLOOR(1 + RAND()*50), '亿'), 
    ROUND(RAND() * 2, 2)
FROM market_security 
WHERE id > 10 AND type = 'STOCK';

-- 9. 插入 公告 (真实)
INSERT INTO company_notice (stock_code, title, type, publish_date) VALUES
('300750', '关于回购公司股份的进展公告', '公告', DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
('300750', '2025年年度报告摘要', '研报', DATE_SUB(CURDATE(), INTERVAL 2 DAY)),
('600519', '关于实施2025年度权益分派的公告', '分红', DATE_SUB(CURDATE(), INTERVAL 5 DAY));

-- 10. 批量生成 公告 (仿真)
INSERT INTO company_notice (stock_code, title, type, publish_date)
SELECT 
    code, 
    CONCAT('关于', name, '召开2026年第一次临时股东大会的通知'), 
    '公告', 
    DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*10) DAY)
FROM market_security 
WHERE id > 10 AND type = 'STOCK';

SET FOREIGN_KEY_CHECKS = 1;
