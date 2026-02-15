package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体检v2-现金流实体
 * UNIQUE(user_id, month)
 */
@Data
@TableName("fc_cashflow")
public class FcCashflowEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 月份 YYYY-MM */
    private String month;

    /** 当月收入 */
    private BigDecimal income;

    /** 固定支出 */
    private BigDecimal fixedExpense;

    /** 可变支出 */
    private BigDecimal variableExpense;

    /** 每月偿债支出 */
    private BigDecimal monthlyDebtPayment;

    private String notes;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public BigDecimal getIncome() {
        return income;
    }

    public BigDecimal getMonthlyDebtPayment() {
        return monthlyDebtPayment;
    }

    public BigDecimal getFixedExpense() {
        return fixedExpense;
    }

    public BigDecimal getVariableExpense() {
        return variableExpense;
    }
}
