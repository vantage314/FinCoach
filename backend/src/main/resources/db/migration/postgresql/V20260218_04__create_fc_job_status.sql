CREATE TABLE IF NOT EXISTS fc_job_status (
  id BIGSERIAL PRIMARY KEY,
  job_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  last_start_at TIMESTAMP NULL,
  last_heartbeat_at TIMESTAMP NULL,
  last_end_at TIMESTAMP NULL,
  last_error VARCHAR(512) NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_job_name ON fc_job_status(job_name);
