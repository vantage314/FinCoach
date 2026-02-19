package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.healthv2.dto.admin.AdminAdviceRuleParamDTO;
import com.fincoach.core.healthv2.dto.admin.AdminAdviceRuleSetDTO;
import com.fincoach.core.healthv2.entity.FcAdviceRuleParamEntity;
import com.fincoach.core.healthv2.entity.FcAdviceRuleSetEntity;
import com.fincoach.core.healthv2.service.AdviceRuleSetService;
import com.fincoach.core.rbac.RbacPermissionCodes;
import com.fincoach.core.security.AdminOnly;
import com.fincoach.core.security.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/api/advice/rulesets")
@Tag(name = "Admin-Advice-Rules", description = "Advice 规则集管理")
@AdminOnly
public class AdminAdviceRuleSetController {

    private final AdviceRuleSetService service;

    public AdminAdviceRuleSetController(AdviceRuleSetService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "查询规则集列表")
    @Permission(RbacPermissionCodes.ADMIN_SCORE_RULE_VIEW)
    public Result<List<AdminAdviceRuleSetDTO>> list() {
        List<FcAdviceRuleSetEntity> list = service.list();
        List<AdminAdviceRuleSetDTO> result = new ArrayList<>();
        for (FcAdviceRuleSetEntity e : list) {
            result.add(toDto(e));
        }
        return Result.success(result);
    }

    @PostMapping
    @Operation(summary = "创建规则集")
    @Permission(RbacPermissionCodes.ADMIN_SCORE_RULE_EDIT)
    public Result<Long> create(@RequestBody AdminAdviceRuleSetDTO request) {
        FcAdviceRuleSetEntity entity = new FcAdviceRuleSetEntity();
        if (request != null) {
            entity.setCode(request.getCode());
            entity.setDescription(request.getDescription());
            entity.setVersion(request.getVersion());
        }
        Long id = service.create(entity);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新规则集元信息")
    @Permission(RbacPermissionCodes.ADMIN_SCORE_RULE_EDIT)
    public Result<String> update(@PathVariable Long id, @RequestBody AdminAdviceRuleSetDTO request) {
        if (id == null) return Result.error(400, "id 不能为空");
        FcAdviceRuleSetEntity entity = new FcAdviceRuleSetEntity();
        if (request != null) {
            entity.setCode(request.getCode());
            entity.setDescription(request.getDescription());
            entity.setVersion(request.getVersion());
        }
        service.updateMeta(id, entity);
        return Result.success("updated");
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用规则集")
    @Permission(RbacPermissionCodes.ADMIN_SCORE_RULE_PUBLISH)
    public Result<String> enable(@PathVariable Long id) {
        if (id == null) return Result.error(400, "id 不能为空");
        service.enable(id);
        return Result.success("enabled");
    }

    @GetMapping("/{code}/params")
    @Operation(summary = "获取规则集参数")
    @Permission(RbacPermissionCodes.ADMIN_SCORE_RULE_VIEW)
    public Result<List<AdminAdviceRuleParamDTO>> params(@PathVariable String code) {
        List<FcAdviceRuleParamEntity> params = service.listParams(code);
        List<AdminAdviceRuleParamDTO> result = new ArrayList<>();
        for (FcAdviceRuleParamEntity p : params) {
            result.add(toParamDto(p));
        }
        return Result.success(result);
    }

    @PutMapping("/{code}/params")
    @Operation(summary = "保存规则集参数")
    @Permission(RbacPermissionCodes.ADMIN_SCORE_RULE_EDIT)
    public Result<String> saveParams(@PathVariable String code, @RequestBody List<AdminAdviceRuleParamDTO> params) {
        List<FcAdviceRuleParamEntity> entities = toParamEntities(params);
        service.upsertParams(code, entities);
        return Result.success("saved");
    }

    private AdminAdviceRuleSetDTO toDto(FcAdviceRuleSetEntity e) {
        AdminAdviceRuleSetDTO dto = new AdminAdviceRuleSetDTO();
        if (e == null) return dto;
        dto.setId(e.getId());
        dto.setCode(e.getCode());
        dto.setVersion(e.getVersion());
        dto.setEnabled(e.getEnabled());
        dto.setDescription(e.getDescription());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    private AdminAdviceRuleParamDTO toParamDto(FcAdviceRuleParamEntity e) {
        AdminAdviceRuleParamDTO dto = new AdminAdviceRuleParamDTO();
        if (e == null) return dto;
        dto.setId(e.getId());
        dto.setRuleSetCode(e.getRuleSetCode());
        dto.setParamKey(e.getParamKey());
        dto.setParamValue(e.getParamValue());
        dto.setValueType(e.getValueType());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    private List<FcAdviceRuleParamEntity> toParamEntities(List<AdminAdviceRuleParamDTO> params) {
        List<FcAdviceRuleParamEntity> result = new ArrayList<>();
        if (params == null) return result;
        for (AdminAdviceRuleParamDTO p : params) {
            if (p == null || p.getParamKey() == null) continue;
            FcAdviceRuleParamEntity entity = new FcAdviceRuleParamEntity();
            entity.setParamKey(p.getParamKey());
            entity.setParamValue(p.getParamValue());
            entity.setValueType(p.getValueType());
            result.add(entity);
        }
        return result;
    }
}
