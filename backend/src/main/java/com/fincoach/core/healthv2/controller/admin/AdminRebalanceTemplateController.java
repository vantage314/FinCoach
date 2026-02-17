package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.healthv2.dto.admin.AdminRebalanceTemplateDTO;
import com.fincoach.core.healthv2.dto.admin.AdminRebalanceTemplatePublishRequest;
import com.fincoach.core.healthv2.dto.admin.AdminRebalanceTemplateSaveRequest;
import com.fincoach.core.healthv2.entity.FcRebalanceTemplateEntity;
import com.fincoach.core.healthv2.service.RebalanceTemplateService;
import com.fincoach.core.rbac.RbacPermissionCodes;
import com.fincoach.core.security.AdminOnly;
import com.fincoach.core.security.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/rebalance-templates")
@Tag(name = "Admin-Rebalance-Template", description = "再平衡模板管理")
@AdminOnly
public class AdminRebalanceTemplateController {

    private final RebalanceTemplateService service;

    public AdminRebalanceTemplateController(RebalanceTemplateService service) {
        this.service = service;
    }

    @GetMapping("/list")
    @Operation(summary = "查询模板列表")
    @Permission(RbacPermissionCodes.ADMIN_REBALANCE_TEMPLATE_VIEW)
    public Result<List<AdminRebalanceTemplateDTO>> list() {
        List<FcRebalanceTemplateEntity> entities = service.list();
        List<AdminRebalanceTemplateDTO> result = new ArrayList<>();
        for (FcRebalanceTemplateEntity e : entities) {
            result.add(toDto(e));
        }
        return Result.success(result);
    }

    @PostMapping("/save")
    @Operation(summary = "新增/更新模板")
    @Permission(RbacPermissionCodes.ADMIN_REBALANCE_TEMPLATE_EDIT)
    public Result<Long> save(@RequestBody AdminRebalanceTemplateSaveRequest request) {
        FcRebalanceTemplateEntity entity = new FcRebalanceTemplateEntity();
        if (request != null) {
            entity.setId(request.getId());
            entity.setCode(request.getCode());
            entity.setName(request.getName());
            entity.setTemplateJson(request.getTemplateJson());
        }
        Long id = service.save(entity);
        return Result.success(id);
    }

    @PostMapping("/publish")
    @Operation(summary = "发布模板")
    @Permission(RbacPermissionCodes.ADMIN_REBALANCE_TEMPLATE_PUBLISH)
    public Result<String> publish(@RequestBody AdminRebalanceTemplatePublishRequest request) {
        if (request == null || request.getTemplateId() == null) {
            return Result.error(400, "templateId 不能为空");
        }
        service.publish(request.getTemplateId());
        return Result.success("published");
    }

    @PostMapping("/reload")
    @Operation(summary = "手动重载模板快照")
    @Permission(RbacPermissionCodes.ADMIN_REBALANCE_TEMPLATE_EDIT)
    public Result<String> reload() {
        service.reload();
        return Result.success("reloaded");
    }

    private AdminRebalanceTemplateDTO toDto(FcRebalanceTemplateEntity e) {
        AdminRebalanceTemplateDTO dto = new AdminRebalanceTemplateDTO();
        if (e == null) return dto;
        dto.setId(e.getId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setVersion(e.getVersion());
        dto.setEnabled(e.getEnabled());
        dto.setTemplateJson(e.getTemplateJson());
        dto.setPublishedAt(e.getPublishedAt());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}
