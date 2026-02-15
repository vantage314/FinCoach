package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.dto.CreateGoalDTO;
import com.fincoach.core.healthv2.dto.UpdateGoalDTO;
import com.fincoach.core.healthv2.entity.FcGoalEntity;
import com.fincoach.core.healthv2.mapper.FcGoalMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.service.FcGoalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 体检v2-目标服务实现
 */
@Slf4j
@Service
public class FcGoalServiceImpl implements FcGoalService {

    @Autowired
    private FcGoalMapper goalMapper;

    @Autowired
    private AuditService auditService;

    @Override
    public FcGoalEntity create(Long userId, CreateGoalDTO dto) {
        log.info("[HealthV2-Goal] 创建目标, userId={}, type={}", userId, dto.getType());

        FcGoalEntity entity = new FcGoalEntity();
        entity.setUserId(userId);
        entity.setType(dto.getType());
        entity.setTargetAmount(dto.getTargetAmount());
        entity.setTargetDate(dto.getTargetDate() != null ? LocalDate.parse(dto.getTargetDate()) : null);
        entity.setCurrentSaved(dto.getCurrentSaved() != null ? dto.getCurrentSaved() : java.math.BigDecimal.ZERO);
        entity.setMonthlyPlan(dto.getMonthlyPlan());
        entity.setRiskLevelSuggestion(dto.getRiskLevelSuggestion());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        goalMapper.insert(entity);
        auditService.log(userId, "CREATE", "FC_GOAL", entity.getId(), null, entity);
        return entity;
    }

    @Override
    public FcGoalEntity update(Long userId, UpdateGoalDTO dto) {
        log.info("[HealthV2-Goal] 更新目标, userId={}, id={}", userId, dto.getId());

        FcGoalEntity existing = goalMapper.selectById(dto.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("目标不存在或无权操作");
        }

        FcGoalEntity before = new FcGoalEntity();
        before.setId(existing.getId());
        before.setType(existing.getType());
        before.setTargetAmount(existing.getTargetAmount());

        if (dto.getType() != null) existing.setType(dto.getType());
        if (dto.getTargetAmount() != null) existing.setTargetAmount(dto.getTargetAmount());
        if (dto.getTargetDate() != null) existing.setTargetDate(LocalDate.parse(dto.getTargetDate()));
        if (dto.getCurrentSaved() != null) existing.setCurrentSaved(dto.getCurrentSaved());
        if (dto.getMonthlyPlan() != null) existing.setMonthlyPlan(dto.getMonthlyPlan());
        if (dto.getRiskLevelSuggestion() != null) existing.setRiskLevelSuggestion(dto.getRiskLevelSuggestion());
        existing.setUpdateTime(LocalDateTime.now());

        goalMapper.updateById(existing);
        auditService.log(userId, "UPDATE", "FC_GOAL", existing.getId(), before, existing);
        return existing;
    }

    @Override
    public List<FcGoalEntity> listByUserId(Long userId) {
        LambdaQueryWrapper<FcGoalEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcGoalEntity::getUserId, userId)
               .orderByDesc(FcGoalEntity::getUpdateTime);
        return goalMapper.selectList(wrapper);
    }

    @Override
    public void delete(Long userId, Long id) {
        log.info("[HealthV2-Goal] 删除目标, userId={}, id={}", userId, id);

        FcGoalEntity existing = goalMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("目标不存在或无权操作");
        }

        goalMapper.deleteById(id);
        auditService.log(userId, "DELETE", "FC_GOAL", id, existing, null);
    }
}
