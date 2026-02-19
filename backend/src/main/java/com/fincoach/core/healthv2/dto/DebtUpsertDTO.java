package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "创建/更新债务请求")
public class DebtUpsertDTO {

    @Schema(description = "债务ID(更新时传)", example = "1")
    private Long id;

    @NotBlank(message = "债务类型不能为空")
    @Schema(description = "债务类型: MORTGAGE/CAR/CONSUMER/CREDITCARD/OTHER", example = "MORTGAGE")
    private String debtType;

    @NotNull(message = "本金不能为空")
    @DecimalMin(value = "0", message = "本金不能为负数")
    @Schema(description = "本金", example = "500000.00")
    private BigDecimal principal;

    @NotNull(message = "年化利率不能为空")
    @DecimalMin(value = "0", message = "利率不能为负数")
    @Schema(description = "年化利率(小数制，如0.042表示4.2%；兼容百分制输入会自动归一)", example = "0.042")
    private BigDecimal apr;

    @Min(value = 1, message = "期限(月)必须大于0")
    @Schema(description = "期限(月)", example = "240")
    private Integer termMonths;

    @DecimalMin(value = "0", message = "每月还款额不能为负数")
    @Schema(description = "每月还款额(可为空，按公式计算)", example = "3200.00")
    private BigDecimal monthlyPayment;

    @NotNull(message = "剩余余额不能为空")
    @DecimalMin(value = "0", message = "剩余余额不能为负数")
    @Schema(description = "剩余余额", example = "480000.00")
    private BigDecimal remainingBalance;

    @Schema(description = "开始日期(YYYY-MM-DD)", example = "2022-01-01")
    private String startDate;

    @Schema(description = "结束日期(YYYY-MM-DD)", example = "2042-01-01")
    private String endDate;

    @Schema(description = "外部唯一键(导入幂等用)", example = "bank-loan-001")
    private String externalKey;
}
