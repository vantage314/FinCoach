package com.fincoach.core.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 资产健康度报告 VO
 */
@Data
@Schema(description = "资产健康度报告")
public class HealthReportVO {
    
    @Schema(description = "健康总分 (0-100)", example = "75")
    private Integer score;
    
    @Schema(description = "健康等级", example = "良好")
    private String level;
    
    @Schema(description = "用户画像", example = "NOVICE")
    private String userType;
    
    @Schema(description = "流动性得分 (0-20)")
    private Integer liquidityScore;
    
    @Schema(description = "风险匹配得分 (0-40)")
    private Integer riskMatchScore;
    
    @Schema(description = "保障力得分 (0-20)")
    private Integer protectionScore;
    
    @Schema(description = "分散度得分 (0-20)")
    private Integer diversityScore;
    
    @Schema(description = "优化建议列表")
    private List<HealthSuggestion> suggestions;
    
    @Data
    @Schema(description = "健康建议项")
    public static class HealthSuggestion {
        @Schema(description = "建议类型：success/warning/info")
        private String type;
        
        @Schema(description = "建议内容")
        private String message;
        
        public HealthSuggestion(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }
}
