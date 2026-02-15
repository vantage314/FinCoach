-- ============================================================
-- HealthV2 M5-B: Notifications
-- ============================================================

CREATE TABLE IF NOT EXISTS `fc_notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(32) NOT NULL COMMENT 'ALERT/SYSTEM/ADVICE',
    `title` VARCHAR(128) NOT NULL,
    `content` VARCHAR(1024) NOT NULL,
    `payload_json` TEXT NULL,
    `is_read` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_read_time` (`user_id`, `is_read`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';

-- compatible upgrade for environments created by early M5 migration
UPDATE `fc_notification` SET `content` = '' WHERE `content` IS NULL;
ALTER TABLE `fc_notification` MODIFY COLUMN `title` VARCHAR(128) NOT NULL;
ALTER TABLE `fc_notification` MODIFY COLUMN `content` VARCHAR(1024) NOT NULL;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'fc_notification'
      AND index_name = 'idx_user_read_time'
);
SET @sql := IF(@idx_exists = 0,
               'CREATE INDEX idx_user_read_time ON fc_notification(user_id, is_read, created_at)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
