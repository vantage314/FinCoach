-- 备份旧表 (如果需要)
ALTER TABLE `investment_plan` RENAME TO `investment_plan_old`;

-- 创建新的调仓计划表
CREATE TABLE `investment_plan` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `plan_name` varchar(100) NOT NULL COMMENT '计划名称',
  `risk_level` varchar(20) DEFAULT NULL COMMENT '风险等级',
  `total_amount` decimal(18,2) DEFAULT NULL COMMENT '总资产',
  `plan_type` varchar(20) DEFAULT NULL COMMENT '计划类型 (CONTRIBUTION/REBALANCE)',
  `invest_money` decimal(18,2) DEFAULT NULL COMMENT '新增投资金额',
  `status` varchar(20) DEFAULT 'draft' COMMENT '状态 (draft/saved/executed)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能调仓计划表';

-- 检查 plan_item 表是否存在，若已存在则无需操作，若不存在则创建
CREATE TABLE IF NOT EXISTS `plan_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_id` bigint(20) NOT NULL COMMENT '所属计划ID',
  `action` varchar(10) NOT NULL COMMENT '操作 (BUY/SELL)',
  `category_id` int(11) DEFAULT NULL COMMENT '资产分类ID',
  `category_name` varchar(50) DEFAULT NULL COMMENT '分类名称',
  `sub_type` varchar(100) DEFAULT NULL COMMENT '具体标的/说明',
  `amount` decimal(18,2) DEFAULT NULL COMMENT '操作金额',
  `current_ratio` decimal(10,4) DEFAULT NULL COMMENT '当前占比',
  `target_ratio` decimal(10,4) DEFAULT NULL COMMENT '目标占比',
  `reason` varchar(500) DEFAULT NULL COMMENT '推荐理由',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划明细表';
