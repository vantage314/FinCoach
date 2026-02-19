CREATE TABLE IF NOT EXISTS fc_insurance_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  annual_income DECIMAL(18,2) NOT NULL DEFAULT 0,
  annual_premium_total DECIMAL(18,2) NOT NULL DEFAULT 0,
  dependents INT NOT NULL DEFAULT 0,
  age INT NULL,
  existing_cover_medical DECIMAL(18,2) NOT NULL DEFAULT 0,
  existing_cover_accident DECIMAL(18,2) NOT NULL DEFAULT 0,
  existing_cover_ci DECIMAL(18,2) NOT NULL DEFAULT 0,
  existing_cover_life DECIMAL(18,2) NOT NULL DEFAULT 0,
  marital_status VARCHAR(16) NULL,
  children_count INT NOT NULL DEFAULT 0,
  dependents_count INT NOT NULL DEFAULT 0,
  city_tier VARCHAR(16) NULL,
  existing_coverage_json TEXT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_fc_insurance_profile_user ON fc_insurance_profile(user_id);

CREATE TABLE IF NOT EXISTS fc_insurance_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  target_medical DECIMAL(18,2) NOT NULL DEFAULT 500000,
  target_accident DECIMAL(18,2) NOT NULL DEFAULT 500000,
  target_ci DECIMAL(18,2) NOT NULL DEFAULT 500000,
  target_life_multiplier DECIMAL(10,4) NOT NULL DEFAULT 5,
  premium_ratio_warn DECIMAL(10,4) NOT NULL DEFAULT 0.10,
  premium_ratio_danger DECIMAL(10,4) NOT NULL DEFAULT 0.20,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO fc_insurance_config
  (code, target_medical, target_accident, target_ci, target_life_multiplier, premium_ratio_warn, premium_ratio_danger)
SELECT 'DEFAULT', 500000, 500000, 500000, 5, 0.10, 0.20
WHERE NOT EXISTS (SELECT 1 FROM fc_insurance_config WHERE code = 'DEFAULT');
