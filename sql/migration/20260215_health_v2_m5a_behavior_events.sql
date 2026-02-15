-- ============================================================
-- HealthV2 M5-A: Behavior Events
-- ============================================================

CREATE TABLE IF NOT EXISTS `fc_behavior_event` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `event_type` VARCHAR(40) NOT NULL COMMENT 'REPORT_GENERATE/REBALANCE_CONFIRM/ASSET_IMPORT/...',
    `amount` DECIMAL(18,2) NULL,
    `meta_json` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行为事件';

-- compatible upgrade for environments created by early M5 migration
ALTER TABLE `fc_behavior_event` MODIFY COLUMN `event_type` VARCHAR(40) NOT NULL;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'fc_behavior_event'
      AND index_name = 'idx_user_time'
);
SET @sql := IF(@idx_exists = 0,
               'CREATE INDEX idx_user_time ON fc_behavior_event(user_id, created_at)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
