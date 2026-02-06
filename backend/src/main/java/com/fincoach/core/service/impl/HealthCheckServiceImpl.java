package com.fincoach.core.service.impl;

import com.fincoach.core.controller.vo.HealthReportVO;
import com.fincoach.core.controller.vo.HealthReportVO.HealthSuggestion;
import com.fincoach.core.controller.vo.PortfolioSummaryVO;
import com.fincoach.core.controller.vo.RiskAssessmentVO;
import com.fincoach.core.service.AssetItemService;
import com.fincoach.core.service.HealthCheckService;
import com.fincoach.core.service.RiskAssessmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 资产健康度检测服务实现
 * 四维健康模型：流动性、风险匹配、保障力、分散度
 */
@Slf4j
@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    @Autowired
    private AssetItemService assetItemService;

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    // 各维度满分
    private static final int LIQUIDITY_MAX = 20;
    private static final int RISK_MATCH_MAX = 40;
    private static final int PROTECTION_MAX = 20;
    private static final int DIVERSITY_MAX = 20;

    @Override
    public HealthReportVO checkHealth(Long userId) {
        log.info("开始资产健康度体检，用户ID: {}", userId);
        
        HealthReportVO report = new HealthReportVO();
        List<HealthSuggestion> suggestions = new ArrayList<>();
        
        // 获取用户资产汇总
        PortfolioSummaryVO summary = assetItemService.getPortfolioSummary(userId);
        BigDecimal totalAmount = summary.getTotalAmount();
        Map<String, BigDecimal> distribution = summary.getCategoryDistribution();
        
        // 获取风险测评结果
        RiskAssessmentVO riskProfile = riskAssessmentService.getLatest(userId);
        
        // 1. 判定用户类型
        boolean isInvestor = isInvestorUser(distribution);
        report.setUserType(isInvestor ? "INVESTOR" : "NOVICE");
        
        int totalScore;
        
        if (!isInvestor) {
            // ===== 小白模式 (Novice Mode) =====
            totalScore = calculateNoviceScore(totalAmount, distribution, suggestions);
            // 填充默认维度分，避免空值
            report.setLiquidityScore(totalScore); 
            report.setRiskMatchScore(0);
            report.setProtectionScore(0); 
            report.setDiversityScore(0);
        } else {
            // ===== 进阶模式 (Investor Mode) =====
            // A. 流动性评分 (20分)
            int liquidityScore = calculateLiquidity(totalAmount, distribution, suggestions);
            report.setLiquidityScore(liquidityScore);
            
            // B. 风险匹配评分 (40分)
            int riskMatchScore = calculateRiskMatch(totalAmount, distribution, riskProfile, suggestions);
            report.setRiskMatchScore(riskMatchScore);
            
            // C. 保障力评分 (20分)
            int protectionScore = calculateProtection(distribution, suggestions);
            report.setProtectionScore(protectionScore);
            
            // D. 分散度评分 (20分)
            int diversityScore = calculateDiversity(distribution, suggestions);
            report.setDiversityScore(diversityScore);
            
            totalScore = liquidityScore + riskMatchScore + protectionScore + diversityScore;
        }
        
        report.setScore(totalScore);
        report.setLevel(getLevel(totalScore));
        report.setSuggestions(suggestions);
        
        log.info("体检完成，用户ID: {}, 类型: {}, 总分: {}", userId, report.getUserType(), totalScore);
        return report;
    }

    private boolean isInvestorUser(Map<String, BigDecimal> dist) {
        BigDecimal equity = dist.getOrDefault("金融投资", BigDecimal.ZERO);
        return equity.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 小白模式评分算法
     * 基准分 60
     * - 没有投资资产 -> 扣 10 分
     * + 现金流充足 (> 30000) -> 加 20 分
     */
    private int calculateNoviceScore(BigDecimal total, Map<String, BigDecimal> dist, List<HealthSuggestion> suggestions) {
        int score = 60;
        
        // 检查抗通胀能力
        suggestions.add(new HealthSuggestion("warning", "⚠️ 您目前资产主要集中在储蓄，虽然安全但难以跑赢通胀"));
        score -= 10;
        
        // 检查应急准备金 (默认阈值 30000)
        BigDecimal cash = dist.getOrDefault("现金储蓄", BigDecimal.ZERO);
        if (cash.compareTo(new BigDecimal("30000")) >= 0) {
            suggestions.add(new HealthSuggestion("success", "✅ 应急准备金充足 (>6个月支出)"));
            score += 20;
        } else {
            suggestions.add(new HealthSuggestion("info", "💡 建议并在留足3~6个月应急金后，尝试低风险理财"));
        }
        
        return Math.min(100, Math.max(0, score));
    }

    /**
     * 流动性评分：现金类资产占比是否 >= 10%
     */
    private int calculateLiquidity(BigDecimal total, Map<String, BigDecimal> dist, List<HealthSuggestion> suggestions) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            suggestions.add(new HealthSuggestion("info", "📋 请先录入资产以获取精准诊断"));
            return 0;
        }
        
        BigDecimal cashAmount = dist.getOrDefault("现金储蓄", BigDecimal.ZERO);
        BigDecimal cashRatio = cashAmount.divide(total, 4, RoundingMode.HALF_UP);
        double ratio = cashRatio.doubleValue();
        
        if (ratio >= 0.10) {
            suggestions.add(new HealthSuggestion("success", "✅ 流动性充足，可应对突发支出"));
            return LIQUIDITY_MAX;
        } else if (ratio >= 0.05) {
            suggestions.add(new HealthSuggestion("warning", "💡 建议适当增加流动资金储备"));
            return 12;
        } else {
            suggestions.add(new HealthSuggestion("warning", "⚠️ 流动资金不足，建议补充现金储备"));
            return 5;
        }
    }

    /**
     * 风险匹配评分：权益类占比是否与风险偏好匹配
     */
    private int calculateRiskMatch(BigDecimal total, Map<String, BigDecimal> dist, 
                                    RiskAssessmentVO riskProfile, List<HealthSuggestion> suggestions) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        
        // 防御性编程：没做风险测评给满分
        if (riskProfile == null) {
            suggestions.add(new HealthSuggestion("info", "📝 请先完成风险测评以获得精准配置建议"));
            return RISK_MATCH_MAX;
        }
        
        BigDecimal equityAmount = dist.getOrDefault("金融投资", BigDecimal.ZERO);
        double actualRatio = equityAmount.divide(total, 4, RoundingMode.HALF_UP).doubleValue();
        double idealRatio = riskProfile.getEquityLimit().doubleValue();
        
        double gap = Math.abs(actualRatio - idealRatio);
        
        if (gap <= 0.10) {
            suggestions.add(new HealthSuggestion("success", "✅ 风险配置与偏好完美匹配"));
            return RISK_MATCH_MAX;
        } else if (gap <= 0.20) {
            int deduct = (int) ((gap - 0.10) / 0.05) * 5;
            if (actualRatio > idealRatio) {
                suggestions.add(new HealthSuggestion("warning", "💡 权益仓位略重，可适度调整"));
            } else {
                suggestions.add(new HealthSuggestion("info", "💡 可适当增加权益配置"));
            }
            return Math.max(20, RISK_MATCH_MAX - deduct);
        } else {
            if (actualRatio > idealRatio) {
                suggestions.add(new HealthSuggestion("warning", "⚠️ 权益仓位过重，建议降低风险敞口"));
            } else {
                suggestions.add(new HealthSuggestion("warning", "⚠️ 配置过于保守，资产可能跑输通胀"));
            }
            return 10;
        }
    }

    /**
     * 保障力评分：是否有保险类资产
     * 注：当前资产分类中暂无保险类，此处预留逻辑
     */
    private int calculateProtection(Map<String, BigDecimal> dist, List<HealthSuggestion> suggestions) {
        // 暂时检查是否有固定资产作为"保障"替代
        BigDecimal propertyAmount = dist.getOrDefault("固定资产", BigDecimal.ZERO);
        
        if (propertyAmount.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.add(new HealthSuggestion("success", "✅ 拥有固定资产作为保障"));
            return PROTECTION_MAX;
        } else {
            suggestions.add(new HealthSuggestion("info", "💡 建议配置固定资产增强保障力"));
            return 10;
        }
    }

    /**
     * 分散度评分：持仓资产种类是否 > 2 种
     */
    private int calculateDiversity(Map<String, BigDecimal> dist, List<HealthSuggestion> suggestions) {
        int categoryCount = 0;
        for (BigDecimal amount : dist.values()) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                categoryCount++;
            }
        }
        
        if (categoryCount >= 3) {
            suggestions.add(new HealthSuggestion("success", "✅ 资产配置分散，风险可控"));
            return DIVERSITY_MAX;
        } else if (categoryCount == 2) {
            suggestions.add(new HealthSuggestion("info", "💡 可增加资产类别进一步分散风险"));
            return 14;
        } else {
            suggestions.add(new HealthSuggestion("warning", "⚠️ 资产过于集中，建议多元配置"));
            return 5;
        }
    }

    private String getLevel(int score) {
        if (score >= 80) return "优秀";
        if (score >= 60) return "良好";
        if (score >= 40) return "一般";
        return "待优化";
    }
}
