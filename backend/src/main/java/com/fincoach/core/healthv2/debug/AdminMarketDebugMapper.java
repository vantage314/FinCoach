package com.fincoach.core.healthv2.debug;

import com.fincoach.core.healthv2.dto.admin.AdminMarketDebugLatestDTO;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class AdminMarketDebugMapper {

    public AdminMarketDebugLatestDTO toDto(PortfolioMarketDebugSnapshot snapshot, Long userId) {
        AdminMarketDebugLatestDTO dto = new AdminMarketDebugLatestDTO();
        dto.setRequestId(resolveRequestId());
        dto.setTimestamp(System.currentTimeMillis());
        dto.setUserId(userId);
        dto.setEnabled(snapshot != null);
        dto.setWarnings(new ArrayList<>());
        dto.setResolvedTickers(new ArrayList<>());

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

        return dto;
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
        return dto;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
