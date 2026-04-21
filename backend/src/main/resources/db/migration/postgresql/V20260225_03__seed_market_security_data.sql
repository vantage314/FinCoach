-- ============================================================
-- 种子数据：80+ 条 A 股龙头股 + 基金 + 债券
-- 价格初始为 0，由 Python 脚本自动更新真实行情
-- ============================================================

-- 确保 code 列有唯一索引（ON CONFLICT 需要）
CREATE UNIQUE INDEX IF NOT EXISTS uk_market_security_code ON market_security(code);

-- ===== 白酒板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('贵州茅台', '600519', 'STOCK', 0, 0, 'R3', '白酒', '中国高端白酒龙头企业'),
('五粮液', '000858', 'STOCK', 0, 0, 'R3', '白酒', '浓香型白酒龙头企业'),
('泸州老窖', '000568', 'STOCK', 0, 0, 'R3', '白酒', '国窖1573品牌持有者'),
('山西汾酒', '600809', 'STOCK', 0, 0, 'R3', '白酒', '清香型白酒龙头')
ON CONFLICT (code) DO NOTHING;

-- ===== 银行板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('招商银行', '600036', 'STOCK', 0, 0, 'R2', '银行', '零售银行之王'),
('工商银行', '601398', 'STOCK', 0, 0, 'R1', '银行', '全球最大商业银行之一'),
('建设银行', '601939', 'STOCK', 0, 0, 'R1', '银行', '四大国有银行之一'),
('农业银行', '601288', 'STOCK', 0, 0, 'R1', '银行', '服务三农的国有大行'),
('中国银行', '601988', 'STOCK', 0, 0, 'R1', '银行', '国际化程度最高的国有行'),
('兴业银行', '601166', 'STOCK', 0, 0, 'R2', '银行', '同业之王股份制银行'),
('平安银行', '000001', 'STOCK', 0, 0, 'R2', '银行', '平安集团旗下银行')
ON CONFLICT (code) DO NOTHING;

-- ===== 保险/券商板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('中国平安', '601318', 'STOCK', 0, 0, 'R3', '保险', '中国最大综合金融集团'),
('中信证券', '600030', 'STOCK', 0, 0, 'R3', '券商', '中国最大券商'),
('东方财富', '300059', 'STOCK', 0, 0, 'R4', '券商', '互联网券商龙头')
ON CONFLICT (code) DO NOTHING;

-- ===== 新能源板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('宁德时代', '300750', 'STOCK', 0, 0, 'R4', '新能源', '全球动力电池龙头'),
('比亚迪', '002594', 'STOCK', 0, 0, 'R4', '新能源', '新能源汽车与电池双龙头'),
('隆基绿能', '601012', 'STOCK', 0, 0, 'R4', '光伏', '全球最大单晶硅片制造商'),
('阳光电源', '300274', 'STOCK', 0, 0, 'R4', '光伏', '光伏逆变器龙头企业'),
('通威股份', '600438', 'STOCK', 0, 0, 'R4', '光伏', '硅料与电池片双龙头'),
('汇川技术', '300124', 'STOCK', 0, 0, 'R4', '工业', '工业自动化龙头')
ON CONFLICT (code) DO NOTHING;

-- ===== 科技/半导体板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('中芯国际', '688981', 'STOCK', 0, 0, 'R5', '半导体', '中国芯片制造龙头'),
('韦尔股份', '603501', 'STOCK', 0, 0, 'R5', '半导体', '图像传感器设计龙头'),
('北方华创', '002371', 'STOCK', 0, 0, 'R5', '半导体', '半导体设备龙头'),
('中微公司', '688012', 'STOCK', 0, 0, 'R5', '半导体', '刻蚀设备龙头企业'),
('立讯精密', '002475', 'STOCK', 0, 0, 'R4', '消费电子', '精密制造龙头'),
('海康威视', '002415', 'STOCK', 0, 0, 'R3', '安防', '全球安防龙头企业'),
('科大讯飞', '002230', 'STOCK', 0, 0, 'R4', 'AI', '中国AI语音技术龙头'),
('紫光国微', '002049', 'STOCK', 0, 0, 'R5', '芯片', '安全芯片龙头')
ON CONFLICT (code) DO NOTHING;

-- ===== 医药/医疗板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('恒瑞医药', '600276', 'STOCK', 0, 0, 'R3', '创新药', '中国创新药龙头'),
('迈瑞医疗', '300760', 'STOCK', 0, 0, 'R3', '医疗器械', '中国医疗器械龙头'),
('药明康德', '603259', 'STOCK', 0, 0, 'R4', '医药研发', 'CXO龙头企业'),
('爱尔眼科', '300015', 'STOCK', 0, 0, 'R3', '医疗服务', '眼科连锁龙头'),
('片仔癀', '600436', 'STOCK', 0, 0, 'R3', '中药', '国家级中药保护品种'),
('智飞生物', '300122', 'STOCK', 0, 0, 'R4', '疫苗', '疫苗流通与代理龙头')
ON CONFLICT (code) DO NOTHING;

-- ===== 消费/家电板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('美的集团', '000333', 'STOCK', 0, 0, 'R3', '家电', '全球领先家电制造商'),
('格力电器', '000651', 'STOCK', 0, 0, 'R3', '家电', '全球最大空调制造商'),
('海天味业', '603288', 'STOCK', 0, 0, 'R3', '调味品', '中国最大调味品企业'),
('伊利股份', '600887', 'STOCK', 0, 0, 'R2', '乳业', '中国最大乳制品企业'),
('海尔智家', '600690', 'STOCK', 0, 0, 'R3', '家电', '全球化白电龙头'),
('中国中免', '601888', 'STOCK', 0, 0, 'R4', '旅游', '全球最大免税运营商')
ON CONFLICT (code) DO NOTHING;

-- ===== 地产/基建板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('万科A', '000002', 'STOCK', 0, 0, 'R4', '地产', '中国地产龙头企业'),
('保利发展', '600048', 'STOCK', 0, 0, 'R3', '地产', '央企地产龙头'),
('中国建筑', '601668', 'STOCK', 0, 0, 'R2', '基建', '全球最大工程承包商')
ON CONFLICT (code) DO NOTHING;

-- ===== 通信/运营商板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('中国移动', '600941', 'STOCK', 0, 0, 'R2', '通信', '全球最大移动运营商'),
('中国电信', '601728', 'STOCK', 0, 0, 'R2', '通信', '综合信息服务提供商'),
('中兴通讯', '000063', 'STOCK', 0, 0, 'R4', '通信', '5G通信设备龙头')
ON CONFLICT (code) DO NOTHING;

-- ===== 能源/资源板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('中国石油', '601857', 'STOCK', 0, 0, 'R2', '石油', '中国最大油气生产商'),
('中国神华', '601088', 'STOCK', 0, 0, 'R2', '煤炭', '中国最大煤炭企业'),
('紫金矿业', '601899', 'STOCK', 0, 0, 'R3', '矿业', '中国最大矿业公司'),
('长江电力', '600900', 'STOCK', 0, 0, 'R2', '电力', '水电龙头三峡集团旗下')
ON CONFLICT (code) DO NOTHING;

-- ===== 交通/物流板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('顺丰控股', '002352', 'STOCK', 0, 0, 'R3', '物流', '中国快递行业龙头'),
('中远海控', '601919', 'STOCK', 0, 0, 'R3', '航运', '全球集运龙头')
ON CONFLICT (code) DO NOTHING;

-- ===== 军工板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('中航沈飞', '600760', 'STOCK', 0, 0, 'R4', '军工', '歼击机整机龙头'),
('航发动力', '600893', 'STOCK', 0, 0, 'R4', '军工', '航空发动机唯一上市平台')
ON CONFLICT (code) DO NOTHING;

-- ===== 互联网/软件板块 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('金山办公', '688111', 'STOCK', 0, 0, 'R4', '软件', 'WPS办公软件龙头'),
('用友网络', '600588', 'STOCK', 0, 0, 'R4', '软件', '企业管理软件龙头'),
('中国软件', '600536', 'STOCK', 0, 0, 'R4', '软件', '国产操作系统龙头')
ON CONFLICT (code) DO NOTHING;

-- ===== 食品饮料/农业 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('牧原股份', '002714', 'STOCK', 0, 0, 'R4', '养殖', '中国最大生猪养殖企业'),
('温氏股份', '300498', 'STOCK', 0, 0, 'R3', '养殖', '畜牧龙头企业'),
('洋河股份', '002304', 'STOCK', 0, 0, 'R3', '白酒', '绵柔型白酒领军企业')
ON CONFLICT (code) DO NOTHING;

-- ===== 基金/ETF =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('沪深300ETF', '510300', 'FUND', 0, 0, 'R3', '指数基金', '跟踪沪深300指数核心资产'),
('科创50ETF', '588000', 'FUND', 0, 0, 'R4', '指数基金', '跟踪科创板50指数'),
('中证500ETF', '510500', 'FUND', 0, 0, 'R3', '指数基金', '跟踪中证500中盘股'),
('创业板ETF', '159915', 'FUND', 0, 0, 'R4', '指数基金', '跟踪创业板指数'),
('黄金ETF', '518880', 'FUND', 0, 0, 'R2', '商品基金', '紧跟黄金现货价格'),
('纳指100ETF', '513100', 'FUND', 0, 0, 'R4', '跨境基金', '跟踪美股纳指100'),
('红利低波ETF', '512890', 'FUND', 0, 0, 'R2', '策略基金', '高股息低波动策略'),
('半导体ETF', '512480', 'FUND', 0, 0, 'R5', '行业基金', '跟踪半导体产业链')
ON CONFLICT DO NOTHING;

-- ===== 债券 =====
INSERT INTO market_security (name, code, type, current_price, change_percent, risk_level, sector, description) VALUES
('国债2401', '019701', 'BOND', 100.25, 0.02, 'R1', '国债', '2024年记账式国债'),
('国债2503', '019725', 'BOND', 99.85, -0.05, 'R1', '国债', '2025年3年期国债'),
('国开债2024', '220210', 'BOND', 101.20, 0.08, 'R1', '政策性金融债', '国家开发银行债券')
ON CONFLICT DO NOTHING;
