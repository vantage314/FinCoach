-- M7-3 Portfolio Price Snapshot Table
CREATE TABLE IF NOT EXISTS fc_portfolio_price_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    as_of_date DATE NOT NULL COMMENT '快照日期',
    base_currency VARCHAR(10) DEFAULT 'CNY' COMMENT '基准货币',
    equity DECIMAL(19, 4) COMMENT '组合总市值',
    returns DECIMAL(10, 6) COMMENT '当日收益率',
    positions_json TEXT COMMENT '持仓明细JSON(symbol, qty, price, weight)',
    data_source VARCHAR(50) COMMENT '数据源(MARKET/MANUAL)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, as_of_date),
    INDEX idx_user_date (user_id, as_of_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组合每日市值快照';
