package com.fincoach.core.healthv2.analyzer.market;

import com.fincoach.core.healthv2.debug.PortfolioDebugContextHolder;
import com.fincoach.core.healthv2.debug.PortfolioMarketDebugSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PositionTickerResolver {

    private static final Logger log = LoggerFactory.getLogger(PositionTickerResolver.class);

    @Autowired
    private TickerMappingRegistry registry;

    public Map<String, BigDecimal> resolve(Map<String, Object> inputPositions, List<String> warnings) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (inputPositions == null || inputPositions.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, Object> entry : inputPositions.entrySet()) {
            String rawKey = entry.getKey();
            Object val = entry.getValue();
            BigDecimal qty = (val instanceof Number) ? new BigDecimal(val.toString()) : BigDecimal.ZERO;

            TickerMappingRegistry.ResolutionResult resolution = registry.resolve(rawKey);
            appendDebug(rawKey, resolution);
            
            if (resolution.getResolvedTicker() != null) {
                // If we have duplicate resolved tickers (e.g. "AAPL" and "Apple" both -> "AAPL.US"), sum them up?
                // M7-4 spec says: "ResolvedPositions keys一律标准ticker"
                // Let's assume distinct inputs map to distinct or same output. If same, we should merge.
                // Map.merge
                result.merge(resolution.getResolvedTicker(), qty, BigDecimal::add);
                
                if (resolution.getWarnings() != null) {
                    // Dedup warnings?
                    for (String w : resolution.getWarnings()) {
                        if (!warnings.contains(w)) {
                            warnings.add(w);
                        }
                    }
                }
            } else {
                 if (resolution.getWarnings() != null) {
                     for (String w : resolution.getWarnings()) {
                         if (!warnings.contains(w)) warnings.add(w);
                     }
                 }
                 // If unresolved, it effectively drops from result (or we can't use it for history).
                 // Logic: If ANY position is unresolved, we might want to fail the whole history builder or just skip?
                 // Current builder logic: "If targetPositions.isEmpty()... return null".
                 // But if we have 5 assets and 1 unresolved?
                 // M7-3: "positions 缺失 / ticker 缺失：仍 fallback，但必须在 warnings 里明确原因"
                 // If we successfully resolved 4/5, we proceed with 4, BUT we warn about the 1 missing.
                 // This allows partial history.
            }
        }
        return result;
    }

    private void appendDebug(String rawKey, TickerMappingRegistry.ResolutionResult resolution) {
        PortfolioDebugContextHolder.record(snapshot -> {
            if (snapshot.getResolvedTickers() == null) {
                snapshot.setResolvedTickers(new ArrayList<>());
            }
            PortfolioMarketDebugSnapshot.ResolvedTickerDebug debug = new PortfolioMarketDebugSnapshot.ResolvedTickerDebug();
            debug.setInput(rawKey);
            debug.setNormalizedKey(resolution.getNormalizedInput());
            debug.setResolvedTicker(resolution.getResolvedTicker());
            debug.setMappingSource(resolution.getSource());
            debug.setWarnings(resolution.getWarnings() == null ? null : new ArrayList<>(resolution.getWarnings()));
            snapshot.getResolvedTickers().add(debug);
        });
    }
}
