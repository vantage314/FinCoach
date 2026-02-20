ALTER TABLE fc_debt
  ADD COLUMN external_key VARCHAR(64) NULL;

CREATE INDEX idx_fc_debt_user_external_key ON fc_debt(user_id, external_key);
