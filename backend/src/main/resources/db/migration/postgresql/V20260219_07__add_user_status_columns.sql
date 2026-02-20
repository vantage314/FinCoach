ALTER TABLE "user"
  ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN last_login_at TIMESTAMP NULL;

CREATE INDEX idx_user_enabled ON "user"(enabled);
