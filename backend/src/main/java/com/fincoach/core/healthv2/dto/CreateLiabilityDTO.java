package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建负债请求 DTO
 */
@Data
@Schema(description = "创建负债请求")
public class CreateLiabilityDTO {

    @NotBlank(message = "负债类型不能为空")
    @Schema(description = "负债类型: MORTGAGE/CAR_LOAN/CREDIT_CARD/CONSUMER_LOAN/OTHER", example = "MORTGAGE")
    private String type;

    @NotNull(message = "本金/余额不能为空")
    @Schema(description = "本金/余额", example = "500000.00")
    private BigDecimal principal;

    @DecimalMin(value = "0", message = "利率不能为负数")
    @Schema(description = "年利率(小数制，如0.042表示4.2%；兼容百分制输入会自动归一)", example = "0.042")
    private BigDecimal interestRate;

    @Schema(description = "剩余还款月数", example = "240")
    private Integer remainingMonths;

    @Schema(description = "每月还款额", example = "3200.00")
    private BigDecimal monthlyPayment;

    @Schema(description = "提前还款罚金信息(JSON)")
    private String prepayPenaltyJson;
}
