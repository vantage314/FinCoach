-- ============================================================
-- FinCoach Base Tables (PostgreSQL)
-- Migrated from MySQL init + early migration scripts
-- ============================================================

-- 1. 用户表 ("user" 是 PG 保留字，需双引号)
CREATE TABLE IF NOT EXISTS "user" (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    enabled INTEGER DEFAULT 1,
    last_login_at TIMESTAMP NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. 资产分类字典
CREATE TABLE IF NOT EXISTS asset_category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    icon_slug VARCHAR(50) DEFAULT NULL,
    description VARCHAR(255) DEFAULT NULL
);

-- 3. 资产明细
CREATE TABLE IF NOT EXISTS asset_item (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id INT NOT NULL,
    asset_name VARCHAR(100) NOT NULL,
    current_value DECIMAL(18,2) NOT NULL,
    holding_cost DECIMAL(18,2) DEFAULT NULL,
    asset_code VARCHAR(50) DEFAULT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_asset_item_user_id ON asset_item(user_id);
CREATE INDEX IF NOT EXISTS idx_asset_item_category_id ON asset_item(category_id);

-- 4. 风险测评
CREATE TABLE IF NOT EXISTS risk_assessment (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_score INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    assessment_json TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_risk_assessment_user_id ON risk_assessment(user_id);

-- 5. 投资计划 (重建后版本)
CREATE TABLE IF NOT EXISTS investment_plan (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    risk_level VARCHAR(20) DEFAULT NULL,
    total_amount DECIMAL(18,2) DEFAULT NULL,
    plan_type VARCHAR(20) DEFAULT NULL,
    invest_money DECIMAL(18,2) DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'draft',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_investment_plan_user_time ON investment_plan(user_id, create_time);

-- 6. 计划明细
CREATE TABLE IF NOT EXISTS plan_item (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    action VARCHAR(10) NOT NULL,
    category_id INT DEFAULT NULL,
    category_name VARCHAR(50) DEFAULT NULL,
    sub_type VARCHAR(100) DEFAULT NULL,
    amount DECIMAL(18,2) DEFAULT NULL,
    current_ratio DECIMAL(10,4) DEFAULT NULL,
    target_ratio DECIMAL(10,4) DEFAULT NULL,
    reason VARCHAR(500) DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_plan_item_plan_id ON plan_item(plan_id);

-- 7. AI 交互记录
CREATE TABLE IF NOT EXISTS ai_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    module_context VARCHAR(100) DEFAULT NULL,
    prompt_text TEXT DEFAULT NULL,
    response_text TEXT DEFAULT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_log_user_id ON ai_log(user_id);

-- 8. 市场证券表
CREATE TABLE IF NOT EXISTS market_security (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    current_price DECIMAL(10,2) DEFAULT NULL,
    change_percent DECIMAL(10,2) DEFAULT NULL,
    risk_level VARCHAR(10) DEFAULT NULL,
    sector VARCHAR(50) DEFAULT NULL,
    description VARCHAR(255) DEFAULT NULL,
    market_cap VARCHAR(50) DEFAULT NULL,
    pe_ratio DECIMAL(10,2) DEFAULT NULL,
    volume VARCHAR(50) DEFAULT NULL,
    high52w DECIMAL(10,2) DEFAULT NULL,
    low52w DECIMAL(10,2) DEFAULT NULL
);

-- 9. 系统用户表 (sys_user)
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) DEFAULT NULL,
    avatar VARCHAR(255) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username);

-- 10. 交易记录
CREATE TABLE IF NOT EXISTS transaction_record (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    related_asset_id BIGINT,
    asset_name VARCHAR(100),
    trans_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2),
    remark VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_transaction_record_user_time ON transaction_record(user_id, create_time);

-- 11. 新闻表
CREATE TABLE IF NOT EXISTS financial_news (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500),
    content TEXT,
    source VARCHAR(50) DEFAULT 'FinCoach财经',
    publish_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    related_code VARCHAR(20),
    url VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_financial_news_time ON financial_news(publish_time);

-- 12. 公司 F10 资料
CREATE TABLE IF NOT EXISTS company_profile (
    stock_code VARCHAR(20) PRIMARY KEY,
    company_name VARCHAR(100),
    establishment_date DATE,
    listing_date DATE,
    chairman VARCHAR(50),
    website VARCHAR(255),
    business_scope TEXT,
    main_business TEXT,
    main_competitors VARCHAR(255),
    employees INT
);

-- 13. 财务报表
CREATE TABLE IF NOT EXISTS financial_report (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    report_name VARCHAR(50),
    revenue VARCHAR(50),
    revenue_growth VARCHAR(20),
    net_profit VARCHAR(50),
    profit_growth VARCHAR(20),
    eps VARCHAR(20),
    roe VARCHAR(20)
);
CREATE INDEX IF NOT EXISTS idx_financial_report_code ON financial_report(stock_code);

-- 14. 公司公告
CREATE TABLE IF NOT EXISTS company_notice (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(20),
    publish_date DATE,
    link VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_company_notice_code ON company_notice(stock_code);

-- 15. 用户自选表
CREATE TABLE IF NOT EXISTS user_watchlist (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, stock_code)
);

-- 16. 行为事件
CREATE TABLE IF NOT EXISTS fc_behavior_event (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    amount DECIMAL(18,2) NULL,
    meta_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_fc_behavior_event_user_time ON fc_behavior_event(user_id, created_at);

-- ============================================================
-- 种子数据
-- ============================================================

-- 资产分类
INSERT INTO asset_category (name, icon_slug, description) VALUES
('现金', 'cash', '货币基金、活期存款等'),
('固收', 'bond', '债券、理财产品等'),
('权益', 'stock', '股票、混合基金等'),
('保障', 'insurance', '重疾险、医疗险等'),
('其他', 'other', '黄金、收藏品等')
ON CONFLICT DO NOTHING;

-- 系统管理员
INSERT INTO sys_user (username, password, nickname, status) VALUES
('admin', '123456', '管理员', 1),
('demo', '123456', '演示用户', 1)
ON CONFLICT (username) DO NOTHING;
