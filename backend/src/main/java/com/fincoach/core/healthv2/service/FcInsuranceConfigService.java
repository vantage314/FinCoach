package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.InsuranceConfigUpsertDTO;
import com.fincoach.core.healthv2.entity.FcInsuranceConfigEntity;

public interface FcInsuranceConfigService {
    FcInsuranceConfigEntity getDefaultConfig();
    FcInsuranceConfigEntity upsertDefault(InsuranceConfigUpsertDTO dto);
}
