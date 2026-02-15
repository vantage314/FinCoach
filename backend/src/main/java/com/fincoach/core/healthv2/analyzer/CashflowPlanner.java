package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.entity.FcCashflowEntity;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 现金流计划与应急金计划 (M3)
 *
 * 输出 advice.cashflowPlan：月度预算分配 + 应急金缺口 + 消息
 */
@Slf4j
@Component
public class CashflowPlanner {

    /**
     * @param cashflow         最新现金流（nullable）
     * @param cashAssets        现金类资产
     * @param emergencyMonths   应急月数（nullable）
     * @param monthlySurplus    月结余（nullable）
     * @param essentialExpense  必要支出（nullable）
     * @param insuranceProfile  保险档案（用于判断家庭责任）
     * @param hasLiabilities    是否有负债
     * @return cashflowPlan map
     */
    public Map<String, Object> plan(FcCashflowEntity cashflow,
                                     BigDecimal cashAssets,
                                     BigDecimal emergencyMonths,
                                     BigDecimal monthlySurplus,
                                     BigDecimal essentialExpense,
                                     FcInsuranceProfileEntity insuranceProfile,
                                     boolean hasLiabilities) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (cashflow == null) {
            result.put("essentialExpense", null);
            result.put("monthlySurplus", null);
            result.put("emergencyMonths", null);
            result.put("targetEmergencyMonths", 3);
            result.put("emergencyGap", null);
            result.put("monthlyPlan", null);
            result.put("messages", Collections.singletonList("💡 请先录入现金流数据，以便生成预算计划。"));
            return result;
        }

        result.put("essentialExpense", essentialExpense);
        result.put("monthlySurplus", monthlySurplus);
        result.put("emergencyMonths", emergencyMonths);

        // 判断目标应急月数
        int targetMonths = 3;
        if (insuranceProfile != null) {
            boolean highResponsibility = (insuranceProfile.getChildrenCount() != null && insuranceProfile.getChildrenCount() > 0)
                    || (insuranceProfile.getDependentsCount() != null && insuranceProfile.getDependentsCount() > 1);
            if (highResponsibility || hasLiabilities) {
                targetMonths = 6;
            }
        }
        result.put("targetEmergencyMonths", targetMonths);

        // 应急金缺口
        BigDecimal targetAmount = essentialExpense != null
                ? essentialExpense.multiply(BigDecimal.valueOf(targetMonths))
                : BigDecimal.ZERO;
        BigDecimal emergencyGap = targetAmount.subtract(cashAssets != null ? cashAssets : BigDecimal.ZERO);
        if (emergencyGap.compareTo(BigDecimal.ZERO) < 0) emergencyGap = BigDecimal.ZERO;
        result.put("emergencyGap", emergencyGap);

        // 月度预算分配
        Map<String, Object> monthlyPlan = new LinkedHashMap<>();
        BigDecimal income = cashflow.getIncome() != null ? cashflow.getIncome() : BigDecimal.ZERO;
        BigDecimal debtPay = cashflow.getMonthlyDebtPayment() != null ? cashflow.getMonthlyDebtPayment() : BigDecimal.ZERO;

        monthlyPlan.put("payDebt", debtPay);

        BigDecimal surplus = monthlySurplus != null ? monthlySurplus : BigDecimal.ZERO;
        BigDecimal buildEmergency = BigDecimal.ZERO;
        BigDecimal invest = BigDecimal.ZERO;

        if (emergencyGap.compareTo(BigDecimal.ZERO) > 0 && surplus.compareTo(BigDecimal.ZERO) > 0) {
            // 应急金缺口存在，优先补
            buildEmergency = surplus.multiply(new BigDecimal("0.6")).setScale(0, RoundingMode.HALF_UP);
            invest = surplus.subtract(buildEmergency);
            if (invest.compareTo(BigDecimal.ZERO) < 0) invest = BigDecimal.ZERO;
        } else if (surplus.compareTo(BigDecimal.ZERO) > 0) {
            // 应急金充足，全部投资
            invest = surplus;
        }

        monthlyPlan.put("buildEmergencyFund", buildEmergency);
        monthlyPlan.put("invest", invest);

        // 可支配消费上限（收入 - 债务 - 应急储蓄 - 投资）
        BigDecimal spendingCap = income.subtract(debtPay).subtract(buildEmergency).subtract(invest);
        if (spendingCap.compareTo(BigDecimal.ZERO) < 0) spendingCap = BigDecimal.ZERO;
        monthlyPlan.put("spendingCap", spendingCap);

        result.put("monthlyPlan", monthlyPlan);

        // 消息
        List<String> messages = new ArrayList<>();
        if (emergencyMonths != null && emergencyMonths.compareTo(BigDecimal.valueOf(targetMonths)) < 0) {
            messages.add("先把应急金补到" + targetMonths + "个月，再考虑提高权益仓位。");
        }
        if (surplus.compareTo(BigDecimal.ZERO) <= 0) {
            messages.add("月结余为零或负，建议控制可变支出或寻求增收途径。");
        } else if (surplus.compareTo(income.multiply(new BigDecimal("0.1"))) < 0) {
            messages.add("月结余偏低（不足收入10%），建议审视可变支出。");
        }
        if (emergencyGap.compareTo(BigDecimal.ZERO) == 0 && surplus.compareTo(BigDecimal.ZERO) > 0) {
            messages.add("✅ 应急金已达标，月结余可全部用于投资或目标储蓄。");
        }

        result.put("messages", messages);
        return result;
    }
}
