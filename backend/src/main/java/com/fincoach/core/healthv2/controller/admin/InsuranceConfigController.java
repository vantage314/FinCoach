package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcInsuranceParamEntity;
import com.fincoach.core.healthv2.mapper.FcInsuranceParamMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.util.ConfigJsonHelper;
import com.fincoach.core.healthv2.util.HealthV2ConfigDefaults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/insurance-config")
@Tag(name = "Admin-InsuranceConfig", description = "保险参数配置")
public class InsuranceConfigController {

    @Autowired
    private FcInsuranceParamMapper paramMapper;
    @Autowired
    private AuditService auditService;
    @Autowired
    private ConfigJsonHelper configJsonHelper;

    @GetMapping
    @Operation(summary = "全部参数")
    public Result<List<FcInsuranceParamEntity>> list() {
        List<FcInsuranceParamEntity> list = paramMapper.selectList(null);
        Long actorUserId = UserContext.getCurrentUserId();
        for (FcInsuranceParamEntity entity : list) {
            entity.setValueJson(configJsonHelper.parseOrDefault(
                    entity.getValueJson(),
                    HealthV2ConfigDefaults.defaultInsuranceParam(entity.getParamKey()),
                    actorUserId,
                    "INSURANCE_PARAM",
                    entity.getId(),
                    "valueJson"));
        }
        return Result.success(list);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新参数")
    public Result<String> update(@PathVariable Long id, @RequestBody FcInsuranceParamEntity dto) {
        FcInsuranceParamEntity existing = paramMapper.selectById(id);
        if (existing == null) return Result.error(404, "参数不存在");
        try {
            configJsonHelper.validateJsonOrThrow(dto.getValueJson(), "valueJson");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
        dto.setId(id);
        dto.setUpdatedAt(LocalDateTime.now());
        paramMapper.updateById(dto);
        auditService.log(UserContext.getCurrentUserId(), "UPDATE_INSURANCE_PARAM", "INSURANCE_PARAM", id, existing, dto);
        return Result.success("更新成功");
    }

    @PostMapping
    @Operation(summary = "新增参数")
    public Result<FcInsuranceParamEntity> create(@RequestBody FcInsuranceParamEntity dto) {
        try {
            configJsonHelper.validateJsonOrThrow(dto.getValueJson(), "valueJson");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
        dto.setId(null);
        dto.setUpdatedAt(LocalDateTime.now());
        paramMapper.insert(dto);
        auditService.log(UserContext.getCurrentUserId(), "CREATE_INSURANCE_PARAM", "INSURANCE_PARAM", dto.getId(), null, dto);
        return Result.success(dto);
    }
}
