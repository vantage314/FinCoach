ALTER TABLE fc_job_status
  ADD COLUMN recent_events_json TEXT NULL,
  ADD COLUMN stale_count INT NOT NULL DEFAULT 0,
  ADD COLUMN recover_count INT NOT NULL DEFAULT 0,
  ADD COLUMN restart_count INT NOT NULL DEFAULT 0,
  ADD COLUMN last_error_at TIMESTAMP NULL;
