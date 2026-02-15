package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;
import com.fincoach.core.healthv2.entity.FcLiabilityEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 保险缺口评估 (M3)
 *
 * 输出 advice.insurance：保费预算建议 + 优先级 + 四大险种缺口评估
 */
@Slf4j
@Component
public class InsuranceGapAnalyzer {

    /**
     * @param profile     保险档案（nullable）
     * @param annualIncome 年收入
     * @param liabilities  负债列表（用于寿险额度估算）
     * @return insurance advice map
     */
    public Map<String, Object> analyze(FcInsuranceProfileEntity profile,
                                        BigDecimal annualIncome,
                                        List<FcLiabilityEntity> liabilities) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (profile == null) {
            result.put("premiumRatioSuggestion", Arrays.asList(0.05, 0.10));
            result.put("priority", Arrays.asList("MEDICAL", "ACCIDENT", "CRITICAL_ILLNESS", "LIFE"));
            result.put("gaps", null);
            result.put("messages", Collections.singletonList("💡 请先完善保险档案（年收入、婚姻状况、子女等），以便评估保障缺口。"));
            return result;
        }

        BigDecimal income = annualIncome != null ? annualIncome :
                (profile.getAnnualIncome() != null ? profile.getAnnualIncome() : BigDecimal.ZERO);

        // 家庭责任系数
        int children = profile.getChildrenCount() != null ? profile.getChildrenCount() : 0;
        int dependents = profile.getDependentsCount() != null ? profile.getDependentsCount() : 0;
        boolean married = "MARRIED".equals(profile.getMaritalStatus());
        boolean highResponsibility = children > 0 || dependents > 1 || married;

        // 负债总额（用于寿险额度）
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        if (liabilities != null) {
            for (FcLiabilityEntity l : liabilities) {
                if (l.getPrincipal() != null) totalPrincipal = totalPrincipal.add(l.getPrincipal());
            }
        }

        // 保费预算建议
        double minRatio = highResponsibility ? 0.07 : 0.05;
        double maxRatio = highResponsibility ? 0.12 : 0.10;
        result.put("premiumRatioSuggestion", Arrays.asList(minRatio, maxRatio));

        // 优先级
        List<String> priority;
        if (highResponsibility) {
            priority = Arrays.asList("MEDICAL", "CRITICAL_ILLNESS", "LIFE", "ACCIDENT");
        } else {
            priority = Arrays.asList("MEDICAL", "ACCIDENT", "CRITICAL_ILLNESS", "LIFE");
        }
        result.put("priority", priority);

        // 缺口评估
        Map<String, Object> gaps = new LinkedHashMap<>();

        // 医疗险
        Map<String, Object> medical = new LinkedHashMap<>();
        medical.put("recommended", "覆盖医保外住院与门诊特殊病种支出");
        medical.put("gapLevel", "MEDIUM");
        medical.put("notes", "建议百万医疗险作为基础保障");
        gaps.put("MEDICAL", medical);

        // 意外险
        Map<String, Object> accident = new LinkedHashMap<>();
        BigDecimal accidentRecommended = income.multiply(new BigDecimal("2"));
        accident.put("recommendedAmount", accidentRecommended);
        accident.put("existingAmount", 0);
        accident.put("gapAmount", accidentRecommended);
        accident.put("notes", "意外险性价比高，建议优先配置");
        gaps.put("ACCIDENT", accident);

        // 重疾险
        Map<String, Object> criticalIllness = new LinkedHashMap<>();
        int illnessMultiplier = highResponsibility ? 5 : 3;
        BigDecimal ciRecommended = income.multiply(BigDecimal.valueOf(illnessMultiplier));
        criticalIllness.put("recommendedAmount", ciRecommended);
        criticalIllness.put("existingAmount", 0);
        criticalIllness.put("gapAmount", ciRecommended);
        criticalIllness.put("notes", "覆盖" + illnessMultiplier + "年收入损失 + 治疗费用");
        gaps.put("CRITICAL_ILLNESS", criticalIllness);

        // 寿险
        Map<String, Object> life = new LinkedHashMap<>();
        int lifeMultiplier = highResponsibility ? 7 : 5;
        BigDecimal lifeRecommended = income.multiply(BigDecimal.valueOf(lifeMultiplier)).add(totalPrincipal);
        life.put("recommendedAmount", lifeRecommended);
        life.put("existingAmount", 0);
        life.put("gapAmount", lifeRecommended);
        life.put("notes", "覆盖" + lifeMultiplier + "年家庭支出 + 未偿负债");
        gaps.put("LIFE", life);

        result.put("gaps", gaps);

        // 消息
        List<String> messages = new ArrayList<>();
        if (totalPrincipal.compareTo(BigDecimal.ZERO) > 0) {
            messages.add("有房贷/消费贷等负债，寿险缺口更优先。");
        }
        if (children > 0) {
            messages.add("有子女抚养责任，重疾与寿险额度建议上浮。");
        }
        messages.add("建议先把医疗与意外补齐，再考虑重疾与寿险。");
        result.put("messages", messages);

        return result;
    }
}
