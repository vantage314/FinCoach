package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新负债请求 DTO
 */
@Data
@Schema(description = "更新负债请求")
public class UpdateLiabilityDTO {

    @NotNull(message = "负债ID不能为空")
    @Schema(description = "负债ID")
    private Long id;

    @Schema(description = "负债类型")
    private String type;

    @Schema(description = "本金/余额")
    private BigDecimal principal;

    @DecimalMin(value = "0", message = "利率不能为负数")
    @Schema(description = "年利率(小数制，如0.042；兼容百分制自动归一)")
    private BigDecimal interestRate;

    @Schema(description = "剩余还款月数")
    private Integer remainingMonths;

    @Schema(description = "每月还款额")
    private BigDecimal monthlyPayment;

    @Schema(description = "提前还款罚金信息(JSON)")
    private String prepayPenaltyJson;
}
