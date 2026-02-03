package com.fincoach.core.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风险测评结果 VO
 */
@Data
@Schema(description = "风险测评结果")
public class RiskAssessmentVO {
    
    @Schema(description = "测评ID")
    private Long id;
    
    @Schema(description = "总分", example = "55")
    private Integer totalScore;
    
    @Schema(description = "风险等级代码", example = "balanced")
    private String riskLevel;
    
    @Schema(description = "风险等级标签", example = "平衡探索者")
    private String label;
    
    @Schema(description = "风险等级描述", example = "攻守兼备，股债各半")
    private String description;
    
    @Schema(description = "建议权益仓位上限", example = "0.40")
    private BigDecimal equityLimit;
    
    @Schema(description = "测评时间")
    private LocalDateTime createTime;
    
    // ===== 知行合一诊断 =====
    
    @Schema(description = "当前实际权益占比", example = "0.25")
    private BigDecimal actualRatio;
    
    @Schema(description = "理想权益占比", example = "0.40")
    private BigDecimal idealRatio;
    
    @Schema(description = "偏差值 (actual - ideal)", example = "-0.15")
    private BigDecimal gap;
    
    @Schema(description = "AI诊断文案")
    private String diagnosis;
}
