package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CreateAssetDTO;
import com.fincoach.core.healthv2.dto.UpdateAssetDTO;
import com.fincoach.core.healthv2.entity.FcAssetEntity;

import java.util.List;

/**
 * 体检v2-资产服务接口
 */
public interface FcAssetService {

    FcAssetEntity create(Long userId, CreateAssetDTO dto);

    FcAssetEntity update(Long userId, UpdateAssetDTO dto);

    List<FcAssetEntity> listByUserId(Long userId);

    void delete(Long userId, Long id);
}
