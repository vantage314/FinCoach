package com.fincoach.core.healthv2.analyzer.portfolio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class ReturnSeriesAlignerTest {

    @Test
    public void testAlignIntersection() {
        LocalDate base = LocalDate.of(2024, 1, 1);
        NavigableMap<LocalDate, Double> a = new TreeMap<>();
        NavigableMap<LocalDate, Double> b = new TreeMap<>();

        for (int i = 0; i < 5; i++) {
            a.put(base.plusDays(i), 0.01 * (i + 1));
        }
        for (int i = 2; i < 7; i++) {
            b.put(base.plusDays(i), 0.02 * (i + 1));
        }

        Map<String, NavigableMap<LocalDate, Double>> input = new HashMap<>();
        input.put("AAPL.US", a);
        input.put("MSFT.US", b);

        ReturnSeriesAligner aligner = new ReturnSeriesAligner(3);
        ReturnSeriesAligner.AlignedSeriesResult res = aligner.align(input);

        assertEquals(3, res.getEffectivePoints());
        assertTrue(res.getWarnings().contains(ReturnSeriesAligner.WARN_INTERSECTION));
        assertEquals(2, res.getReturns().length);
        assertEquals(3, res.getReturns()[0].length);
    }

    @Test
    public void testAlignRelaxedInsufficient() {
        LocalDate base = LocalDate.of(2024, 1, 1);
        NavigableMap<LocalDate, Double> a = new TreeMap<>();
        NavigableMap<LocalDate, Double> b = new TreeMap<>();

        a.put(base.plusDays(0), 0.01);
        a.put(base.plusDays(1), 0.02);
        a.put(base.plusDays(3), 0.03);
        a.put(base.plusDays(4), 0.04);

        b.put(base.plusDays(0), 0.01);
        b.put(base.plusDays(1), 0.02);
        b.put(base.plusDays(2), 0.03);
        b.put(base.plusDays(4), 0.04);

        Map<String, NavigableMap<LocalDate, Double>> input = new HashMap<>();
        input.put("AAPL.US", a);
        input.put("MSFT.US", b);

        ReturnSeriesAligner aligner = new ReturnSeriesAligner(4);
        ReturnSeriesAligner.AlignedSeriesResult res = aligner.align(input);

        assertTrue(res.getWarnings().contains(ReturnSeriesAligner.WARN_RELAXED));
        assertTrue(res.getWarnings().contains(ReturnSeriesAligner.WARN_INSUFFICIENT));
        assertTrue(res.getEffectivePoints() < 4);
    }

    @Test
    public void testAnalyzerCorrelationInsufficientPoints() {
        LocalDate base = LocalDate.of(2024, 1, 1);
        NavigableMap<LocalDate, Double> a = new TreeMap<>();
        NavigableMap<LocalDate, Double> b = new TreeMap<>();
        a.put(base.plusDays(0), 0.01);
        a.put(base.plusDays(1), 0.02);
        b.put(base.plusDays(0), 0.01);
        b.put(base.plusDays(1), 0.02);

        Map<String, NavigableMap<LocalDate, Double>> series = new HashMap<>();
        series.put("AAPL.US", a);
        series.put("MSFT.US", b);

        PortfolioInput input = new PortfolioInput();
        input.setReturnsByAssetDate(series);

        PortfolioAnalyzerImpl analyzer = new PortfolioAnalyzerImpl();
        PortfolioMetrics metrics = analyzer.analyze(input);

        assertNull(metrics.getCorrelation());
        assertTrue(metrics.getWarnings().contains(ReturnSeriesAligner.WARN_INSUFFICIENT));
    }

    @Test
    public void testAnalyzerCorrelationGapRatioTooHigh() {
        LocalDate base = LocalDate.of(2024, 1, 1);
        NavigableMap<LocalDate, Double> a = new TreeMap<>();
        NavigableMap<LocalDate, Double> b = new TreeMap<>();

        for (int i = 0; i < 100; i++) {
            a.put(base.plusDays(i), 0.01 + (i * 0.0001));
        }
        for (int i = 0; i < 40; i++) {
            b.put(base.plusDays(i), 0.02 + (i * 0.0002));
        }

        Map<String, NavigableMap<LocalDate, Double>> series = new HashMap<>();
        series.put("AAPL.US", a);
        series.put("MSFT.US", b);

        PortfolioInput input = new PortfolioInput();
        input.setReturnsByAssetDate(series);

        PortfolioAnalyzerImpl analyzer = new PortfolioAnalyzerImpl();
        PortfolioMetrics metrics = analyzer.analyze(input);

        assertNull(metrics.getCorrelation());
        assertTrue(metrics.getWarnings().contains(ReturnSeriesAligner.WARN_GAP_RATIO_TOO_HIGH));
    }

    @Test
    public void testAnalyzerCorrelationGapRatioOk() {
        LocalDate base = LocalDate.of(2024, 1, 1);
        NavigableMap<LocalDate, Double> a = new TreeMap<>();
        NavigableMap<LocalDate, Double> b = new TreeMap<>();

        for (int i = 0; i < 60; i++) {
            a.put(base.plusDays(i), 0.01 + (i * 0.0001));
            b.put(base.plusDays(i), 0.02 + (i * 0.0002));
        }

        Map<String, NavigableMap<LocalDate, Double>> series = new HashMap<>();
        series.put("AAPL.US", a);
        series.put("MSFT.US", b);

        PortfolioInput input = new PortfolioInput();
        input.setReturnsByAssetDate(series);

        PortfolioAnalyzerImpl analyzer = new PortfolioAnalyzerImpl();
        PortfolioMetrics metrics = analyzer.analyze(input);

        assertNotNull(metrics.getCorrelation());
        assertFalse(metrics.getWarnings().contains(ReturnSeriesAligner.WARN_GAP_RATIO_TOO_HIGH));
    }
}
