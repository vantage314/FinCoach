package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建资产请求 DTO
 */
@Data
@Schema(description = "创建资产请求")
public class CreateAssetDTO {

    @NotBlank(message = "资产类型不能为空")
    @Schema(description = "资产类型: CASH/STOCK/FUND/BOND/REAL_ESTATE/OTHER", example = "CASH")
    private String type;

    @NotBlank(message = "资产名称不能为空")
    @Schema(description = "资产名称", example = "招商银行活期存款")
    private String name;

    @NotNull(message = "资产金额不能为空")
    @Schema(description = "资产金额", example = "50000.00")
    private BigDecimal amount;

    @Schema(description = "币种，默认CNY", example = "CNY")
    private String currency;

    @Schema(description = "风险等级: LOW/MEDIUM/HIGH", example = "LOW")
    private String riskLevel;

    @Schema(description = "估值日期 YYYY-MM-DD", example = "2026-01-15")
    private String asOfDate;

    @Schema(description = "扩展信息(JSON字符串)")
    private String metaJson;
}
