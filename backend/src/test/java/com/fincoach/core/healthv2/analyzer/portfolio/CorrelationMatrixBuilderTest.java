package com.fincoach.core.healthv2.analyzer.portfolio;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CorrelationMatrixBuilderTest {

    @Test
    public void testThreeAssetsEnoughSamples() {
        Map<String, List<Double>> series = new LinkedHashMap<>();
        series.put("A", seq(0.01, 12));
        series.put("B", seq(0.015, 12));
        series.put("C", seq(-0.01, 12));

        PortfolioInput input = PortfolioInput.builder()
                .returnsByAssetKey(series)
                .build();

        CorrelationMatrixResult result = new CorrelationMatrixBuilder().build(input);

        assertNotNull(result);
        assertEquals(3, result.getAssets().size());
        assertEquals(12, result.getSampleSize());
        assertEquals(3, result.getMatrix().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(3, result.getMatrix().get(i).size());
            assertEquals(1.0, result.getMatrix().get(i).get(i));
        }
        assertSymmetric(result.getMatrix());
        assertWithinRange(result.getMatrix());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testNotEnoughAssets() {
        Map<String, List<Double>> series = new LinkedHashMap<>();
        series.put("A", seq(0.01, 12));

        PortfolioInput input = PortfolioInput.builder()
                .returnsByAssetKey(series)
                .build();

        CorrelationMatrixResult result = new CorrelationMatrixBuilder().build(input);

        assertNotNull(result);
        assertTrue(result.getWarnings().contains(CorrelationWarningCodes.CORR_NOT_ENOUGH_ASSETS));
        assertTrue(result.getMatrix().isEmpty());
    }

    @Test
    public void testInsufficientPointsDropsAsset() {
        Map<String, List<Double>> series = new LinkedHashMap<>();
        series.put("A", seq(0.01, 5));
        series.put("B", seq(0.02, 12));

        PortfolioInput input = PortfolioInput.builder()
                .returnsByAssetKey(series)
                .build();

        CorrelationMatrixResult result = new CorrelationMatrixBuilder().build(input);

        assertNotNull(result);
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().contains(CorrelationWarningCodes.CORR_INSUFFICIENT_POINTS + ":A"));
        assertTrue(result.getWarnings().contains(CorrelationWarningCodes.CORR_NOT_ENOUGH_ASSETS));
    }

    private List<Double> seq(double start, int count) {
        List<Double> values = new ArrayList<>();
        double v = start;
        for (int i = 0; i < count; i++) {
            values.add(v);
            v += 0.001;
        }
        return values;
    }

    private void assertSymmetric(List<List<Double>> matrix) {
        for (int i = 0; i < matrix.size(); i++) {
            for (int j = 0; j < matrix.size(); j++) {
                assertEquals(matrix.get(i).get(j), matrix.get(j).get(i));
            }
        }
    }

    private void assertWithinRange(List<List<Double>> matrix) {
        for (List<Double> row : matrix) {
            for (Double v : row) {
                assertTrue(v >= -1.0 && v <= 1.0);
            }
        }
    }
}
