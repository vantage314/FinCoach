CREATE TABLE IF NOT EXISTS fc_alert (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  alert_type VARCHAR(64) NOT NULL,
  severity VARCHAR(16) NOT NULL,
  title VARCHAR(200) NOT NULL,
  message TEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  first_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  hit_count INT NOT NULL DEFAULT 1,
  dedupe_key VARCHAR(128) NOT NULL,
  source VARCHAR(64) NOT NULL,
  meta_json TEXT NULL
);

CREATE INDEX idx_fc_alert_user_status ON fc_alert(user_id, status);
CREATE INDEX idx_fc_alert_type_status ON fc_alert(alert_type, status);
CREATE UNIQUE INDEX uk_fc_alert_dedupe_status ON fc_alert(dedupe_key, status);

CREATE TABLE IF NOT EXISTS fc_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NULL,
  body TEXT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  alert_id BIGINT NULL
);

CREATE INDEX idx_fc_notification_user_read ON fc_notification(user_id, is_read);
CREATE INDEX idx_fc_notification_created_at ON fc_notification(created_at);
