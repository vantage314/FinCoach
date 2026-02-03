-- 智能调仓计划表
-- 日期: 2026-02-05

-- 计划主表
CREATE TABLE IF NOT EXISTS investment_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '计划ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    risk_level VARCHAR(32) NOT NULL COMMENT '当时的风险等级',
    total_amount DECIMAL(18,2) COMMENT '当时的总资产',
    status VARCHAR(16) DEFAULT 'draft' COMMENT '状态: draft/saved/executed',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调仓计划主表';

-- 计划明细表
CREATE TABLE IF NOT EXISTS plan_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '明细ID',
    plan_id BIGINT NOT NULL COMMENT '计划ID',
    action VARCHAR(8) NOT NULL COMMENT '操作类型: BUY/SELL',
    category_id INT NOT NULL COMMENT '资产大类ID',
    category_name VARCHAR(64) COMMENT '资产大类名称',
    sub_type VARCHAR(64) COMMENT '子类型（如：宽基指数基金）',
    amount DECIMAL(18,2) NOT NULL COMMENT '建议操作金额',
    current_ratio DECIMAL(5,4) COMMENT '当前占比',
    target_ratio DECIMAL(5,4) COMMENT '目标占比',
    reason TEXT COMMENT '推荐理由',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_plan_id (plan_id),
    FOREIGN KEY (plan_id) REFERENCES investment_plan(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调仓建议明细表';
