CREATE TABLE IF NOT EXISTS transaction_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    related_asset_id BIGINT COMMENT '关联资产ID',
    asset_name VARCHAR(100) COMMENT '资产名称快照',
    trans_type VARCHAR(20) NOT NULL COMMENT '类型: DEPOSIT(入金), WITHDRAW(出金), BUY(买入), SELL(卖出), DELETE(删除)',
    amount DECIMAL(15,2) NOT NULL COMMENT '变动金额 (+/-)',
    balance_after DECIMAL(15,2) COMMENT '变动后现金余额(可选)',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, create_time)
);
