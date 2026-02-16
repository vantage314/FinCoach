package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private LocalDate asOfDate;
    private String baseCurrency;
    private BigDecimal equity;
    private BigDecimal returns;
    private String positionsJson;
    private String dataSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
