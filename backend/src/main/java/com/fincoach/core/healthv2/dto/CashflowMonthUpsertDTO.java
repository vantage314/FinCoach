package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "创建/更新现金流月度请求")
public class CashflowMonthUpsertDTO {

    @NotNull(message = "月份不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式必须为 YYYY-MM")
    @Schema(description = "月份 YYYY-MM", example = "2026-01")
    private String month;

    @NotNull(message = "收入不能为空")
    @DecimalMin(value = "0", message = "收入不能为负数")
    @Schema(description = "收入", example = "15000.00")
    private BigDecimal income;

    @NotNull(message = "支出不能为空")
    @DecimalMin(value = "0", message = "支出不能为负数")
    @Schema(description = "支出", example = "8000.00")
    private BigDecimal expense;
}
