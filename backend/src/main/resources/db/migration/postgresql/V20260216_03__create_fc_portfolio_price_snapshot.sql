CREATE TABLE IF NOT EXISTS fc_portfolio_price_snapshot (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  snap_date DATE NOT NULL,
  source VARCHAR(32) NOT NULL,
  portfolio_value DECIMAL(18, 6) NOT NULL,
  currency VARCHAR(8) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_user_date ON fc_portfolio_price_snapshot(user_id, snap_date);
CREATE INDEX idx_snapshot_user_date ON fc_portfolio_price_snapshot(user_id, snap_date);

COMMENT ON COLUMN fc_portfolio_price_snapshot.source IS 'MARKET_DATA_DAILY_CLOSE / REPORT_NET_WORTH_APPROX / etc';
