package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.InsuranceProfileUpsertV2DTO;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;
import com.fincoach.core.healthv2.mapper.FcInsuranceProfileMapper;
import com.fincoach.core.healthv2.service.impl.FcInsuranceProfileV2ServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class FcInsuranceProfileV2ServiceTest {

    @Test
    public void testUpsertCreatesProfile() {
        FcInsuranceProfileMapper mapper = Mockito.mock(FcInsuranceProfileMapper.class);
        FcInsuranceProfileV2ServiceImpl service = new FcInsuranceProfileV2ServiceImpl();
        ReflectionTestUtils.setField(service, "profileMapper", mapper);

        InsuranceProfileUpsertV2DTO dto = new InsuranceProfileUpsertV2DTO();
        dto.setAnnualIncome(new BigDecimal("120000"));
        dto.setAnnualPremiumTotal(new BigDecimal("6000"));
        dto.setDependents(2);
        dto.setExistingCoverMedical(new BigDecimal("100000"));

        ArgumentCaptor<FcInsuranceProfileEntity> captor = ArgumentCaptor.forClass(FcInsuranceProfileEntity.class);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(captor.capture())).thenReturn(1);

        FcInsuranceProfileEntity saved = service.upsert(1L, dto);

        assertNotNull(saved);
        assertEquals(new BigDecimal("120000"), captor.getValue().getAnnualIncome());
        assertEquals(new BigDecimal("6000"), captor.getValue().getAnnualPremiumTotal());
        assertEquals(2, captor.getValue().getDependents());
        assertEquals(2, captor.getValue().getDependentsCount());
        assertEquals(new BigDecimal("100000"), captor.getValue().getExistingCoverMedical());
    }

    @Test
    public void testGetReturnsDefaultsWhenMissing() {
        FcInsuranceProfileMapper mapper = Mockito.mock(FcInsuranceProfileMapper.class);
        FcInsuranceProfileV2ServiceImpl service = new FcInsuranceProfileV2ServiceImpl();
        ReflectionTestUtils.setField(service, "profileMapper", mapper);

        when(mapper.selectOne(any())).thenReturn(null);
        FcInsuranceProfileEntity result = service.getByUserId(1L);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getAnnualIncome());
        assertEquals(BigDecimal.ZERO, result.getAnnualPremiumTotal());
        assertEquals(0, result.getDependents());
    }
}
