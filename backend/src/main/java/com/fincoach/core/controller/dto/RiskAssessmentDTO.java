package com.fincoach.core.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * 风险测评请求 DTO
 */
@Data
@Schema(description = "风险测评请求")
public class RiskAssessmentDTO {
    
    @NotEmpty(message = "答卷不能为空")
    @Schema(description = "答卷：题目ID -> 选项分值", example = "{\"q1\": 5, \"q2\": 3, \"q3\": 5}")
    private Map<String, Integer> answers;
}
