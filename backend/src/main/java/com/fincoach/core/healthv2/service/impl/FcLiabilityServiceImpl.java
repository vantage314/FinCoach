package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.dto.CreateLiabilityDTO;
import com.fincoach.core.healthv2.dto.UpdateLiabilityDTO;
import com.fincoach.core.healthv2.entity.FcLiabilityEntity;
import com.fincoach.core.healthv2.mapper.FcLiabilityMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.service.FcLiabilityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 体检v2-负债服务实现
 */
@Slf4j
@Service
public class FcLiabilityServiceImpl implements FcLiabilityService {

    @Autowired
    private FcLiabilityMapper liabilityMapper;

    @Autowired
    private AuditService auditService;

    @Override
    public FcLiabilityEntity create(Long userId, CreateLiabilityDTO dto) {
        log.info("[HealthV2-Liability] 创建负债, userId={}, type={}", userId, dto.getType());

        FcLiabilityEntity entity = new FcLiabilityEntity();
        entity.setUserId(userId);
        entity.setType(dto.getType());
        entity.setPrincipal(dto.getPrincipal());
        entity.setInterestRate(normalizeInterestRate(dto.getInterestRate()));
        entity.setRemainingMonths(dto.getRemainingMonths());
        entity.setMonthlyPayment(dto.getMonthlyPayment());
        entity.setPrepayPenaltyJson(dto.getPrepayPenaltyJson());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        liabilityMapper.insert(entity);
        auditService.log(userId, "CREATE", "FC_LIABILITY", entity.getId(), null, entity);
        return entity;
    }

    @Override
    public FcLiabilityEntity update(Long userId, UpdateLiabilityDTO dto) {
        log.info("[HealthV2-Liability] 更新负债, userId={}, id={}", userId, dto.getId());

        FcLiabilityEntity existing = liabilityMapper.selectById(dto.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("负债不存在或无权操作");
        }

        FcLiabilityEntity before = new FcLiabilityEntity();
        before.setId(existing.getId());
        before.setType(existing.getType());
        before.setPrincipal(existing.getPrincipal());

        if (dto.getType() != null) existing.setType(dto.getType());
        if (dto.getPrincipal() != null) existing.setPrincipal(dto.getPrincipal());
        if (dto.getInterestRate() != null) existing.setInterestRate(normalizeInterestRate(dto.getInterestRate()));
        if (dto.getRemainingMonths() != null) existing.setRemainingMonths(dto.getRemainingMonths());
        if (dto.getMonthlyPayment() != null) existing.setMonthlyPayment(dto.getMonthlyPayment());
        if (dto.getPrepayPenaltyJson() != null) existing.setPrepayPenaltyJson(dto.getPrepayPenaltyJson());
        existing.setUpdateTime(LocalDateTime.now());

        liabilityMapper.updateById(existing);
        auditService.log(userId, "UPDATE", "FC_LIABILITY", existing.getId(), before, existing);
        return existing;
    }

    @Override
    public List<FcLiabilityEntity> listByUserId(Long userId) {
        LambdaQueryWrapper<FcLiabilityEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcLiabilityEntity::getUserId, userId)
               .orderByDesc(FcLiabilityEntity::getUpdateTime);
        return liabilityMapper.selectList(wrapper);
    }

    @Override
    public void delete(Long userId, Long id) {
        log.info("[HealthV2-Liability] 删除负债, userId={}, id={}", userId, id);

        FcLiabilityEntity existing = liabilityMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("负债不存在或无权操作");
        }

        liabilityMapper.deleteById(id);
        auditService.log(userId, "DELETE", "FC_LIABILITY", id, existing, null);
    }

    /**
     * 利率归一化：落库统一为小数制 (0~1)
     * 兼容百分制输入（如 4.2 -> 0.042），M1 兼容期 warn 日志
     */
    private BigDecimal normalizeInterestRate(BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            BigDecimal normalized = rate.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
            log.warn("[HealthV2-Liability] interestRate 以百分制输入({})，已自动归一为小数制({})", rate, normalized);
            return normalized;
        }
        return rate;
    }
}
