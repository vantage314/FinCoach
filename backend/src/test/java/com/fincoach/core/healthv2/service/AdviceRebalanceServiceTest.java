package com.fincoach.core.healthv2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleDefaults;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleParamValue;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleSnapshot;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleValueType;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleRegistry;
import com.fincoach.core.healthv2.dto.AdviceRebalanceResponseDTO;
import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.entity.FcCashflowMonthEntity;
import com.fincoach.core.healthv2.entity.FcDebtEntity;
import com.fincoach.core.healthv2.mapper.FcAssetMapper;
import com.fincoach.core.healthv2.mapper.FcCashflowMonthMapper;
import com.fincoach.core.healthv2.mapper.FcDebtMapper;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateRegistry;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateSnapshot;
import com.fincoach.core.healthv2.service.impl.AdviceRebalanceServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

public class AdviceRebalanceServiceTest {

    @Test
    public void testRebalanceSuggestionsGenerated() {
        FcAssetMapper assetMapper = Mockito.mock(FcAssetMapper.class);
        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);
        BehaviorEventService behaviorEventService = Mockito.mock(BehaviorEventService.class);
        AdviceRuleRegistry ruleRegistry = Mockito.mock(AdviceRuleRegistry.class);
        RebalanceTemplateRegistry templateRegistry = Mockito.mock(RebalanceTemplateRegistry.class);
        FcDebtMapper debtMapper = Mockito.mock(FcDebtMapper.class);
        FcCashflowMonthMapper cashflowMonthMapper = Mockito.mock(FcCashflowMonthMapper.class);

        List<FcAssetEntity> assets = List.of(asset("STOCK", 80), asset("BOND", 20));
        Mockito.when(assetMapper.selectList(any())).thenReturn(assets);
        Mockito.when(reportMapper.selectOne(any())).thenReturn(null);
        Mockito.when(behaviorEventService.countByType(anyLong(), anyInt())).thenReturn(Map.of());
        Mockito.when(debtMapper.selectList(any())).thenReturn(List.of());
        Mockito.when(cashflowMonthMapper.selectList(any())).thenReturn(List.of());

        AdviceRuleSnapshot snapshot = snapshotWith(
                Map.of(
                        AdviceRuleDefaults.REBALANCE_DRIFT_PCT, "0.05",
                        AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON, "{\"tradeFreq\":0.4,\"concentration\":0.3,\"cashDrag\":0.3}",
                        AdviceRuleDefaults.MAX_POSITIONS, "8",
                        AdviceRuleDefaults.RISK_SCORE_LOW, "30",
                        AdviceRuleDefaults.RISK_SCORE_MID, "60",
                        AdviceRuleDefaults.RISK_SCORE_HIGH, "80"
                )
        );
        Mockito.when(ruleRegistry.get()).thenReturn(snapshot);

        Map<String, Double> targets = new LinkedHashMap<>();
        targets.put("STOCK", 0.5);
        targets.put("BOND", 0.5);
        RebalanceTemplateSnapshot templateSnapshot = new RebalanceTemplateSnapshot(1L, "BALANCED", 1,
                RebalanceTemplateSnapshot.SOURCE_DB_ACTIVE, targets, List.of());
        Mockito.when(templateRegistry.getActive()).thenReturn(templateSnapshot);

        AdviceRebalanceServiceImpl service = new AdviceRebalanceServiceImpl(
                assetMapper, reportMapper, behaviorEventService, ruleRegistry, templateRegistry, new ObjectMapper(),
                debtMapper, cashflowMonthMapper);

        AdviceRebalanceResponseDTO dto = service.buildRebalanceAdvice(1L);
        assertNotNull(dto);
        assertNotNull(dto.getRebalanceSuggestions());
        assertTrue(dto.getRebalanceSuggestions().stream()
                .anyMatch(s -> "STOCK".equals(s.getAssetType()) && "SELL".equals(s.getAction())));
    }

    @Test
    public void testBehaviorScoreAffectedByWeights() {
        FcAssetMapper assetMapper = Mockito.mock(FcAssetMapper.class);
        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);
        BehaviorEventService behaviorEventService = Mockito.mock(BehaviorEventService.class);
        AdviceRuleRegistry ruleRegistry = Mockito.mock(AdviceRuleRegistry.class);
        RebalanceTemplateRegistry templateRegistry = Mockito.mock(RebalanceTemplateRegistry.class);
        FcDebtMapper debtMapper = Mockito.mock(FcDebtMapper.class);
        FcCashflowMonthMapper cashflowMonthMapper = Mockito.mock(FcCashflowMonthMapper.class);

        List<FcAssetEntity> assets = List.of(asset("CASH", 60), asset("STOCK", 40));
        Mockito.when(assetMapper.selectList(any())).thenReturn(assets);
        Mockito.when(reportMapper.selectOne(any())).thenReturn(null);
        Mockito.when(behaviorEventService.countByType(anyLong(), anyInt())).thenReturn(Map.of());
        Mockito.when(templateRegistry.getActive()).thenReturn(null);
        Mockito.when(debtMapper.selectList(any())).thenReturn(List.of());
        Mockito.when(cashflowMonthMapper.selectList(any())).thenReturn(List.of());

        AdviceRuleSnapshot cashHeavy = snapshotWith(
                Map.of(
                        AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON, "{\"tradeFreq\":0.0,\"concentration\":0.0,\"cashDrag\":1.0}",
                        AdviceRuleDefaults.REBALANCE_DRIFT_PCT, "0.05",
                        AdviceRuleDefaults.MAX_POSITIONS, "8",
                        AdviceRuleDefaults.RISK_SCORE_LOW, "30",
                        AdviceRuleDefaults.RISK_SCORE_MID, "60",
                        AdviceRuleDefaults.RISK_SCORE_HIGH, "80"
                )
        );
        AdviceRuleSnapshot tradeHeavy = snapshotWith(
                Map.of(
                        AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON, "{\"tradeFreq\":1.0,\"concentration\":0.0,\"cashDrag\":0.0}",
                        AdviceRuleDefaults.REBALANCE_DRIFT_PCT, "0.05",
                        AdviceRuleDefaults.MAX_POSITIONS, "8",
                        AdviceRuleDefaults.RISK_SCORE_LOW, "30",
                        AdviceRuleDefaults.RISK_SCORE_MID, "60",
                        AdviceRuleDefaults.RISK_SCORE_HIGH, "80"
                )
        );
        Mockito.when(ruleRegistry.get()).thenReturn(cashHeavy, tradeHeavy);

        AdviceRebalanceServiceImpl service = new AdviceRebalanceServiceImpl(
                assetMapper, reportMapper, behaviorEventService, ruleRegistry, templateRegistry, new ObjectMapper(),
                debtMapper, cashflowMonthMapper);

        AdviceRebalanceResponseDTO dto1 = service.buildRebalanceAdvice(1L);
        AdviceRebalanceResponseDTO dto2 = service.buildRebalanceAdvice(1L);

        assertTrue(dto1.getBehaviorScore() < dto2.getBehaviorScore());
    }

    @Test
    public void testPayloadNonNullFieldsAndWarnings() {
        FcAssetMapper assetMapper = Mockito.mock(FcAssetMapper.class);
        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);
        BehaviorEventService behaviorEventService = Mockito.mock(BehaviorEventService.class);
        AdviceRuleRegistry ruleRegistry = Mockito.mock(AdviceRuleRegistry.class);
        RebalanceTemplateRegistry templateRegistry = Mockito.mock(RebalanceTemplateRegistry.class);
        FcDebtMapper debtMapper = Mockito.mock(FcDebtMapper.class);
        FcCashflowMonthMapper cashflowMonthMapper = Mockito.mock(FcCashflowMonthMapper.class);

        Mockito.when(assetMapper.selectList(any())).thenReturn(List.of());
        Mockito.when(reportMapper.selectOne(any())).thenReturn(null);
        Mockito.when(behaviorEventService.countByType(anyLong(), anyInt())).thenReturn(Map.of());
        Mockito.when(ruleRegistry.get()).thenReturn(null);
        Mockito.when(templateRegistry.getActive()).thenReturn(null);
        Mockito.when(debtMapper.selectList(any())).thenReturn(List.of());
        Mockito.when(cashflowMonthMapper.selectList(any())).thenReturn(List.of());

        AdviceRebalanceServiceImpl service = new AdviceRebalanceServiceImpl(
                assetMapper, reportMapper, behaviorEventService, ruleRegistry, templateRegistry, new ObjectMapper(),
                debtMapper, cashflowMonthMapper);

        AdviceRebalanceResponseDTO dto = service.buildRebalanceAdvice(1L);
        assertNotNull(dto.getRiskScore());
        assertNotNull(dto.getHealthScore());
        assertNotNull(dto.getBehaviorScore());
        assertNotNull(dto.getRebalanceSuggestions());
        assertNotNull(dto.getDebtSuggestions());
        assertNotNull(dto.getCashflowSuggestions());
        assertNotNull(dto.getMeta());
        Object warnings = dto.getMeta().get("warnings");
        assertTrue(warnings instanceof List);
        assertTrue(((List<?>) warnings).size() > 0);
    }

    @Test
    public void testDebtSuggestionsIncludeDtiAndEmergencyFund() {
        FcAssetMapper assetMapper = Mockito.mock(FcAssetMapper.class);
        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);
        BehaviorEventService behaviorEventService = Mockito.mock(BehaviorEventService.class);
        AdviceRuleRegistry ruleRegistry = Mockito.mock(AdviceRuleRegistry.class);
        RebalanceTemplateRegistry templateRegistry = Mockito.mock(RebalanceTemplateRegistry.class);
        FcDebtMapper debtMapper = Mockito.mock(FcDebtMapper.class);
        FcCashflowMonthMapper cashflowMonthMapper = Mockito.mock(FcCashflowMonthMapper.class);

        Mockito.when(assetMapper.selectList(any())).thenReturn(List.of(asset("CASH", 100), asset("STOCK", 900)));
        Mockito.when(reportMapper.selectOne(any())).thenReturn(null);
        Mockito.when(behaviorEventService.countByType(anyLong(), anyInt())).thenReturn(Map.of());
        Mockito.when(templateRegistry.getActive()).thenReturn(null);
        Mockito.when(debtMapper.selectList(any())).thenReturn(List.of(debt(1L, "CREDITCARD", 0.20, 600)));
        Mockito.when(cashflowMonthMapper.selectList(any())).thenReturn(List.of(
                cashflow("2025-01", 1000, 500),
                cashflow("2025-02", 1000, 500),
                cashflow("2025-03", 1000, 500)
        ));

        AdviceRuleSnapshot snapshot = snapshotWith(
                Map.ofEntries(
                        Map.entry(AdviceRuleDefaults.REBALANCE_DRIFT_PCT, "0.05"),
                        Map.entry(AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON, "{\"tradeFreq\":0.4,\"concentration\":0.3,\"cashDrag\":0.3}"),
                        Map.entry(AdviceRuleDefaults.MAX_POSITIONS, "8"),
                        Map.entry(AdviceRuleDefaults.RISK_SCORE_LOW, "30"),
                        Map.entry(AdviceRuleDefaults.RISK_SCORE_MID, "60"),
                        Map.entry(AdviceRuleDefaults.RISK_SCORE_HIGH, "80"),
                        Map.entry(AdviceRuleDefaults.EMERGENCY_FUND_MONTHS_TARGET, "3"),
                        Map.entry(AdviceRuleDefaults.DTI_WARN, "0.35"),
                        Map.entry(AdviceRuleDefaults.DTI_DANGER, "0.50"),
                        Map.entry(AdviceRuleDefaults.DEBT_STRATEGY, "AVALANCHE"),
                        Map.entry(AdviceRuleDefaults.MIN_NET_FOR_EXTRA_DEBT_PAYMENT, "0")
                )
        );
        Mockito.when(ruleRegistry.get()).thenReturn(snapshot);

        AdviceRebalanceServiceImpl service = new AdviceRebalanceServiceImpl(
                assetMapper, reportMapper, behaviorEventService, ruleRegistry, templateRegistry, new ObjectMapper(),
                debtMapper, cashflowMonthMapper);

        AdviceRebalanceResponseDTO dto = service.buildRebalanceAdvice(1L);
        assertTrue(dto.getDebtSuggestions().stream().anyMatch(s -> "DTI_RISK".equals(s.getType())));
        assertTrue(dto.getDebtSuggestions().stream().anyMatch(s -> "EMERGENCY_FUND".equals(s.getType())));
    }

    @Test
    public void testNegativeCashflowAddsCashflowStabilize() {
        FcAssetMapper assetMapper = Mockito.mock(FcAssetMapper.class);
        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);
        BehaviorEventService behaviorEventService = Mockito.mock(BehaviorEventService.class);
        AdviceRuleRegistry ruleRegistry = Mockito.mock(AdviceRuleRegistry.class);
        RebalanceTemplateRegistry templateRegistry = Mockito.mock(RebalanceTemplateRegistry.class);
        FcDebtMapper debtMapper = Mockito.mock(FcDebtMapper.class);
        FcCashflowMonthMapper cashflowMonthMapper = Mockito.mock(FcCashflowMonthMapper.class);

        Mockito.when(assetMapper.selectList(any())).thenReturn(List.of(asset("CASH", 100)));
        Mockito.when(reportMapper.selectOne(any())).thenReturn(null);
        Mockito.when(behaviorEventService.countByType(anyLong(), anyInt())).thenReturn(Map.of());
        Mockito.when(templateRegistry.getActive()).thenReturn(null);
        Mockito.when(debtMapper.selectList(any())).thenReturn(List.of(debt(2L, "MORTGAGE", 0.04, 300)));
        Mockito.when(cashflowMonthMapper.selectList(any())).thenReturn(List.of(
                cashflow("2025-01", 1000, 1200),
                cashflow("2025-02", 1000, 1200),
                cashflow("2025-03", 1000, 1200)
        ));

        Mockito.when(ruleRegistry.get()).thenReturn(snapshotWith(Map.of(
                AdviceRuleDefaults.REBALANCE_DRIFT_PCT, "0.05",
                AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON, "{\"tradeFreq\":0.4,\"concentration\":0.3,\"cashDrag\":0.3}",
                AdviceRuleDefaults.MAX_POSITIONS, "8",
                AdviceRuleDefaults.RISK_SCORE_LOW, "30",
                AdviceRuleDefaults.RISK_SCORE_MID, "60",
                AdviceRuleDefaults.RISK_SCORE_HIGH, "80"
        )));

        AdviceRebalanceServiceImpl service = new AdviceRebalanceServiceImpl(
                assetMapper, reportMapper, behaviorEventService, ruleRegistry, templateRegistry, new ObjectMapper(),
                debtMapper, cashflowMonthMapper);

        AdviceRebalanceResponseDTO dto = service.buildRebalanceAdvice(1L);
        assertTrue(dto.getCashflowSuggestions().stream().anyMatch(s -> "CASHFLOW_STABILIZE".equals(s.getType())));
        assertTrue(dto.getDebtSuggestions().stream().anyMatch(s -> "CASHFLOW_STABILIZE".equals(s.getType())));
    }

    @Test
    public void testThresholdsReadFromRegistry() {
        FcAssetMapper assetMapper = Mockito.mock(FcAssetMapper.class);
        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);
        BehaviorEventService behaviorEventService = Mockito.mock(BehaviorEventService.class);
        AdviceRuleRegistry ruleRegistry = Mockito.mock(AdviceRuleRegistry.class);
        RebalanceTemplateRegistry templateRegistry = Mockito.mock(RebalanceTemplateRegistry.class);
        FcDebtMapper debtMapper = Mockito.mock(FcDebtMapper.class);
        FcCashflowMonthMapper cashflowMonthMapper = Mockito.mock(FcCashflowMonthMapper.class);

        Mockito.when(assetMapper.selectList(any())).thenReturn(List.of(asset("CASH", 500)));
        Mockito.when(reportMapper.selectOne(any())).thenReturn(null);
        Mockito.when(behaviorEventService.countByType(anyLong(), anyInt())).thenReturn(Map.of());
        Mockito.when(templateRegistry.getActive()).thenReturn(null);
        Mockito.when(debtMapper.selectList(any())).thenReturn(List.of(debt(3L, "CONSUMER", 0.12, 250)));
        Mockito.when(cashflowMonthMapper.selectList(any())).thenReturn(List.of(
                cashflow("2025-01", 1000, 500),
                cashflow("2025-02", 1000, 500),
                cashflow("2025-03", 1000, 500)
        ));

        AdviceRuleSnapshot snapshot = snapshotWith(
                Map.of(
                        AdviceRuleDefaults.REBALANCE_DRIFT_PCT, "0.05",
                        AdviceRuleDefaults.BEHAVIOR_SCORE_WEIGHTS_JSON, "{\"tradeFreq\":0.4,\"concentration\":0.3,\"cashDrag\":0.3}",
                        AdviceRuleDefaults.MAX_POSITIONS, "8",
                        AdviceRuleDefaults.RISK_SCORE_LOW, "30",
                        AdviceRuleDefaults.RISK_SCORE_MID, "60",
                        AdviceRuleDefaults.RISK_SCORE_HIGH, "80",
                        AdviceRuleDefaults.DTI_WARN, "0.20",
                        AdviceRuleDefaults.DTI_DANGER, "0.40"
                )
        );
        Mockito.when(ruleRegistry.get()).thenReturn(snapshot);

        AdviceRebalanceServiceImpl service = new AdviceRebalanceServiceImpl(
                assetMapper, reportMapper, behaviorEventService, ruleRegistry, templateRegistry, new ObjectMapper(),
                debtMapper, cashflowMonthMapper);

        AdviceRebalanceResponseDTO dto = service.buildRebalanceAdvice(1L);
        assertTrue(dto.getDebtSuggestions().stream().anyMatch(s -> "DTI_RISK".equals(s.getType())));
    }

    private AdviceRuleSnapshot snapshotWith(Map<String, String> values) {
        Map<String, com.fincoach.core.healthv2.advice.rules.AdviceRuleParamValue> params = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : values.entrySet()) {
            AdviceRuleValueType type = AdviceRuleValueType.JSON;
            if (AdviceRuleDefaults.REBALANCE_DRIFT_PCT.equals(e.getKey())) {
                type = AdviceRuleValueType.DECIMAL;
            } else if (AdviceRuleDefaults.MAX_POSITIONS.equals(e.getKey())
                    || AdviceRuleDefaults.RISK_SCORE_LOW.equals(e.getKey())
                    || AdviceRuleDefaults.RISK_SCORE_MID.equals(e.getKey())
                    || AdviceRuleDefaults.RISK_SCORE_HIGH.equals(e.getKey())
                    || AdviceRuleDefaults.EMERGENCY_FUND_MONTHS_TARGET.equals(e.getKey())) {
                type = AdviceRuleValueType.INT;
            } else if (AdviceRuleDefaults.DTI_WARN.equals(e.getKey())
                    || AdviceRuleDefaults.DTI_DANGER.equals(e.getKey())
                    || AdviceRuleDefaults.MIN_NET_FOR_EXTRA_DEBT_PAYMENT.equals(e.getKey())) {
                type = AdviceRuleValueType.DECIMAL;
            } else if (AdviceRuleDefaults.DEBT_STRATEGY.equals(e.getKey())) {
                type = AdviceRuleValueType.STRING;
            }
            AdviceRuleParamValue value = AdviceRuleParamValue.parse(e.getKey(), type, e.getValue(), "TEST");
            params.put(e.getKey(), value);
        }
        return new AdviceRuleSnapshot(1L, AdviceRuleDefaults.DEFAULT_CODE, 1,
                AdviceRuleSnapshot.SOURCE_DB_ACTIVE, params, List.of(), List.of());
    }

    private FcAssetEntity asset(String type, double amount) {
        FcAssetEntity entity = new FcAssetEntity();
        entity.setType(type);
        entity.setAmount(BigDecimal.valueOf(amount));
        return entity;
    }

    private FcDebtEntity debt(Long id, String type, double apr, double monthlyPayment) {
        FcDebtEntity debt = new FcDebtEntity();
        debt.setId(id);
        debt.setDebtType(type);
        debt.setApr(BigDecimal.valueOf(apr));
        debt.setMonthlyPayment(BigDecimal.valueOf(monthlyPayment));
        debt.setIsActive(1);
        return debt;
    }

    private FcCashflowMonthEntity cashflow(String month, double income, double expense) {
        FcCashflowMonthEntity entity = new FcCashflowMonthEntity();
        entity.setMonth(month);
        entity.setIncome(BigDecimal.valueOf(income));
        entity.setExpense(BigDecimal.valueOf(expense));
        entity.setNet(BigDecimal.valueOf(income - expense));
        return entity;
    }
}
