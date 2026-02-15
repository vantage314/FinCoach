package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.InsuranceProfileDTO;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;

/**
 * 体检v2-保险档案服务接口
 */
public interface FcInsuranceProfileService {

    /**
     * 创建或更新（upsert）：一个用户仅一条记录
     */
    FcInsuranceProfileEntity upsert(Long userId, InsuranceProfileDTO dto);

    FcInsuranceProfileEntity getByUserId(Long userId);
}
