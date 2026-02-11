package com.fincoach.core.controller.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 资产分析结果 VO
 * 包含总资产、盈亏、健康分和投资建议
 */
@Data
public class AssetAnalysisVO {
    // 核心数据
    private BigDecimal totalAsset;      // 总资产 (实时)
    private BigDecimal totalProfit;     // 总盈亏 (实时)
    private BigDecimal dayProfit;       // 今日盈亏 (估算)
    
    // 健康体检
    private Integer healthScore;        // 健康分 (0-100)
    private String healthLevel;         // 健康等级 (优秀/良好/亚健康/高危)
    private List<String> suggestions;   // 具体的投资建议
    
    // 图表数据
    private Map<String, BigDecimal> typeDistribution; // 资产分布 (权益/固收/现金)
    private List<AssetItemVO> topHoldings; // 重仓前5名
    
    /**
     * 资产项目 VO
     */
    @Data
    public static class AssetItemVO {
        private String name;
        private String code;
        private BigDecimal value;
        private BigDecimal percent;
    }
}
