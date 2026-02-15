package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.dto.CreateAssetDTO;
import com.fincoach.core.healthv2.dto.UpdateAssetDTO;
import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.mapper.FcAssetMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.service.FcAssetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 体检v2-资产服务实现
 */
@Slf4j
@Service
public class FcAssetServiceImpl implements FcAssetService {

    @Autowired
    private FcAssetMapper assetMapper;

    @Autowired
    private AuditService auditService;

    @Override
    public FcAssetEntity create(Long userId, CreateAssetDTO dto) {
        log.info("[HealthV2-Asset] 创建资产, userId={}, type={}, name={}", userId, dto.getType(), dto.getName());

        FcAssetEntity entity = new FcAssetEntity();
        entity.setUserId(userId);
        entity.setType(dto.getType());
        entity.setName(dto.getName());
        entity.setAmount(dto.getAmount());
        entity.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "CNY");
        entity.setRiskLevel(dto.getRiskLevel());
        entity.setAsOfDate(dto.getAsOfDate() != null ? LocalDate.parse(dto.getAsOfDate()) : null);
        entity.setMetaJson(dto.getMetaJson());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        assetMapper.insert(entity);

        auditService.log(userId, "CREATE", "FC_ASSET", entity.getId(), null, entity);
        return entity;
    }

    @Override
    public FcAssetEntity update(Long userId, UpdateAssetDTO dto) {
        log.info("[HealthV2-Asset] 更新资产, userId={}, assetId={}", userId, dto.getId());

        FcAssetEntity existing = assetMapper.selectById(dto.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("资产不存在或无权操作");
        }

        FcAssetEntity before = new FcAssetEntity();
        // 浅拷贝关键字段用于审计
        before.setId(existing.getId());
        before.setType(existing.getType());
        before.setName(existing.getName());
        before.setAmount(existing.getAmount());

        if (dto.getType() != null) existing.setType(dto.getType());
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getAmount() != null) existing.setAmount(dto.getAmount());
        if (dto.getCurrency() != null) existing.setCurrency(dto.getCurrency());
        if (dto.getRiskLevel() != null) existing.setRiskLevel(dto.getRiskLevel());
        if (dto.getAsOfDate() != null) existing.setAsOfDate(LocalDate.parse(dto.getAsOfDate()));
        if (dto.getMetaJson() != null) existing.setMetaJson(dto.getMetaJson());
        existing.setUpdateTime(LocalDateTime.now());

        assetMapper.updateById(existing);

        auditService.log(userId, "UPDATE", "FC_ASSET", existing.getId(), before, existing);
        return existing;
    }

    @Override
    public List<FcAssetEntity> listByUserId(Long userId) {
        LambdaQueryWrapper<FcAssetEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcAssetEntity::getUserId, userId)
               .orderByDesc(FcAssetEntity::getUpdateTime);
        return assetMapper.selectList(wrapper);
    }

    @Override
    public void delete(Long userId, Long id) {
        log.info("[HealthV2-Asset] 删除资产, userId={}, assetId={}", userId, id);

        FcAssetEntity existing = assetMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("资产不存在或无权操作");
        }

        assetMapper.deleteById(id);
        auditService.log(userId, "DELETE", "FC_ASSET", id, existing, null);
    }
}
