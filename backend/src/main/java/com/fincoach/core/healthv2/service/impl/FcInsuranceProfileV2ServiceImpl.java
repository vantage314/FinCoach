package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.dto.InsuranceProfileUpsertV2DTO;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;
import com.fincoach.core.healthv2.mapper.FcInsuranceProfileMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.service.FcInsuranceProfileV2Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class FcInsuranceProfileV2ServiceImpl implements FcInsuranceProfileV2Service {

    @Autowired
    private FcInsuranceProfileMapper profileMapper;

    @Autowired(required = false)
    private AuditService auditService;

    @Override
    public FcInsuranceProfileEntity upsert(Long userId, InsuranceProfileUpsertV2DTO dto) {
        log.info("[InsuranceProfileV2] upsert, userId={}", userId);
        LambdaQueryWrapper<FcInsuranceProfileEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcInsuranceProfileEntity::getUserId, userId);
        FcInsuranceProfileEntity existing = profileMapper.selectOne(wrapper);

        if (existing != null) {
            FcInsuranceProfileEntity before = new FcInsuranceProfileEntity();
            before.setId(existing.getId());
            before.setAnnualIncome(existing.getAnnualIncome());
            before.setAnnualPremiumTotal(existing.getAnnualPremiumTotal());
            before.setDependents(existing.getDependents());
            before.setAge(existing.getAge());
            before.setExistingCoverMedical(existing.getExistingCoverMedical());
            before.setExistingCoverAccident(existing.getExistingCoverAccident());
            before.setExistingCoverCi(existing.getExistingCoverCi());
            before.setExistingCoverLife(existing.getExistingCoverLife());

            if (dto.getAnnualIncome() != null) existing.setAnnualIncome(dto.getAnnualIncome());
            if (dto.getAnnualPremiumTotal() != null) existing.setAnnualPremiumTotal(dto.getAnnualPremiumTotal());
            if (dto.getDependents() != null) {
                existing.setDependents(dto.getDependents());
                existing.setDependentsCount(dto.getDependents());
            }
            if (dto.getAge() != null) existing.setAge(dto.getAge());
            if (dto.getExistingCoverMedical() != null) existing.setExistingCoverMedical(dto.getExistingCoverMedical());
            if (dto.getExistingCoverAccident() != null) existing.setExistingCoverAccident(dto.getExistingCoverAccident());
            if (dto.getExistingCoverCi() != null) existing.setExistingCoverCi(dto.getExistingCoverCi());
            if (dto.getExistingCoverLife() != null) existing.setExistingCoverLife(dto.getExistingCoverLife());
            existing.setUpdateTime(LocalDateTime.now());

            profileMapper.updateById(existing);
            if (auditService != null) {
                auditService.log(userId, "UPDATE", "FC_INSURANCE_PROFILE", existing.getId(), before, existing);
            }
            return fillDefaults(existing);
        }

        FcInsuranceProfileEntity entity = new FcInsuranceProfileEntity();
        entity.setUserId(userId);
        entity.setAnnualIncome(safe(dto.getAnnualIncome()));
        entity.setAnnualPremiumTotal(safe(dto.getAnnualPremiumTotal()));
        entity.setDependents(dto.getDependents() == null ? 0 : dto.getDependents());
        entity.setDependentsCount(dto.getDependents() == null ? 0 : dto.getDependents());
        entity.setAge(dto.getAge());
        entity.setExistingCoverMedical(safe(dto.getExistingCoverMedical()));
        entity.setExistingCoverAccident(safe(dto.getExistingCoverAccident()));
        entity.setExistingCoverCi(safe(dto.getExistingCoverCi()));
        entity.setExistingCoverLife(safe(dto.getExistingCoverLife()));
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        profileMapper.insert(entity);
        if (auditService != null) {
            auditService.log(userId, "CREATE", "FC_INSURANCE_PROFILE", entity.getId(), null, entity);
        }
        return fillDefaults(entity);
    }

    @Override
    public FcInsuranceProfileEntity getByUserId(Long userId) {
        LambdaQueryWrapper<FcInsuranceProfileEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcInsuranceProfileEntity::getUserId, userId);
        FcInsuranceProfileEntity entity = profileMapper.selectOne(wrapper);
        if (entity == null) {
            return defaultProfile(userId);
        }
        return fillDefaults(entity);
    }

    private FcInsuranceProfileEntity defaultProfile(Long userId) {
        FcInsuranceProfileEntity entity = new FcInsuranceProfileEntity();
        entity.setUserId(userId);
        entity.setAnnualIncome(BigDecimal.ZERO);
        entity.setAnnualPremiumTotal(BigDecimal.ZERO);
        entity.setDependents(0);
        entity.setDependentsCount(0);
        entity.setExistingCoverMedical(BigDecimal.ZERO);
        entity.setExistingCoverAccident(BigDecimal.ZERO);
        entity.setExistingCoverCi(BigDecimal.ZERO);
        entity.setExistingCoverLife(BigDecimal.ZERO);
        return entity;
    }

    private FcInsuranceProfileEntity fillDefaults(FcInsuranceProfileEntity entity) {
        if (entity.getAnnualIncome() == null) entity.setAnnualIncome(BigDecimal.ZERO);
        if (entity.getAnnualPremiumTotal() == null) entity.setAnnualPremiumTotal(BigDecimal.ZERO);
        if (entity.getDependents() == null) entity.setDependents(0);
        if (entity.getDependentsCount() == null) entity.setDependentsCount(entity.getDependents());
        if (entity.getExistingCoverMedical() == null) entity.setExistingCoverMedical(BigDecimal.ZERO);
        if (entity.getExistingCoverAccident() == null) entity.setExistingCoverAccident(BigDecimal.ZERO);
        if (entity.getExistingCoverCi() == null) entity.setExistingCoverCi(BigDecimal.ZERO);
        if (entity.getExistingCoverLife() == null) entity.setExistingCoverLife(BigDecimal.ZERO);
        return entity;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
