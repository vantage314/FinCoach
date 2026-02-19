package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fc_cashflow_month")
public class FcCashflowMonthEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** YYYY-MM */
    private String month;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal net;
    private LocalDateTime updatedAt;
}
