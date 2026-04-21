-- ============================================================
-- 补全 company_profile 表缺失的 description 列
-- fetch_f10_sina.py 脚本会写入该列
-- ============================================================

ALTER TABLE company_profile ADD COLUMN IF NOT EXISTS description TEXT DEFAULT NULL;
ALTER TABLE company_profile ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
