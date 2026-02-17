package com.fincoach.core.healthv2.debug;

import com.fincoach.core.healthv2.advice.AdviceThresholds;
import com.fincoach.core.healthv2.dto.admin.AdminMarketDebugLatestDTO;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateRegistry;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateSnapshot;
import com.fincoach.core.healthv2.rules.ScoreRuleSetRegistry;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class AdminMarketDebugMapper {

    @Autowired(required = false)
    private ScoreRuleSetRegistry scoreRuleSetRegistry;
    @Autowired(required = false)
    private RebalanceTemplateRegistry rebalanceTemplateRegistry;

    public AdminMarketDebugLatestDTO toDto(PortfolioMarketDebugSnapshot snapshot, Long userId, boolean includeMatrix) {
        AdminMarketDebugLatestDTO dto = new AdminMarketDebugLatestDTO();
        dto.setRequestId(resolveRequestId());
        dto.setTimestamp(System.currentTimeMillis());
        dto.setUserId(userId);
        dto.setEnabled(snapshot != null);
        dto.setWarnings(new ArrayList<>());
        dto.setResolvedTickers(new ArrayList<>());

        attachRuleSet(dto);
        attachAdviceMeta(dto);

        if (snapshot == null) {
            dto.getWarnings().add("DEBUG_SNAPSHOT_EMPTY");
            return dto;
        }

        List<String> warnings = safeList(snapshot.getWarnings());
        dto.setWarnings(new ArrayList<>(warnings));

        dto.setResolvedTickers(mapResolvedTickers(snapshot.getResolvedTickers()));
        dto.setHistory(mapHistory(snapshot));
        dto.setCache(mapCache(snapshot.getCache()));
        dto.setMarketFetch(mapMarketFetch(snapshot.getMarketFetch()));
        dto.setFallback(mapFallback(snapshot.getFallback()));
        dto.setCorrelation(mapCorrelation(snapshot.getCorrelation()));
        dto.setCorrelationMatrixSummary(mapCorrelationMatrixSummary(snapshot.getCorrelationMatrixSummary()));
        if (includeMatrix) {
            dto.setCorrelationMatrix(mapCorrelationMatrix(snapshot.getCorrelationMatrix()));
        }
        dto.setScoreSummary(mapScoreSummary(snapshot.getScoreSummary()));
        dto.setDebtCashflowSummary(mapDebtCashflowSummary(snapshot.getDebtCashflowSummary()));
        dto.setDebtOptimizerSummary(mapDebtOptimizerSummary(snapshot.getDebtOptimizerSummary()));
        dto.setInsuranceGapSummary(mapInsuranceGapSummary(snapshot.getInsuranceGapSummary()));

        return dto;
    }

    private void attachRuleSet(AdminMarketDebugLatestDTO dto) {
        if (scoreRuleSetRegistry == null || dto == null) return;
        ScoreRuleSnapshot snapshot = scoreRuleSetRegistry.get();
        if (snapshot == null) return;
        dto.setRuleSetCode(snapshot.getCode());
        dto.setRuleSetVersion(snapshot.getVersion());
        dto.setRuleSetSource(snapshot.getSource());
        dto.setRuleSetMissingParams(new ArrayList<>(safeList(snapshot.getMissingParams())));
        dto.setRuleSetWarnings(new ArrayList<>(safeList(snapshot.getWarnings())));
    }

    private void attachAdviceMeta(AdminMarketDebugLatestDTO dto) {
        if (dto == null) return;
        ScoreRuleSnapshot scoreSnapshot = scoreRuleSetRegistry == null ? null : scoreRuleSetRegistry.get();
        AdviceThresholds thresholds = AdviceThresholds.fromSnapshot(scoreSnapshot);
        dto.setAdviceThresholds(thresholds.toMap());
        dto.setAdviceWarnings(new ArrayList<>(safeList(thresholds.getWarnings())));

        if (rebalanceTemplateRegistry == null) return;
        RebalanceTemplateSnapshot templateSnapshot = rebalanceTemplateRegistry.getActive();
        if (templateSnapshot == null) return;
        dto.setRebalanceTemplateCode(templateSnapshot.getCode());
        dto.setRebalanceTemplateVersion(templateSnapshot.getVersion());
        dto.setRebalanceTemplateSource(templateSnapshot.getSource());
        if (templateSnapshot.getWarnings() != null) {
            List<String> merged = new ArrayList<>(safeList(dto.getAdviceWarnings()));
            for (String w : templateSnapshot.getWarnings()) {
                if (!merged.contains(w)) merged.add(w);
            }
            dto.setAdviceWarnings(merged);
        }
    }

    private String resolveRequestId() {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }

    private List<AdminMarketDebugLatestDTO.ResolvedTickerDTO> mapResolvedTickers(
            List<PortfolioMarketDebugSnapshot.ResolvedTickerDebug> resolvedTickers) {
        List<AdminMarketDebugLatestDTO.ResolvedTickerDTO> result = new ArrayList<>();
        if (resolvedTickers == null) {
            return result;
        }
        for (PortfolioMarketDebugSnapshot.ResolvedTickerDebug item : resolvedTickers) {
            AdminMarketDebugLatestDTO.ResolvedTickerDTO dto = new AdminMarketDebugLatestDTO.ResolvedTickerDTO();
            dto.setKeyword(item.getInput());
            dto.setNormalizedKey(item.getNormalizedKey());
            dto.setTicker(item.getResolvedTicker());
            dto.setSource(item.getMappingSource());
            dto.setPriority(null);
            dto.setWarnings(new ArrayList<>(safeList(item.getWarnings())));
            result.add(dto);
        }
        return result;
    }

    private AdminMarketDebugLatestDTO.HistoryDTO mapHistory(PortfolioMarketDebugSnapshot snapshot) {
        AdminMarketDebugLatestDTO.HistoryDTO dto = new AdminMarketDebugLatestDTO.HistoryDTO();
        dto.setSource(snapshot.getHistorySource());
        dto.setPath(new ArrayList<>(safeList(snapshot.getHistoryPath())));
        dto.setPoints(0);
        dto.setReturnsPoints(0);
        return dto;
    }

    private AdminMarketDebugLatestDTO.CacheDTO mapCache(PortfolioMarketDebugSnapshot.CacheDebug cache) {
        AdminMarketDebugLatestDTO.CacheDTO dto = new AdminMarketDebugLatestDTO.CacheDTO();
        if (cache == null) {
            return dto;
        }
        dto.setPreferCache(cache.isPreferCache());
        dto.setHit(cache.isHit());
        dto.setMissReason(cache.getMissReason());
        dto.setSnapshotsCount(cache.getSnapshotsCount());
        dto.setLookbackDays(cache.getLookbackDays());
        dto.setEnabled(true);
        return dto;
    }

    private AdminMarketDebugLatestDTO.MarketFetchDTO mapMarketFetch(PortfolioMarketDebugSnapshot.MarketFetchDebug marketFetch) {
        AdminMarketDebugLatestDTO.MarketFetchDTO dto = new AdminMarketDebugLatestDTO.MarketFetchDTO();
        if (marketFetch == null) {
            return dto;
        }
        dto.setProvider(marketFetch.getProvider());
        dto.setFailedSymbols(new ArrayList<>(safeList(marketFetch.getFailedSymbols())));
        dto.setErrorSummary(marketFetch.getNotes());
        return dto;
    }

    private AdminMarketDebugLatestDTO.FallbackDTO mapFallback(PortfolioMarketDebugSnapshot.FallbackDebug fallback) {
        AdminMarketDebugLatestDTO.FallbackDTO dto = new AdminMarketDebugLatestDTO.FallbackDTO();
        if (fallback == null) {
            return dto;
        }
        dto.setUsed(fallback.isFallback());
        dto.setWhy(fallback.getReason());
        dto.setReportLookbackDays(null);
        return dto;
    }

    private AdminMarketDebugLatestDTO.CorrelationDTO mapCorrelation(PortfolioMarketDebugSnapshot.CorrelationDebug correlation) {
        AdminMarketDebugLatestDTO.CorrelationDTO dto = new AdminMarketDebugLatestDTO.CorrelationDTO();
        if (correlation == null) {
            return dto;
        }
        dto.setAlignmentMode(correlation.getAlignedMode());
        dto.setEffectivePoints(correlation.getEffectivePoints());
        dto.setMinPoints(correlation.getMinPoints());
        dto.setMatrixEmitted(correlation.isMatrixEmitted());
        dto.setMaxCandidatePoints(correlation.getMaxCandidatePoints());
        dto.setGapRatio(correlation.getGapRatio());
        return dto;
    }

    private AdminMarketDebugLatestDTO.CorrelationMatrixSummaryDTO mapCorrelationMatrixSummary(
            PortfolioMarketDebugSnapshot.CorrelationMatrixSummary summary) {
        AdminMarketDebugLatestDTO.CorrelationMatrixSummaryDTO dto = new AdminMarketDebugLatestDTO.CorrelationMatrixSummaryDTO();
        if (summary == null) {
            return dto;
        }
        dto.setAssetsCount(summary.getAssetsCount());
        dto.setSampleSize(summary.getSampleSize());
        dto.setWarnings(new ArrayList<>(safeList(summary.getWarnings())));
        return dto;
    }

    private AdminMarketDebugLatestDTO.CorrelationMatrixDTO mapCorrelationMatrix(
            PortfolioMarketDebugSnapshot.CorrelationMatrixData data) {
        AdminMarketDebugLatestDTO.CorrelationMatrixDTO dto = new AdminMarketDebugLatestDTO.CorrelationMatrixDTO();
        if (data == null) {
            return dto;
        }
        dto.setAssets(new ArrayList<>(safeList(data.getAssets())));
        dto.setMatrix(data.getMatrix());
        dto.setMethod(data.getMethod());
        dto.setSampleSize(data.getSampleSize());
        dto.setStartDate(data.getStartDate());
        dto.setEndDate(data.getEndDate());
        return dto;
    }

    private AdminMarketDebugLatestDTO.ScoreSummaryDTO mapScoreSummary(PortfolioMarketDebugSnapshot.ScoreSummary summary) {
        AdminMarketDebugLatestDTO.ScoreSummaryDTO dto = new AdminMarketDebugLatestDTO.ScoreSummaryDTO();
        if (summary == null) {
            return dto;
        }
        dto.setRisk(mapScoreSummaryItem(summary.getRisk()));
        dto.setAssetHealth(mapScoreSummaryItem(summary.getAssetHealth()));
        dto.setBehavior(mapScoreSummaryItem(summary.getBehavior()));
        return dto;
    }

    private AdminMarketDebugLatestDTO.ScoreSummaryItemDTO mapScoreSummaryItem(
            PortfolioMarketDebugSnapshot.ScoreSummaryItem item) {
        AdminMarketDebugLatestDTO.ScoreSummaryItemDTO dto = new AdminMarketDebugLatestDTO.ScoreSummaryItemDTO();
        if (item == null) {
            return dto;
        }
        dto.setValue(item.getValue());
        dto.setLevel(item.getLevel());
        dto.setWarningsCount(item.getWarnings() == null ? 0 : item.getWarnings().size());
        return dto;
    }

    private AdminMarketDebugLatestDTO.DebtCashflowSummaryDTO mapDebtCashflowSummary(
            PortfolioMarketDebugSnapshot.DebtCashflowSummary summary) {
        AdminMarketDebugLatestDTO.DebtCashflowSummaryDTO dto = new AdminMarketDebugLatestDTO.DebtCashflowSummaryDTO();
        if (summary == null) {
            return dto;
        }
        dto.setDti(summary.getDti());
        dto.setSurplusRate(summary.getSurplusRate());
        dto.setEmergencyFundMonths(summary.getEmergencyFundMonths());
        dto.setStressLevel(summary.getStressLevel());
        dto.setWarningsCount(summary.getWarningsCount());
        return dto;
    }

    private AdminMarketDebugLatestDTO.DebtOptimizerSummaryDTO mapDebtOptimizerSummary(
            PortfolioMarketDebugSnapshot.DebtOptimizerSummary summary) {
        AdminMarketDebugLatestDTO.DebtOptimizerSummaryDTO dto = new AdminMarketDebugLatestDTO.DebtOptimizerSummaryDTO();
        if (summary == null) {
            return dto;
        }
        dto.setStrategy(summary.getStrategy());
        dto.setTopDebtName(summary.getTopDebtName());
        dto.setBudgetForExtraPayment(summary.getBudgetForExtraPayment());
        dto.setWarningsCount(summary.getWarningsCount());
        return dto;
    }

    private AdminMarketDebugLatestDTO.InsuranceGapSummaryDTO mapInsuranceGapSummary(
            PortfolioMarketDebugSnapshot.InsuranceGapSummary summary) {
        AdminMarketDebugLatestDTO.InsuranceGapSummaryDTO dto = new AdminMarketDebugLatestDTO.InsuranceGapSummaryDTO();
        if (summary == null) {
            return dto;
        }
        dto.setPremiumRatio(summary.getPremiumRatio());
        dto.setTopGapType(summary.getTopGapType());
        dto.setTopGapValue(summary.getTopGapValue());
        dto.setWarningsCount(summary.getWarningsCount());
        return dto;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
