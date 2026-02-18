package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fc_portfolio_price_snapshot")
public class FcPortfolioPriceSnapshotEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    @TableField("snap_date")
    private LocalDate asOfDate;
    @TableField("currency")
    private String baseCurrency;
    @TableField("portfolio_value")
    private BigDecimal equity;
    @TableField(exist = false)
    private BigDecimal returns;
    @TableField(exist = false)
    private String positionsJson;
    @TableField("source")
    private String dataSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
