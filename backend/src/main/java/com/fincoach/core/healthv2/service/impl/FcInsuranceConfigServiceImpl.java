package com.fincoach.core.healthv2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.healthv2.dto.InsuranceConfigUpsertDTO;
import com.fincoach.core.healthv2.entity.FcInsuranceConfigEntity;
import com.fincoach.core.healthv2.mapper.FcInsuranceConfigMapper;
import com.fincoach.core.healthv2.service.FcInsuranceConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class FcInsuranceConfigServiceImpl implements FcInsuranceConfigService {

    private static final String DEFAULT_CODE = "DEFAULT";

    @Autowired
    private FcInsuranceConfigMapper configMapper;

    @Override
    public FcInsuranceConfigEntity getDefaultConfig() {
        FcInsuranceConfigEntity entity = selectDefault();
        if (entity == null) {
            return defaultConfig();
        }
        return fillDefaults(entity);
    }

    @Override
    public FcInsuranceConfigEntity upsertDefault(InsuranceConfigUpsertDTO dto) {
        FcInsuranceConfigEntity entity = selectDefault();
        if (entity == null) {
            entity = new FcInsuranceConfigEntity();
            entity.setCode(DEFAULT_CODE);
            applyDto(entity, dto);
            configMapper.insert(entity);
            return fillDefaults(entity);
        }
        applyDto(entity, dto);
        configMapper.updateById(entity);
        return fillDefaults(entity);
    }

    private FcInsuranceConfigEntity selectDefault() {
        LambdaQueryWrapper<FcInsuranceConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FcInsuranceConfigEntity::getCode, DEFAULT_CODE);
        return configMapper.selectOne(wrapper);
    }

    private void applyDto(FcInsuranceConfigEntity entity, InsuranceConfigUpsertDTO dto) {
        if (dto.getTargetMedical() != null) entity.setTargetMedical(dto.getTargetMedical());
        if (dto.getTargetAccident() != null) entity.setTargetAccident(dto.getTargetAccident());
        if (dto.getTargetCi() != null) entity.setTargetCi(dto.getTargetCi());
        if (dto.getTargetLifeMultiplier() != null) entity.setTargetLifeMultiplier(dto.getTargetLifeMultiplier());
        if (dto.getPremiumRatioWarn() != null) entity.setPremiumRatioWarn(dto.getPremiumRatioWarn());
        if (dto.getPremiumRatioDanger() != null) entity.setPremiumRatioDanger(dto.getPremiumRatioDanger());
    }

    private FcInsuranceConfigEntity defaultConfig() {
        FcInsuranceConfigEntity entity = new FcInsuranceConfigEntity();
        entity.setCode(DEFAULT_CODE);
        entity.setTargetMedical(new BigDecimal("500000"));
        entity.setTargetAccident(new BigDecimal("500000"));
        entity.setTargetCi(new BigDecimal("500000"));
        entity.setTargetLifeMultiplier(new BigDecimal("5"));
        entity.setPremiumRatioWarn(new BigDecimal("0.10"));
        entity.setPremiumRatioDanger(new BigDecimal("0.20"));
        return entity;
    }

    private FcInsuranceConfigEntity fillDefaults(FcInsuranceConfigEntity entity) {
        if (entity.getTargetMedical() == null) entity.setTargetMedical(new BigDecimal("500000"));
        if (entity.getTargetAccident() == null) entity.setTargetAccident(new BigDecimal("500000"));
        if (entity.getTargetCi() == null) entity.setTargetCi(new BigDecimal("500000"));
        if (entity.getTargetLifeMultiplier() == null) entity.setTargetLifeMultiplier(new BigDecimal("5"));
        if (entity.getPremiumRatioWarn() == null) entity.setPremiumRatioWarn(new BigDecimal("0.10"));
        if (entity.getPremiumRatioDanger() == null) entity.setPremiumRatioDanger(new BigDecimal("0.20"));
        return entity;
    }
}
