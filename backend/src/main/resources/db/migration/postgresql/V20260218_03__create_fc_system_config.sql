CREATE TABLE IF NOT EXISTS fc_system_config (
  id BIGSERIAL PRIMARY KEY,
  cfg_key VARCHAR(128) NOT NULL,
  cfg_value VARCHAR(256) NOT NULL,
  updated_by BIGINT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_cfg_key ON fc_system_config(cfg_key);
