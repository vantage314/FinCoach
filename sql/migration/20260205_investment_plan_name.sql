-- 调仓计划表增强 2
-- 日期: 2026-02-05

ALTER TABLE investment_plan ADD COLUMN plan_name VARCHAR(100) COMMENT '计划名称';
