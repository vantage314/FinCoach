package com.fincoach.core.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 调仓计划 VO
 */
@Data
@Schema(description = "调仓计划")
public class InvestmentPlanVO {
    
    @Schema(description = "计划ID")
    private Long id;
    
    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "当时的风险等级")
    private String riskLevel;
    
    @Schema(description = "风险等级标签")
    private String riskLabel;
    
    @Schema(description = "当时的总资产")
    private BigDecimal totalAmount;
    
    @Schema(description = "计划类型: CONTRIBUTION/REBALANCE")
    private String planType;
    
    @Schema(description = "本次投入的新资金额度")
    private BigDecimal investMoney;
    
    @Schema(description = "状态")
    private String status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "建议明细列表")
    private List<PlanItemVO> items;
    
    @Data
    @Schema(description = "调仓建议明细")
    public static class PlanItemVO {
        
        @Schema(description = "明细ID")
        private Long id;
        
        @Schema(description = "操作类型: BUY/SELL")
        private String action;
        
        @Schema(description = "资产大类ID")
        private Integer categoryId;
        
        @Schema(description = "资产大类名称")
        private String categoryName;
        
        @Schema(description = "子类型")
        private String subType;
        
        @Schema(description = "建议操作金额")
        private BigDecimal amount;
        
        @Schema(description = "当前占比")
        private BigDecimal currentRatio;
        
        @Schema(description = "目标占比")
        private BigDecimal targetRatio;
        
        @Schema(description = "推荐理由")
        private String reason;
    }
}
