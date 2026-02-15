package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.dto.InsuranceProfileDTO;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;
import com.fincoach.core.healthv2.mapper.FcInsuranceProfileMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.service.FcInsuranceProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 体检v2-保险档案服务实现
 * upsert 语义：一个用户仅一条记录
 */
@Slf4j
@Service
public class FcInsuranceProfileServiceImpl implements FcInsuranceProfileService {

    @Autowired
    private FcInsuranceProfileMapper insuranceMapper;

    @Autowired
    private AuditService auditService;

    @Override
    public FcInsuranceProfileEntity upsert(Long userId, InsuranceProfileDTO dto) {
        log.info("[HealthV2-Insurance] upsert, userId={}", userId);

        LambdaQueryWrapper<FcInsuranceProfileEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcInsuranceProfileEntity::getUserId, userId);
        FcInsuranceProfileEntity existing = insuranceMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新
            FcInsuranceProfileEntity before = new FcInsuranceProfileEntity();
            before.setId(existing.getId());
            before.setAnnualIncome(existing.getAnnualIncome());

            if (dto.getAnnualIncome() != null) existing.setAnnualIncome(dto.getAnnualIncome());
            if (dto.getMaritalStatus() != null) existing.setMaritalStatus(dto.getMaritalStatus());
            if (dto.getChildrenCount() != null) existing.setChildrenCount(dto.getChildrenCount());
            if (dto.getDependentsCount() != null) existing.setDependentsCount(dto.getDependentsCount());
            if (dto.getCityTier() != null) existing.setCityTier(dto.getCityTier());
            if (dto.getExistingCoverageJson() != null) existing.setExistingCoverageJson(dto.getExistingCoverageJson());
            existing.setUpdateTime(LocalDateTime.now());

            insuranceMapper.updateById(existing);
            auditService.log(userId, "UPDATE", "FC_INSURANCE_PROFILE", existing.getId(), before, existing);
            return existing;
        } else {
            // 新增
            FcInsuranceProfileEntity entity = new FcInsuranceProfileEntity();
            entity.setUserId(userId);
            entity.setAnnualIncome(dto.getAnnualIncome());
            entity.setMaritalStatus(dto.getMaritalStatus());
            entity.setChildrenCount(dto.getChildrenCount() != null ? dto.getChildrenCount() : 0);
            entity.setDependentsCount(dto.getDependentsCount() != null ? dto.getDependentsCount() : 0);
            entity.setCityTier(dto.getCityTier());
            entity.setExistingCoverageJson(dto.getExistingCoverageJson());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());

            insuranceMapper.insert(entity);
            auditService.log(userId, "CREATE", "FC_INSURANCE_PROFILE", entity.getId(), null, entity);
            return entity;
        }
    }

    @Override
    public FcInsuranceProfileEntity getByUserId(Long userId) {
        LambdaQueryWrapper<FcInsuranceProfileEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcInsuranceProfileEntity::getUserId, userId);
        return insuranceMapper.selectOne(wrapper);
    }
}
