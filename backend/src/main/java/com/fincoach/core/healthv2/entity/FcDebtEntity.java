package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fc_debt")
public class FcDebtEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String debtType;
    private BigDecimal principal;
    private BigDecimal apr;
    private Integer termMonths;
    private BigDecimal monthlyPayment;
    private BigDecimal remainingBalance;
    private String externalKey;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
