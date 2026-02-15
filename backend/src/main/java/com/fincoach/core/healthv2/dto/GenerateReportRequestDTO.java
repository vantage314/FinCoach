package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 生成报告请求 DTO
 */
@Data
@Schema(description = "生成体检报告请求")
public class GenerateReportRequestDTO {

    @Schema(description = "用户ID（可选，默认从 Token 获取）")
    private Long userId;

    @Schema(description = "截止日期（可选，默认当天）YYYY-MM-DD")
    private String asOfDate;

    @Schema(description = "月份（可选，用于定位现金流）YYYY-MM")
    private String month;
}
