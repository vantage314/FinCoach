package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.entity.FcGoalEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 目标账户计划 (M3)
 *
 * 输出 advice.goals：每个目标的进度、所需月投入、状态、风险等级建议
 */
@Slf4j
@Component
public class GoalPlanner {

    /**
     * @param goals          用户目标列表
     * @param monthlySurplus 月结余（nullable）
     * @return goals advice list
     */
    public List<Map<String, Object>> plan(List<FcGoalEntity> goals, BigDecimal monthlySurplus) {
        if (goals == null || goals.isEmpty()) {
            return Collections.emptyList();
        }

        double surplus = monthlySurplus != null ? monthlySurplus.doubleValue() : 0;
        List<Map<String, Object>> result = new ArrayList<>();

        for (FcGoalEntity g : goals) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("goalId", g.getId());
            item.put("type", g.getType());
            item.put("targetAmount", g.getTargetAmount());
            item.put("currentSaved", g.getCurrentSaved());

            BigDecimal target = g.getTargetAmount() != null ? g.getTargetAmount() : BigDecimal.ZERO;
            BigDecimal saved = g.getCurrentSaved() != null ? g.getCurrentSaved() : BigDecimal.ZERO;

            // 进度
            double progress = 0;
            if (target.compareTo(BigDecimal.ZERO) > 0) {
                progress = saved.divide(target, 4, RoundingMode.HALF_UP).doubleValue();
            }
            item.put("progress", Math.round(progress * 10000.0) / 10000.0);

            // 剩余月数
            BigDecimal gap = target.subtract(saved);
            if (gap.compareTo(BigDecimal.ZERO) < 0) gap = BigDecimal.ZERO;

            long monthsLeft = 0;
            if (g.getTargetDate() != null) {
                monthsLeft = ChronoUnit.MONTHS.between(LocalDate.now(), g.getTargetDate());
                if (monthsLeft < 1) monthsLeft = 1;
            }
            item.put("monthsLeft", monthsLeft);

            // 所需月投入
            BigDecimal requiredMonthly = BigDecimal.ZERO;
            if (monthsLeft > 0 && gap.compareTo(BigDecimal.ZERO) > 0) {
                requiredMonthly = gap.divide(BigDecimal.valueOf(monthsLeft), 0, RoundingMode.CEILING);
            }
            item.put("requiredMonthly", requiredMonthly);

            // 推荐月投入（不超过结余的70%）
            BigDecimal recommendedMonthly = requiredMonthly;
            if (surplus > 0 && requiredMonthly.doubleValue() > surplus * 0.7) {
                recommendedMonthly = BigDecimal.valueOf(Math.round(surplus * 0.7));
            }
            item.put("recommendedMonthly", recommendedMonthly);

            // 月计划金额
            item.put("monthlyPlan", g.getMonthlyPlan());

            // 状态判定
            String status;
            String action = null;
            if (gap.compareTo(BigDecimal.ZERO) <= 0) {
                status = "COMPLETED";
            } else if (surplus <= 0) {
                status = "AT_RISK";
                action = "月结余不足，建议降低目标金额或延长期限";
            } else if (requiredMonthly.doubleValue() > surplus * 0.7) {
                status = "AT_RISK";
                action = "所需月投入超过结余70%，建议增加月投入、延长目标期限或降低目标金额";
            } else if (requiredMonthly.doubleValue() > surplus * 0.3) {
                status = "BEHIND";
                action = "建议适当增加月投入以确保按期达标";
            } else {
                status = "ON_TRACK";
            }
            item.put("status", status);
            item.put("action", action);

            // 风险等级建议
            String riskSuggestion;
            if (monthsLeft <= 36) {
                riskSuggestion = "LOW";
            } else if (monthsLeft <= 84) {
                riskSuggestion = "MEDIUM";
            } else {
                riskSuggestion = "MEDIUM_HIGH";
            }
            item.put("riskLevelSuggestion", riskSuggestion);

            result.add(item);
        }

        return result;
    }
}
