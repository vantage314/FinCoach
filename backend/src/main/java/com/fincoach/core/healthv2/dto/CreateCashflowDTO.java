package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建/更新现金流请求 DTO（upsert语义：同 user_id+month 存在则更新）
 */
@Data
@Schema(description = "创建/更新现金流请求")
public class CreateCashflowDTO {

    @NotNull(message = "月份不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式必须为 YYYY-MM")
    @Schema(description = "月份 YYYY-MM", example = "2026-01")
    private String month;

    @NotNull(message = "收入不能为空")
    @Schema(description = "当月收入", example = "15000.00")
    private BigDecimal income;

    @NotNull(message = "固定支出不能为空")
    @Schema(description = "固定支出", example = "5000.00")
    private BigDecimal fixedExpense;

    @NotNull(message = "可变支出不能为空")
    @Schema(description = "可变支出", example = "3000.00")
    private BigDecimal variableExpense;

    @Schema(description = "每月偿债支出", example = "3200.00")
    private BigDecimal monthlyDebtPayment;

    @Schema(description = "备注")
    private String notes;
}
