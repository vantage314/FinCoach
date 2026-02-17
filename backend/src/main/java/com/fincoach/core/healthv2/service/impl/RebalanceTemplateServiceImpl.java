package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.entity.FcRebalanceTemplateEntity;
import com.fincoach.core.healthv2.mapper.FcRebalanceTemplateMapper;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateDefaults;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateRegistry;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateSnapshot;
import com.fincoach.core.healthv2.service.RebalanceTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class RebalanceTemplateServiceImpl implements RebalanceTemplateService {

    private final FcRebalanceTemplateMapper templateMapper;
    private final RebalanceTemplateRegistry registry;

    public RebalanceTemplateServiceImpl(FcRebalanceTemplateMapper templateMapper,
                                        RebalanceTemplateRegistry registry) {
        this.templateMapper = templateMapper;
        this.registry = registry;
    }

    @Override
    public List<FcRebalanceTemplateEntity> list() {
        return templateMapper.selectList(
                new LambdaQueryWrapper<FcRebalanceTemplateEntity>()
                        .orderByDesc(FcRebalanceTemplateEntity::getCreatedAt));
    }

    @Override
    @Transactional
    public Long save(FcRebalanceTemplateEntity dto) {
        if (dto == null) return null;
        LocalDateTime now = LocalDateTime.now();
        if (dto.getId() != null) {
            FcRebalanceTemplateEntity existing = templateMapper.selectById(dto.getId());
            if (existing == null) {
                return null;
            }
            dto.setUpdatedAt(now);
            if (dto.getTemplateJson() == null) {
                dto.setTemplateJson(existing.getTemplateJson());
            }
            templateMapper.updateById(dto);
            return dto.getId();
        }

        String code = dto.getCode() == null || dto.getCode().isBlank()
                ? RebalanceTemplateDefaults.DEFAULT_CODE
                : dto.getCode().trim();
        Integer maxVersion = templateMapper.selectMaxVersion(code);
        int nextVersion = maxVersion == null ? 1 : maxVersion + 1;

        FcRebalanceTemplateEntity entity = new FcRebalanceTemplateEntity();
        entity.setCode(code);
        entity.setName(dto.getName() == null ? RebalanceTemplateDefaults.DEFAULT_NAME : dto.getName());
        entity.setVersion(nextVersion);
        entity.setEnabled(0);
        entity.setTemplateJson(dto.getTemplateJson());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        templateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void publish(Long templateId) {
        if (templateId == null) return;
        FcRebalanceTemplateEntity target = templateMapper.selectById(templateId);
        if (target == null) return;

        LocalDateTime now = LocalDateTime.now();
        templateMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FcRebalanceTemplateEntity>()
                        .eq(FcRebalanceTemplateEntity::getEnabled, 1)
                        .set(FcRebalanceTemplateEntity::getEnabled, 0)
                        .set(FcRebalanceTemplateEntity::getUpdatedAt, now));

        target.setEnabled(1);
        target.setPublishedAt(now);
        target.setUpdatedAt(now);
        templateMapper.updateById(target);

        registry.reload();
        log.info("[RebalanceTemplate] published: code={}, version={}", target.getCode(), target.getVersion());
    }

    @Override
    public void reload() {
        registry.reload();
    }

    @Override
    public RebalanceTemplateSnapshot getActiveSnapshot() {
        return registry.getActive();
    }
}
