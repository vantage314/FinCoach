-- ============================================================
-- 补全 HealthV2 模块缺失的表 + asset_item 缺失列
-- ============================================================

-- 1. asset_item 补充列（Phase 14 持仓字段）
ALTER TABLE asset_item ADD COLUMN IF NOT EXISTS sub_type VARCHAR(100) NULL;
ALTER TABLE asset_item ADD COLUMN IF NOT EXISTS stock_code VARCHAR(50) NULL;
ALTER TABLE asset_item ADD COLUMN IF NOT EXISTS quantity DECIMAL(18,4) NULL;
ALTER TABLE asset_item ADD COLUMN IF NOT EXISTS cost_price DECIMAL(18,4) NULL;
ALTER TABLE asset_item ADD COLUMN IF NOT EXISTS market_value DECIMAL(18,2) NULL;

-- 2. fc_asset（体检v2-资产）
CREATE TABLE IF NOT EXISTS fc_asset (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    currency VARCHAR(10) DEFAULT 'CNY',
    risk_level VARCHAR(20) NULL,
    as_of_date DATE NULL,
    meta_json TEXT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_fc_asset_user_id ON fc_asset(user_id);

-- 3. fc_liability（体检v2-负债）
CREATE TABLE IF NOT EXISTS fc_liability (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    principal DECIMAL(18,2) NOT NULL DEFAULT 0,
    interest_rate DECIMAL(10,4) NULL,
    monthly_payment DECIMAL(18,2) NULL,
    remaining_months INT NULL,
    prepay_penalty_json TEXT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_fc_liability_user_id ON fc_liability(user_id);

-- 4. fc_goal（体检v2-理财目标）
CREATE TABLE IF NOT EXISTS fc_goal (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    target_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    target_date DATE NULL,
    current_saved DECIMAL(18,2) NULL DEFAULT 0,
    monthly_plan DECIMAL(18,2) NULL DEFAULT 0,
    risk_level_suggestion VARCHAR(20) NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_fc_goal_user_id ON fc_goal(user_id);

-- 5. fc_cashflow（体检v2-现金流月度）
CREATE TABLE IF NOT EXISTS fc_cashflow (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    month VARCHAR(7) NOT NULL,
    income DECIMAL(18,2) NULL DEFAULT 0,
    fixed_expense DECIMAL(18,2) NULL DEFAULT 0,
    variable_expense DECIMAL(18,2) NULL DEFAULT 0,
    monthly_debt_payment DECIMAL(18,2) NULL DEFAULT 0,
    notes TEXT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, month)
);
CREATE INDEX IF NOT EXISTS idx_fc_cashflow_user_month ON fc_cashflow(user_id, month);

-- 6. fc_insurance_profile（体检v2-保险档案）
CREATE TABLE IF NOT EXISTS fc_insurance_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    annual_income DECIMAL(18,2) NULL,
    marital_status VARCHAR(20) NULL,
    children_count INT NULL DEFAULT 0,
    dependents_count INT NULL DEFAULT 0,
    city_tier VARCHAR(20) NULL,
    existing_coverage_json TEXT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. fc_insurance_param（保险参数配置）
CREATE TABLE IF NOT EXISTS fc_insurance_param (
    id BIGSERIAL PRIMARY KEY,
    param_key VARCHAR(100) NOT NULL,
    value_json TEXT NULL,
    description VARCHAR(500) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. fc_audit_log（体检v2-审计日志）
CREATE TABLE IF NOT EXISTS fc_audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    target_type VARCHAR(60) NULL,
    target_id BIGINT NULL,
    before_json TEXT NULL,
    after_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_fc_audit_log_actor ON fc_audit_log(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_fc_audit_log_target ON fc_audit_log(target_type, target_id);

-- 9. fc_advice_template（建议模板）
CREATE TABLE IF NOT EXISTS fc_advice_template (
    id BIGSERIAL PRIMARY KEY,
    scene_key VARCHAR(100) NOT NULL,
    template_text TEXT NULL,
    variables_json TEXT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. fc_alert_rule（预警规则配置）
CREATE TABLE IF NOT EXISTS fc_alert_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_key VARCHAR(100) NOT NULL,
    thresholds_json TEXT NULL,
    severity VARCHAR(20) NULL,
    message_template TEXT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. fc_strategy_flag（策略开关）
CREATE TABLE IF NOT EXISTS fc_strategy_flag (
    id BIGSERIAL PRIMARY KEY,
    flag_key VARCHAR(100) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(500) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. fc_score_rule_version（评分规则版本）
CREATE TABLE IF NOT EXISTS fc_score_rule_version (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    weights_json TEXT NULL,
    thresholds_json TEXT NULL,
    description VARCHAR(500) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 13. fc_asset_class_param（资产类别参数）
CREATE TABLE IF NOT EXISTS fc_asset_class_param (
    asset_type VARCHAR(40) PRIMARY KEY,
    expected_return DECIMAL(10,6) NULL,
    volatility DECIMAL(10,6) NULL,
    risk_free_flag INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 14. fc_asset_corr_param（资产相关性矩阵）
CREATE TABLE IF NOT EXISTS fc_asset_corr_param (
    type_a VARCHAR(40) NOT NULL,
    type_b VARCHAR(40) NOT NULL,
    corr DECIMAL(10,6) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (type_a, type_b)
);
