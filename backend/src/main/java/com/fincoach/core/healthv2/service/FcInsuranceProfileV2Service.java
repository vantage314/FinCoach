package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.InsuranceProfileUpsertV2DTO;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;

public interface FcInsuranceProfileV2Service {
    FcInsuranceProfileEntity upsert(Long userId, InsuranceProfileUpsertV2DTO dto);
    FcInsuranceProfileEntity getByUserId(Long userId);
}
