package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "保险档案 V2 请求")
public class InsuranceProfileUpsertV2DTO {

    @Schema(description = "年收入", example = "200000.00")
    @DecimalMin(value = "0", message = "annualIncome must be >= 0")
    private BigDecimal annualIncome;

    @Schema(description = "年度保费总额", example = "12000.00")
    @DecimalMin(value = "0", message = "annualPremiumTotal must be >= 0")
    private BigDecimal annualPremiumTotal;

    @Schema(description = "被赡养人数", example = "2")
    @Min(value = 0, message = "dependents must be >= 0")
    private Integer dependents;

    @Schema(description = "年龄", example = "35")
    @Min(value = 0, message = "age must be >= 0")
    private Integer age;

    @Schema(description = "已有医疗险保额", example = "300000.00")
    @DecimalMin(value = "0", message = "existingCoverMedical must be >= 0")
    private BigDecimal existingCoverMedical;

    @Schema(description = "已有意外险保额", example = "200000.00")
    @DecimalMin(value = "0", message = "existingCoverAccident must be >= 0")
    private BigDecimal existingCoverAccident;

    @Schema(description = "已有重疾险保额", example = "200000.00")
    @DecimalMin(value = "0", message = "existingCoverCi must be >= 0")
    private BigDecimal existingCoverCi;

    @Schema(description = "已有寿险保额", example = "500000.00")
    @DecimalMin(value = "0", message = "existingCoverLife must be >= 0")
    private BigDecimal existingCoverLife;
}
