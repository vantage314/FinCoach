package com.fincoach.core.healthv2.advice;

import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.entity.FcCashflowEntity;
import com.fincoach.core.healthv2.entity.FcLiabilityEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class AdviceInputQuality {
    private final List<FcAssetEntity> assets;
    private final List<FcLiabilityEntity> liabilities;
    private final FcCashflowEntity cashflow;
    private final Map<String, Object> allocation;
    private final BigDecimal totalAssets;

    private AdviceInputQuality(List<FcAssetEntity> assets,
                               List<FcLiabilityEntity> liabilities,
                               FcCashflowEntity cashflow,
                               Map<String, Object> allocation,
                               BigDecimal totalAssets) {
        this.assets = assets;
        this.liabilities = liabilities;
        this.cashflow = cashflow;
        this.allocation = allocation;
        this.totalAssets = totalAssets;
    }

    public static AdviceInputQuality evaluate(List<FcAssetEntity> assets,
                                              List<FcLiabilityEntity> liabilities,
                                              FcCashflowEntity cashflow,
                                              Map<String, Object> allocation,
                                              BigDecimal totalAssets,
                                              WarningCollector warnings) {
        if (warnings != null) {
            if (assets == null || assets.isEmpty()) {
                warnings.add(AdviceWarningCodes.MISSING_ASSETS, "assets null or empty");
            }
            if (liabilities == null || liabilities.isEmpty()) {
                warnings.add(AdviceWarningCodes.MISSING_LIABILITIES, "liabilities null or empty");
            }
            if (cashflow == null) {
                warnings.add(AdviceWarningCodes.MISSING_CASHFLOW, "cashflow is null");
            }
            if (allocation == null || allocation.isEmpty()) {
                warnings.add(AdviceWarningCodes.MISSING_ALLOCATION, "allocation null or empty");
            }
            if (totalAssets == null) {
                warnings.add(AdviceWarningCodes.MISSING_TOTAL_ASSETS, "totalAssets is null");
            }
        }

        return new AdviceInputQuality(
                SafeValue.listOrEmpty(assets),
                SafeValue.listOrEmpty(liabilities),
                cashflow,
                SafeValue.mapOrEmpty(allocation),
                totalAssets
        );
    }

    public List<FcAssetEntity> getAssets() { return assets; }
    public List<FcLiabilityEntity> getLiabilities() { return liabilities; }
    public FcCashflowEntity getCashflow() { return cashflow; }
    public Map<String, Object> getAllocation() { return allocation; }
    public BigDecimal getTotalAssets() { return totalAssets; }
}
