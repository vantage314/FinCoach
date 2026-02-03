-- 创建数据库 (如果不存在)
CREATE DATABASE IF NOT EXISTS fincoach DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE fincoach;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 资产分类字典
CREATE TABLE IF NOT EXISTS `asset_category` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon_slug` VARCHAR(50) DEFAULT NULL COMMENT '图标标识符',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产分类字典';

-- 3. 资产明细
CREATE TABLE IF NOT EXISTS `asset_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `category_id` INT NOT NULL COMMENT '分类 ID',
    `asset_name` VARCHAR(100) NOT NULL COMMENT '资产名称',
    `current_value` DECIMAL(18,2) NOT NULL COMMENT '当前市值',
    `holding_cost` DECIMAL(18,2) DEFAULT NULL COMMENT '持仓成本',
    `asset_code` VARCHAR(50) DEFAULT NULL COMMENT '资产代码',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产明细';

-- 4. 风险测评
CREATE TABLE IF NOT EXISTS `risk_assessment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `total_score` INT DEFAULT NULL COMMENT '总分',
    `risk_level` VARCHAR(20) DEFAULT NULL COMMENT '风险等级',
    `assessment_json` TEXT DEFAULT NULL COMMENT '存储答案详情 (JSON)',
    `assess_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '测评日期',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险测评';

-- 5. 投资计划
CREATE TABLE IF NOT EXISTS `investment_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `plan_type` TINYINT DEFAULT NULL COMMENT '计划类型 (1-单笔/2-定投/3-目标)',
    `target_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '目标金额',
    `current_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '当前已投金额',
    `frequency` VARCHAR(50) DEFAULT NULL COMMENT '执行频率',
    `next_execute_date` DATE DEFAULT NULL COMMENT '下次执行日期',
    `status` TINYINT DEFAULT NULL COMMENT '状态 (0-进行, 1-完成)',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投资计划';

-- 6. AI 交互记录
CREATE TABLE IF NOT EXISTS `ai_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `module_context` VARCHAR(100) DEFAULT NULL COMMENT '来自哪个页面/模块',
    `prompt_text` TEXT DEFAULT NULL COMMENT '用户输入的 Prompt',
    `response_text` TEXT DEFAULT NULL COMMENT 'AI 返回的内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 交互记录';

-- 初始化资产分类基础数据
TRUNCATE TABLE `asset_category`;
INSERT INTO `asset_category` (`name`, `icon_slug`, `description`) VALUES
('现金', 'cash', '货币基金、活期存款等'),
('固收', 'bond', '债券、理财产品等'),
('权益', 'stock', '股票、混合基金等'),
('保障', 'insurance', '重疾险、医疗险等'),
('其他', 'other', '黄金、收藏品等');
