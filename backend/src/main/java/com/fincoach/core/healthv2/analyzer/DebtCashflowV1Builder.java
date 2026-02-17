package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.advice.WarningCollector;
import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.entity.FcCashflowEntity;
import com.fincoach.core.healthv2.entity.FcLiabilityEntity;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DebtCashflowV1Builder {

    public DebtCashflowV1Result build(List<FcAssetEntity> assets,
                                      List<FcLiabilityEntity> liabilities,
                                      FcCashflowEntity cashflow,
                                      BigDecimal totalAssets,
                                      BigDecimal totalDebt,
                                      BigDecimal cashAssets,
                                      ScoreRuleSnapshot ruleSnapshot) {

        DebtCashflowV1Result result = new DebtCashflowV1Result();
        WarningCollector warnings = new WarningCollector();
        DebtCashflowThresholds thresholds = DebtCashflowThresholds.fromSnapshot(ruleSnapshot);
        warnings.addAll(thresholds.getWarnings());

        if (liabilities == null) {
            warnings.add(DebtCashflowWarningCodes.MISSING_DEBT_ITEMS, "liabilities null");
        }

        BigDecimal monthlyIncome = cashflow == null ? null : cashflow.getIncome();
        BigDecimal monthlyDebtPayment = cashflow == null ? null : cashflow.getMonthlyDebtPayment();
        BigDecimal monthlyExpense = calcMonthlyExpense(cashflow);
        if (cashflow == null || monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            warnings.add(DebtCashflowWarningCodes.MISSING_CASHFLOW_INCOME, "cashflow income missing");
        }
        if (monthlyExpense == null || monthlyExpense.compareTo(BigDecimal.ZERO) <= 0) {
            warnings.add(DebtCashflowWarningCodes.MISSING_CASHFLOW_EXPENSE, "cashflow expense missing");
        }
        if ((liabilities == null || liabilities.isEmpty()) && monthlyDebtPayment == null) {
            warnings.add(DebtCashflowWarningCodes.MISSING_DEBT_ITEMS, "liabilities empty and debt payment missing");
        }

        BigDecimal monthlySurplus = (monthlyIncome != null && monthlyExpense != null)
                ? monthlyIncome.subtract(monthlyExpense)
                : null;
        BigDecimal surplusRate = divide(monthlySurplus, monthlyIncome);
        BigDecimal dti = (monthlyIncome != null && monthlyIncome.compareTo(BigDecimal.ZERO) > 0 && monthlyDebtPayment != null)
                ? monthlyDebtPayment.divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                : null;
        BigDecimal debtToAssets = divide(totalDebt, totalAssets);

        BigDecimal liquidAssets = calcLiquidAssets(assets, cashAssets);
        if (liquidAssets == null || liquidAssets.compareTo(BigDecimal.ZERO) <= 0) {
            if (totalAssets != null && totalAssets.compareTo(BigDecimal.ZERO) > 0) {
                liquidAssets = totalAssets.multiply(BigDecimal.valueOf(thresholds.getLiquidAssetEstimateRatio()))
                        .setScale(2, RoundingMode.HALF_UP);
                warnings.add(DebtCashflowWarningCodes.EMERGENCY_FUND_ESTIMATED, "use totalAssets estimate");
            } else {
                warnings.add(DebtCashflowWarningCodes.MISSING_LIQUID_ASSETS, "liquid assets missing");
                warnings.add(DebtCashflowWarningCodes.MISSING_TOTAL_ASSETS, "total assets missing");
            }
        }

        BigDecimal emergencyFundMonths = null;
        if (monthlyExpense != null && monthlyExpense.compareTo(BigDecimal.ZERO) > 0 && liquidAssets != null) {
            emergencyFundMonths = liquidAssets.divide(monthlyExpense, 2, RoundingMode.HALF_UP);
        }

        String stressLevel = "LOW";
        List<String> stressReasons = new ArrayList<>();
        boolean hasHigh = false;
        boolean hasMed = false;

        if (dti != null) {
            double dtiVal = dti.doubleValue();
            if (dtiVal >= thresholds.getDtiHigh()) {
                hasHigh = true;
                stressReasons.add("DTI >= " + formatPercent(thresholds.getDtiHigh()));
            } else if (dtiVal >= thresholds.getDtiMed()) {
                hasMed = true;
                stressReasons.add("DTI >= " + formatPercent(thresholds.getDtiMed()));
            }
        }
        if (surplusRate != null) {
            double s = surplusRate.doubleValue();
            if (s < 0) {
                hasHigh = true;
                stressReasons.add("月度现金流为负");
            } else if (s < thresholds.getSurplusRateMin()) {
                hasMed = true;
                stressReasons.add("结余率 < " + formatPercent(thresholds.getSurplusRateMin()));
            }
        }
        if (emergencyFundMonths != null) {
            double m = emergencyFundMonths.doubleValue();
            if (m < thresholds.getEmergencyLow()) {
                hasHigh = true;
                stressReasons.add("应急金 < " + thresholds.getEmergencyLow() + "月");
            } else if (m < thresholds.getEmergencyOk()) {
                hasMed = true;
                stressReasons.add("应急金 < " + thresholds.getEmergencyOk() + "月");
            }
        }
        if (debtToAssets != null && debtToAssets.doubleValue() > thresholds.getDebtToAssetsHigh()) {
            hasMed = true;
            stressReasons.add("负债占比 > " + formatPercent(thresholds.getDebtToAssetsHigh()));
        }

        if (warnings.codes().contains(DebtCashflowWarningCodes.MISSING_CASHFLOW_INCOME)
                || warnings.codes().contains(DebtCashflowWarningCodes.MISSING_CASHFLOW_EXPENSE)) {
            hasMed = true;
            stressReasons.add("现金流数据不完整");
        }

        if (hasHigh) {
            stressLevel = "HIGH";
        } else if (hasMed) {
            stressLevel = "MED";
        }

        Map<String, Object> debtMetrics = new LinkedHashMap<>();
        debtMetrics.put("totalDebt", totalDebt);
        debtMetrics.put("monthlyDebtPayment", monthlyDebtPayment);
        debtMetrics.put("dti", scale(dti));
        debtMetrics.put("debtToAssets", scale(debtToAssets));
        debtMetrics.put("interestWeightedRate", scale(calcInterestRate(liabilities, totalDebt)));
        debtMetrics.put("warnings", warnings.codes());
        debtMetrics.put("explanations", buildDebtExplanations(dti, debtToAssets, thresholds));

        Map<String, Object> cashflowMetrics = new LinkedHashMap<>();
        cashflowMetrics.put("monthlyIncome", monthlyIncome);
        cashflowMetrics.put("monthlyExpense", monthlyExpense);
        cashflowMetrics.put("monthlySurplus", monthlySurplus);
        cashflowMetrics.put("surplusRate", scale(surplusRate));
        cashflowMetrics.put("emergencyFundMonths", scale(emergencyFundMonths));
        cashflowMetrics.put("warnings", warnings.codes());
        cashflowMetrics.put("explanations", buildCashflowExplanations(surplusRate, emergencyFundMonths, thresholds));

        Map<String, Object> combined = new LinkedHashMap<>();
        combined.put("stressLevel", stressLevel);
        combined.put("stressReasons", stressReasons);
        combined.put("warnings", warnings.codes());

        result.setDebtMetrics(debtMetrics);
        result.setCashflowMetrics(cashflowMetrics);
        result.setCombined(combined);
        result.setWarnings(warnings.codes());
        result.setWarningDetails(warnings.detailMaps());
        result.setDti(dti == null ? null : dti.doubleValue());
        result.setSurplusRate(surplusRate == null ? null : surplusRate.doubleValue());
        result.setEmergencyFundMonths(emergencyFundMonths == null ? null : emergencyFundMonths.doubleValue());
        result.setStressLevel(stressLevel);

        result.setAdvice(buildAdvice(result, thresholds));
        return result;
    }

    private Map<String, Object> buildAdvice(DebtCashflowV1Result result, DebtCashflowThresholds thresholds) {
        Map<String, Object> advice = new LinkedHashMap<>();
        List<Map<String, Object>> advices = new ArrayList<>();

        Double dti = result.getDti();
        if (dti != null && dti >= thresholds.getDtiHigh()) {
            advices.add(adviceItem(
                    "DEBT_DTI_HIGH", "降低负债压力",
                    "DTI=" + formatPercent(dti) + "，高于阈值" + formatPercent(thresholds.getDtiHigh())
                            + "，建议控制月供/优化债务/增收节支。",
                    "P0",
                    List.of("dti")));
        } else if (dti != null && dti >= thresholds.getDtiMed()) {
            advices.add(adviceItem(
                    "DEBT_DTI_ELEVATED", "负债压力偏高",
                    "DTI=" + formatPercent(dti) + "，接近阈值" + formatPercent(thresholds.getDtiHigh())
                            + "，建议控制负债增长。",
                    "P1",
                    List.of("dti")));
        }

        Double surplusRate = result.getSurplusRate();
        if (surplusRate != null && surplusRate < 0) {
            advices.add(adviceItem(
                    "CASHFLOW_NEGATIVE", "现金流为负，先止血",
                    "结余率=" + formatPercent(surplusRate) + "，现金流为负，优先缩减支出或增加收入。",
                    "P0",
                    List.of("surplusRate")));
        } else if (surplusRate != null && surplusRate < thresholds.getSurplusRateMin()) {
            advices.add(adviceItem(
                    "CASHFLOW_LOW_SURPLUS", "提升结余率",
                    "结余率=" + formatPercent(surplusRate) + "，低于阈值" + formatPercent(thresholds.getSurplusRateMin())
                            + "，建议优化支出结构。",
                    "P1",
                    List.of("surplusRate")));
        }

        Double emergencyMonths = result.getEmergencyFundMonths();
        if (emergencyMonths != null && emergencyMonths < thresholds.getEmergencyLow()) {
            advices.add(adviceItem(
                    "EMERGENCY_FUND_LOW", "优先建立应急金",
                    "应急金=" + formatDecimal(emergencyMonths) + "个月，低于" + formatDecimal(thresholds.getEmergencyLow())
                            + "个月，建议优先补足储备。",
                    "P0",
                    List.of("emergencyFundMonths")));
        } else if (emergencyMonths != null && emergencyMonths < thresholds.getEmergencyOk()) {
            advices.add(adviceItem(
                    "EMERGENCY_FUND_MED", "提高应急金覆盖",
                    "应急金=" + formatDecimal(emergencyMonths) + "个月，低于" + formatDecimal(thresholds.getEmergencyOk())
                            + "个月，建议逐步提高。",
                    "P1",
                    List.of("emergencyFundMonths")));
        }

        BigDecimal debtToAssets = scaleToBigDecimal(result.getDebtMetrics().get("debtToAssets"));
        if (debtToAssets != null && debtToAssets.doubleValue() > thresholds.getDebtToAssetsHigh()) {
            advices.add(adviceItem(
                    "DEBT_TO_ASSETS_HIGH", "限制新增负债",
                    "负债占比=" + formatPercent(debtToAssets.doubleValue()) + "，高于阈值"
                            + formatPercent(thresholds.getDebtToAssetsHigh()) + "，建议限制新增负债并提升净资产。",
                    "P1",
                    List.of("debtToAssets")));
        }

        advice.put("summaryLevel", result.getStressLevel());
        advice.put("advices", advices);
        advice.put("warnings", result.getWarnings());
        return advice;
    }

    private Map<String, Object> adviceItem(String code,
                                           String title,
                                           String detail,
                                           String priority,
                                           List<String> relatedMetrics) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("title", title);
        item.put("detail", detail);
        item.put("priority", priority);
        item.put("relatedMetrics", relatedMetrics == null ? new ArrayList<>() : relatedMetrics);
        return item;
    }

    private List<Map<String, Object>> buildDebtExplanations(BigDecimal dti,
                                                            BigDecimal debtToAssets,
                                                            DebtCashflowThresholds thresholds) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(explanation(
                "DTI", dti,
                "HIGH>=" + formatPercent(thresholds.getDtiHigh()) + ", MED>=" + formatPercent(thresholds.getDtiMed()),
                dti == null ? "DTI缺失" : "DTI=" + formatPercent(dti.doubleValue())
        ));
        items.add(explanation(
                "DEBT_TO_ASSETS", debtToAssets,
                "HIGH>" + formatPercent(thresholds.getDebtToAssetsHigh()),
                debtToAssets == null ? "负债占比缺失" : "负债占比=" + formatPercent(debtToAssets.doubleValue())
        ));
        return items;
    }

    private List<Map<String, Object>> buildCashflowExplanations(BigDecimal surplusRate,
                                                                BigDecimal emergencyFundMonths,
                                                                DebtCashflowThresholds thresholds) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(explanation(
                "SURPLUS_RATE", surplusRate,
                "MIN>=" + formatPercent(thresholds.getSurplusRateMin()),
                surplusRate == null ? "结余率缺失" : "结余率=" + formatPercent(surplusRate.doubleValue())
        ));
        items.add(explanation(
                "EMERGENCY_MONTHS", emergencyFundMonths,
                "LOW<" + formatDecimal(thresholds.getEmergencyLow()) + ", OK>=" + formatDecimal(thresholds.getEmergencyOk()),
                emergencyFundMonths == null ? "应急金月数缺失" : "应急金=" + formatDecimal(emergencyFundMonths.doubleValue()) + "个月"
        ));
        return items;
    }

    private Map<String, Object> explanation(String code, Object rawValue, String threshold, String detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("rawValue", rawValue == null ? null : rawValue);
        map.put("threshold", threshold);
        map.put("detail", detail);
        return map;
    }

    private BigDecimal calcMonthlyExpense(FcCashflowEntity cashflow) {
        if (cashflow == null) return null;
        BigDecimal total = BigDecimal.ZERO;
        boolean has = false;
        if (cashflow.getFixedExpense() != null) {
            total = total.add(cashflow.getFixedExpense());
            has = true;
        }
        if (cashflow.getVariableExpense() != null) {
            total = total.add(cashflow.getVariableExpense());
            has = true;
        }
        if (cashflow.getMonthlyDebtPayment() != null) {
            total = total.add(cashflow.getMonthlyDebtPayment());
            has = true;
        }
        return has ? total : null;
    }

    private BigDecimal calcLiquidAssets(List<FcAssetEntity> assets, BigDecimal cashAssets) {
        if (cashAssets != null) return cashAssets;
        if (assets == null || assets.isEmpty()) return null;
        BigDecimal sum = BigDecimal.ZERO;
        boolean has = false;
        for (FcAssetEntity asset : assets) {
            if (asset == null) continue;
            if ("CASH".equals(asset.getType()) && asset.getAmount() != null) {
                sum = sum.add(asset.getAmount());
                has = true;
            }
        }
        return has ? sum : null;
    }

    private BigDecimal calcInterestRate(List<FcLiabilityEntity> liabilities, BigDecimal totalDebt) {
        if (liabilities == null || liabilities.isEmpty() || totalDebt == null || totalDebt.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (FcLiabilityEntity l : liabilities) {
            if (l.getInterestRate() != null && l.getPrincipal() != null) {
                weightedSum = weightedSum.add(l.getInterestRate().multiply(l.getPrincipal()));
            }
        }
        return weightedSum.divide(totalDebt, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null) return null;
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) return null;
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) return null;
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleToBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        return null;
    }

    private String formatPercent(double value) {
        return BigDecimal.valueOf(value * 100).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private String formatDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
