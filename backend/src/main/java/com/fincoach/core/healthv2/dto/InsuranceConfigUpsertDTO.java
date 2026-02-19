package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "保险配置参数")
public class InsuranceConfigUpsertDTO {

    @Schema(description = "医疗险目标保额", example = "500000")
    @DecimalMin(value = "0", message = "targetMedical must be >= 0")
    private BigDecimal targetMedical;

    @Schema(description = "意外险目标保额", example = "500000")
    @DecimalMin(value = "0", message = "targetAccident must be >= 0")
    private BigDecimal targetAccident;

    @Schema(description = "重疾险目标保额", example = "500000")
    @DecimalMin(value = "0", message = "targetCi must be >= 0")
    private BigDecimal targetCi;

    @Schema(description = "寿险倍数(收入乘数)", example = "5")
    @DecimalMin(value = "0", message = "targetLifeMultiplier must be >= 0")
    private BigDecimal targetLifeMultiplier;

    @Schema(description = "保费占比警戒阈值(0-1)", example = "0.10")
    @DecimalMin(value = "0.0001", message = "premiumRatioWarn must be > 0")
    @DecimalMax(value = "1.0", message = "premiumRatioWarn must be <= 1")
    private BigDecimal premiumRatioWarn;

    @Schema(description = "保费占比危险阈值(0-1)", example = "0.20")
    @DecimalMin(value = "0.0001", message = "premiumRatioDanger must be > 0")
    @DecimalMax(value = "1.0", message = "premiumRatioDanger must be <= 1")
    private BigDecimal premiumRatioDanger;
}
