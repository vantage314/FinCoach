package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.InsuranceConfigUpsertDTO;
import com.fincoach.core.healthv2.entity.FcInsuranceConfigEntity;
import com.fincoach.core.healthv2.mapper.FcInsuranceConfigMapper;
import com.fincoach.core.healthv2.service.impl.FcInsuranceConfigServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class FcInsuranceConfigServiceTest {

    @Test
    public void testUpsertCreatesDefaultConfig() {
        FcInsuranceConfigMapper mapper = Mockito.mock(FcInsuranceConfigMapper.class);
        FcInsuranceConfigServiceImpl service = new FcInsuranceConfigServiceImpl();
        ReflectionTestUtils.setField(service, "configMapper", mapper);

        InsuranceConfigUpsertDTO dto = new InsuranceConfigUpsertDTO();
        dto.setTargetMedical(new BigDecimal("400000"));
        dto.setPremiumRatioWarn(new BigDecimal("0.12"));
        dto.setPremiumRatioDanger(new BigDecimal("0.22"));

        when(mapper.selectOne(any())).thenReturn(null);
        ArgumentCaptor<FcInsuranceConfigEntity> captor = ArgumentCaptor.forClass(FcInsuranceConfigEntity.class);
        when(mapper.insert(captor.capture())).thenReturn(1);

        FcInsuranceConfigEntity saved = service.upsertDefault(dto);

        assertNotNull(saved);
        assertEquals("DEFAULT", captor.getValue().getCode());
        assertEquals(new BigDecimal("400000"), captor.getValue().getTargetMedical());
        assertEquals(new BigDecimal("0.12"), captor.getValue().getPremiumRatioWarn());
        assertEquals(new BigDecimal("0.22"), captor.getValue().getPremiumRatioDanger());
    }

    @Test
    public void testGetDefaultReturnsFallbackValues() {
        FcInsuranceConfigMapper mapper = Mockito.mock(FcInsuranceConfigMapper.class);
        FcInsuranceConfigServiceImpl service = new FcInsuranceConfigServiceImpl();
        ReflectionTestUtils.setField(service, "configMapper", mapper);

        when(mapper.selectOne(any())).thenReturn(null);
        FcInsuranceConfigEntity config = service.getDefaultConfig();

        assertNotNull(config);
        assertEquals(new BigDecimal("500000"), config.getTargetMedical());
        assertEquals(new BigDecimal("0.10"), config.getPremiumRatioWarn());
    }
}
