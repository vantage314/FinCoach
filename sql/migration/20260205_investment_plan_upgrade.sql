-- 调仓计划表增强
-- 日期: 2026-02-05

ALTER TABLE investment_plan ADD COLUMN plan_type VARCHAR(20) DEFAULT 'REBALANCE' COMMENT '计划类型: CONTRIBUTION (新增资金), REBALANCE (存量调整)';
ALTER TABLE investment_plan ADD COLUMN invest_money DECIMAL(18,2) DEFAULT 0.00 COMMENT '本次投入的新资金额度';
