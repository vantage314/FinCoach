package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CreateLiabilityDTO;
import com.fincoach.core.healthv2.dto.UpdateLiabilityDTO;
import com.fincoach.core.healthv2.entity.FcLiabilityEntity;

import java.util.List;

/**
 * 体检v2-负债服务接口
 */
public interface FcLiabilityService {

    FcLiabilityEntity create(Long userId, CreateLiabilityDTO dto);

    FcLiabilityEntity update(Long userId, UpdateLiabilityDTO dto);

    List<FcLiabilityEntity> listByUserId(Long userId);

    void delete(Long userId, Long id);
}
