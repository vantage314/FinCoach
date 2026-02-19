package com.fincoach.core.healthv2.analyzer;

import com.fincoach.core.healthv2.entity.FcInsuranceConfigEntity;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InsuranceGapCalculatorTest {

    @Test
    public void testGapFloorsAtZero() {
        FcInsuranceProfileEntity profile = new FcInsuranceProfileEntity();
        profile.setExistingCoverMedical(new BigDecimal("600000"));
        profile.setAnnualIncome(new BigDecimal("100000"));

        FcInsuranceConfigEntity config = new FcInsuranceConfigEntity();
        config.setTargetMedical(new BigDecimal("500000"));

        InsuranceGapCalculator calculator = new InsuranceGapCalculator();
        InsuranceGapCalculator.InsuranceGapCalcResult result = calculator.calculate(profile, config);

        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = result.metrics();
        assertEquals(new BigDecimal("0"), metrics.get("medicalGap"));
    }

    @Test
    public void testPremiumRatioWarnings() {
        FcInsuranceProfileEntity profile = new FcInsuranceProfileEntity();
        profile.setAnnualIncome(new BigDecimal("100000"));
        profile.setAnnualPremiumTotal(new BigDecimal("20000"));

        FcInsuranceConfigEntity config = new FcInsuranceConfigEntity();
        config.setPremiumRatioWarn(new BigDecimal("0.10"));
        config.setPremiumRatioDanger(new BigDecimal("0.20"));

        InsuranceGapCalculator calculator = new InsuranceGapCalculator();
        InsuranceGapCalculator.InsuranceGapCalcResult result = calculator.calculate(profile, config);

        List<String> warnings = result.warnings();
        assertTrue(warnings.contains(InsuranceWarningCodes.PREMIUM_RATIO_DANGER));
    }

    @Test
    public void testLifeTargetMultiplierEffect() {
        FcInsuranceProfileEntity profile = new FcInsuranceProfileEntity();
        profile.setAnnualIncome(new BigDecimal("100000"));

        FcInsuranceConfigEntity config = new FcInsuranceConfigEntity();
        config.setTargetLifeMultiplier(new BigDecimal("5"));

        InsuranceGapCalculator calculator = new InsuranceGapCalculator();
        InsuranceGapCalculator.InsuranceGapCalcResult result = calculator.calculate(profile, config);

        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = result.metrics();
        assertEquals(new BigDecimal("500000.00"), metrics.get("lifeTarget"));
    }
}
