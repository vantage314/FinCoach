CREATE TABLE IF NOT EXISTS fc_portfolio_price_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  snap_date DATE NOT NULL,
  source VARCHAR(32) NOT NULL COMMENT 'MARKET_DATA_DAILY_CLOSE / REPORT_NET_WORTH_APPROX / etc',
  portfolio_value DECIMAL(18, 6) NOT NULL,
  currency VARCHAR(8) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (user_id, snap_date)
);

CREATE INDEX idx_snapshot_user_date ON fc_portfolio_price_snapshot(user_id, snap_date);
