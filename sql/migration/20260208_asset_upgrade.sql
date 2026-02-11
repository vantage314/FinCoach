/*
 Phase 14 紧急修复：合并资产表结构
 作用：给核心表 asset_item 打补丁，添加持仓相关字段，并迁移 user_asset 演示数据
 日期：2026-02-08
*/

USE fincoach;

-- 1. 给核心表 asset_item 添加缺失的字段 (修复报错的根源)
ALTER TABLE asset_item
ADD COLUMN stock_code VARCHAR(20) COMMENT '关联证券代码',
ADD COLUMN quantity DECIMAL(18, 4) DEFAULT 0 COMMENT '持仓数量',
ADD COLUMN cost_price DECIMAL(18, 4) DEFAULT 0 COMMENT '持仓成本价',
ADD COLUMN market_value DECIMAL(18, 4) DEFAULT 0 COMMENT '实时市值';

-- 2. 将我们刚才在 user_asset 里生成的演示数据，迁移到 asset_item
--    (注意：这里做了一些字段映射，以适配 asset_item 的结构)
INSERT INTO asset_item (user_id, asset_name, sub_type, current_value, stock_code, quantity, cost_price, category_id, holding_cost)
SELECT 
    user_id, 
    asset_name, 
    asset_type,  -- 映射到 sub_type
    amount,      -- 映射到 current_value
    stock_code, 
    quantity, 
    cost_price,
    1,           -- 默认为 1 (假设是 category_id)
    cost_price * quantity -- 估算持仓成本
FROM user_asset;

-- 3. (可选) 清理掉临时的 user_asset 表，避免以后混淆
-- DROP TABLE user_asset;
