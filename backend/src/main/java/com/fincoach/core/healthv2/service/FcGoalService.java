package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CreateGoalDTO;
import com.fincoach.core.healthv2.dto.UpdateGoalDTO;
import com.fincoach.core.healthv2.entity.FcGoalEntity;

import java.util.List;

/**
 * 体检v2-目标服务接口
 */
public interface FcGoalService {

    FcGoalEntity create(Long userId, CreateGoalDTO dto);

    FcGoalEntity update(Long userId, UpdateGoalDTO dto);

    List<FcGoalEntity> listByUserId(Long userId);

    void delete(Long userId, Long id);
}
