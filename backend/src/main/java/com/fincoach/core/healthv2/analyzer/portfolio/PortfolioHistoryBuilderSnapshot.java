package com.fincoach.core.healthv2.analyzer.portfolio;

import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.repository.PortfolioPriceSnapshotRepository;
import com.fincoach.core.healthv2.util.HealthV2ConfigDefaults;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PortfolioHistoryBuilderSnapshot {

    public static final String WARN_CACHE_HIT = "SNAPSHOT_CACHE_HIT";
    public static final String WARN_CACHE_MISS = "SNAPSHOT_CACHE_MISS_INSUFFICIENT_POINTS";
    public static final String WARN_DB_UNAVAILABLE = "SNAPSHOT_DB_UNAVAILABLE";

    private static final int MIN_POINTS = 2;

    private final PortfolioPriceSnapshotRepository repository;

    public PortfolioHistoryBuilderSnapshot(PortfolioPriceSnapshotRepository repository) {
        this.repository = repository;
    }

    public PortfolioInput buildFromSnapshots(long userId, int lookbackDays) {
        Double rfAnnual = HealthV2ConfigDefaults.DEFAULT_RF_ANNUAL;
        List<String> warnings = new ArrayList<>();

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(1, lookbackDays));

        List<FcPortfolioPriceSnapshotEntity> snapshots =
                repository.findByUserIdAndDateRange(userId, start, end);

        if (repository.consumeDbUnavailable()) {
            warnings.add(WARN_DB_UNAVAILABLE);
        }

        if (snapshots == null || snapshots.isEmpty()) {
            warnings.add(WARN_CACHE_MISS);
            return new PortfolioInput(rfAnnual, null, null, null, null, warnings);
        }

        List<Double> equityCurve = new ArrayList<>();
        List<Double> returnsSeries = new ArrayList<>();
        BigDecimal prev = null;

        for (FcPortfolioPriceSnapshotEntity snap : snapshots) {
            if (snap == null || snap.getEquity() == null) continue;
            BigDecimal curr = snap.getEquity();
            equityCurve.add(curr.doubleValue());
            if (prev != null && prev.compareTo(BigDecimal.ZERO) > 0 && curr.compareTo(BigDecimal.ZERO) > 0) {
                returnsSeries.add(curr.doubleValue() / prev.doubleValue() - 1.0);
            }
            prev = curr;
        }

        if (equityCurve.size() < MIN_POINTS || returnsSeries.size() < MIN_POINTS - 1) {
            warnings.add(WARN_CACHE_MISS);
        } else {
            warnings.add(WARN_CACHE_HIT);
        }

        return new PortfolioInput(rfAnnual,
                returnsSeries.isEmpty() ? null : returnsSeries,
                equityCurve.isEmpty() ? null : equityCurve,
                null,
                null,
                warnings);
    }
}
