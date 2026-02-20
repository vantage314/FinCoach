CREATE TABLE IF NOT EXISTS fc_debt (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  debt_type VARCHAR(32) NOT NULL,
  principal DECIMAL(18,2) NOT NULL DEFAULT 0,
  apr DECIMAL(10,6) NOT NULL DEFAULT 0,
  term_months INT NULL,
  monthly_payment DECIMAL(18,2) NULL,
  remaining_balance DECIMAL(18,2) NOT NULL DEFAULT 0,
  start_date DATE NULL,
  end_date DATE NULL,
  is_active TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_fc_debt_user_active ON fc_debt(user_id, is_active);
CREATE INDEX idx_fc_debt_user_type ON fc_debt(user_id, debt_type);

CREATE TABLE IF NOT EXISTS fc_cashflow_month (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  month VARCHAR(7) NOT NULL,
  income DECIMAL(18,2) NOT NULL DEFAULT 0,
  expense DECIMAL(18,2) NOT NULL DEFAULT 0,
  net DECIMAL(18,2) NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_fc_cashflow_month_user_month (user_id, month)
);

CREATE INDEX idx_fc_cashflow_month_user_month ON fc_cashflow_month(user_id, month);
