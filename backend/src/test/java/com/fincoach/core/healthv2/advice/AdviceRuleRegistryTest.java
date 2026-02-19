package com.fincoach.core.healthv2.advice;

import com.fincoach.core.healthv2.advice.rules.AdviceRuleDefaults;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleRegistry;
import com.fincoach.core.healthv2.advice.rules.AdviceRuleSnapshot;
import com.fincoach.core.healthv2.entity.FcAdviceRuleSetEntity;
import com.fincoach.core.healthv2.mapper.FcAdviceRuleParamMapper;
import com.fincoach.core.healthv2.mapper.FcAdviceRuleSetMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class AdviceRuleRegistryTest {

    @Test
    public void testFallbackWhenParamsMissing() {
        FcAdviceRuleSetMapper ruleSetMapper = Mockito.mock(FcAdviceRuleSetMapper.class);
        FcAdviceRuleParamMapper paramMapper = Mockito.mock(FcAdviceRuleParamMapper.class);

        FcAdviceRuleSetEntity active = new FcAdviceRuleSetEntity();
        active.setId(1L);
        active.setCode(AdviceRuleDefaults.DEFAULT_CODE);
        active.setVersion(1);
        active.setEnabled(1);

        Mockito.when(ruleSetMapper.selectActive()).thenReturn(active);
        Mockito.when(paramMapper.selectList(any())).thenReturn(List.of());

        AdviceRuleRegistry registry = new AdviceRuleRegistry(ruleSetMapper, paramMapper);
        registry.reload();
        AdviceRuleSnapshot snapshot = registry.get();

        assertNotNull(snapshot);
        assertEquals(AdviceRuleDefaults.DEFAULT_CODE, snapshot.getCode());
        assertTrue(snapshot.getWarnings().stream().anyMatch(w -> w.contains("ADVICE_RULE_PARAM_MISSING")));
        assertEquals(0.05, snapshot.getDecimal(AdviceRuleDefaults.REBALANCE_DRIFT_PCT, BigDecimal.ZERO).doubleValue(), 0.0001);
    }
}
