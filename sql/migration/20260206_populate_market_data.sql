-- 插入白酒板块数据
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description, market_cap, pe_ratio, volume, high52w, low52w) VALUES
('贵州茅台', '600519', 'stock', 1688.88, 2.35, 'R3', '白酒', '中国高端白酒龙头企业', '2.12万亿', 28.5, '2.5万手', 1850.00, 1550.00),
('五粮液', '000858', 'stock', 156.80, 1.56, 'R3', '白酒', '中国知名白酒企业', '6085亿', 18.2, '18.5万手', 175.50, 128.00),
('山西汾酒', '600809', 'stock', 265.42, 3.12, 'R3', '白酒', '清香型白酒龙头', '3250亿', 30.5, '5.6万手', 310.00, 210.00);

-- 插入科技/互联网
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description, market_cap, pe_ratio, volume, high52w, low52w) VALUES
('腾讯控股', '00700', 'stock', 388.20, -1.28, 'R4', '互联网', '全球领先的互联网科技公司', '3.6万亿', 25.8, '1500万股', 420.00, 280.00),
('苹果', 'AAPL', 'stock', 189.25, 1.12, 'R4', '科技', '全球市值最大的科技公司', '2.95万亿美元', 29.5, '4500万股', 199.62, 150.00),
('宁德时代', '300750', 'stock', 185.60, 3.25, 'R4', '新能源', '全球动力电池龙头', '8500亿', 22.5, '1200万手', 250.00, 140.00);

-- 插入基金 (覆盖各个风险等级)
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description, market_cap, pe_ratio, volume, high52w, low52w) VALUES
('国债2024', '019701', 'bond', 100.25, 0.02, 'R1', '国债', '安全性极高的国债', NULL, NULL, '1000手', 100.50, 100.00),
('红利低波ETF', '512890', 'fund', 1.285, 0.28, 'R2', '策略基金', '主要投资高股息股票', '50亿', 8.5, '500万手', 1.35, 1.10),
('沪深300ETF', '510300', 'fund', 3.856, 0.65, 'R3', '指数基金', '中国核心资产集合', '1200亿', 12.5, '850万手', 4.20, 3.50),
('半导体ETF', '512480', 'fund', 1.125, 4.15, 'R5', '行业基金', '高弹性科技板块', '200亿', 45.0, '600万手', 1.50, 0.90);
