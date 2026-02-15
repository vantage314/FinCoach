package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CreateCashflowDTO;
import com.fincoach.core.healthv2.entity.FcCashflowEntity;

import java.util.List;

/**
 * 体检v2-现金流服务接口
 */
public interface FcCashflowService {

    /**
     * 创建或更新（upsert）：同 user_id + month 存在则更新
     */
    FcCashflowEntity upsert(Long userId, CreateCashflowDTO dto);

    List<FcCashflowEntity> listByUserId(Long userId);
}
