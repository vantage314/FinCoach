ALTER TABLE fc_job_status
  ADD COLUMN recent_events_json TEXT NULL AFTER last_log,
  ADD COLUMN stale_count INT NOT NULL DEFAULT 0 AFTER recent_events_json,
  ADD COLUMN recover_count INT NOT NULL DEFAULT 0 AFTER stale_count,
  ADD COLUMN restart_count INT NOT NULL DEFAULT 0 AFTER recover_count,
  ADD COLUMN last_error_at DATETIME NULL AFTER restart_count;
