package com.fincoach.core.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transaction_record")
public class TransactionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long relatedAssetId;
    private String assetName;
    private String transType; // DEPOSIT, WITHDRAW, BUY, SELL, DELETE
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String remark;
    private LocalDateTime createTime;
}
