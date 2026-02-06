-- 创建市场证券表
CREATE TABLE IF NOT EXISTS `market_security` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '证券名称',
  `code` varchar(20) NOT NULL COMMENT '证券代码',
  `type` varchar(20) NOT NULL COMMENT '类型 (stock, fund, bond)',
  `current_price` decimal(10,2) DEFAULT NULL COMMENT '当前价格',
  `change_percent` decimal(10,2) DEFAULT NULL COMMENT '涨跌幅 (%)',
  `risk_level` varchar(10) DEFAULT NULL COMMENT '风险等级 (R1-R5)',
  `sector` varchar(50) DEFAULT NULL COMMENT '所属板块',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `market_cap` varchar(50) DEFAULT NULL COMMENT '市值',
  `pe_ratio` decimal(10,2) DEFAULT NULL COMMENT '市盈率',
  `volume` varchar(50) DEFAULT NULL COMMENT '成交量',
  `high52w` decimal(10,2) DEFAULT NULL COMMENT '52周最高',
  `low52w` decimal(10,2) DEFAULT NULL COMMENT '52周最低',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市场证券表';

-- 插入一些测试数据
INSERT INTO `market_security` (`name`, `code`, `type`, `current_price`, `change_percent`, `risk_level`, `sector`, `description`, `market_cap`, `pe_ratio`, `volume`, `high52w`, `low52w`) VALUES
('贵州茅台', '600519', 'stock', 1700.00, 2.50, 'R3', '消费', '高端白酒龙头', '2.1万亿', 30.00, '50亿', 1900.00, 1500.00),
('宁德时代', '300750', 'stock', 180.00, 1.20, 'R4', '新能源', '动力电池全球第一', '8000亿', 25.00, '30亿', 250.00, 150.00),
('沪深300ETF', '510300', 'fund', 4.00, 0.80, 'R3', '大盘指数', '跟踪沪深300指数', '1000亿', NULL, '20亿', 4.50, 3.50),
('招商银行', '600036', 'stock', 32.00, 1.50, 'R3', '金融', '零售银行之王', '8000亿', 6.00, '15亿', 35.00, 28.00),
('腾讯控股', '00700', 'stock', 300.00, -1.00, 'R4', '互联网', '社交与游戏巨头', '3万亿', 18.00, '40亿', 400.00, 250.00),
('纳指ETF', '513100', 'fund', 1.50, 3.00, 'R4', '美股指数', '跟踪纳斯达克100', '500亿', NULL, '10亿', 1.60, 1.00),
('国债ETF', '511010', 'bond', 100.00, 0.05, 'R1', '债券', '极低风险国债', '200亿', NULL, '5亿', 101.00, 99.00);
