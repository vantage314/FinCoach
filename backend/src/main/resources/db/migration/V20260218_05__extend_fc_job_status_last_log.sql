ALTER TABLE fc_job_status
  ADD COLUMN last_log VARCHAR(2000) NULL AFTER last_error;
