package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcAdviceTemplateEntity;
import com.fincoach.core.healthv2.mapper.FcAdviceTemplateMapper;
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
@RequestMapping("/api/admin/advice-templates")
@Tag(name = "Admin-AdviceTemplate", description = "建议文案模板管理")
public class AdviceTemplateController {

    @Autowired
    private FcAdviceTemplateMapper templateMapper;
    @Autowired
    private AuditService auditService;
    @Autowired
    private ConfigJsonHelper configJsonHelper;

    @GetMapping
    @Operation(summary = "全部模板")
    public Result<List<FcAdviceTemplateEntity>> list() {
        List<FcAdviceTemplateEntity> list = templateMapper.selectList(null);
        Long actorUserId = UserContext.getCurrentUserId();
        for (FcAdviceTemplateEntity entity : list) {
            entity.setTemplateText(configJsonHelper.parseOrDefault(
                    entity.getTemplateText(),
                    HealthV2ConfigDefaults.defaultAdviceTemplate(entity.getSceneKey()),
                    actorUserId,
                    "ADVICE_TEMPLATE",
                    entity.getId(),
                    "templateText"));
        }
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "创建模板")
    public Result<FcAdviceTemplateEntity> create(@RequestBody FcAdviceTemplateEntity dto) {
        try {
            configJsonHelper.validateJsonOrThrow(dto.getTemplateText(), "templateText");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
        dto.setId(null);
        dto.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(dto);
        auditService.log(UserContext.getCurrentUserId(), "CREATE_TEMPLATE", "ADVICE_TEMPLATE", dto.getId(), null, dto);
        return Result.success(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模板")
    public Result<String> update(@PathVariable Long id, @RequestBody FcAdviceTemplateEntity dto) {
        FcAdviceTemplateEntity existing = templateMapper.selectById(id);
        if (existing == null) return Result.error(404, "模板不存在");
        try {
            configJsonHelper.validateJsonOrThrow(dto.getTemplateText(), "templateText");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
        dto.setId(id);
        dto.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(dto);
        auditService.log(UserContext.getCurrentUserId(), "UPDATE_TEMPLATE", "ADVICE_TEMPLATE", id, existing, dto);
        return Result.success("更新成功");
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "启用/停用模板")
    public Result<String> toggle(@PathVariable Long id) {
        FcAdviceTemplateEntity entity = templateMapper.selectById(id);
        if (entity == null) return Result.error(404, "模板不存在");
        entity.setEnabled(!entity.getEnabled());
        entity.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(entity);
        auditService.log(UserContext.getCurrentUserId(), "TOGGLE_TEMPLATE", "ADVICE_TEMPLATE", id, null, entity.getEnabled());
        return Result.success(entity.getEnabled() ? "已启用" : "已停用");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板")
    public Result<String> delete(@PathVariable Long id) {
        templateMapper.deleteById(id);
        auditService.log(UserContext.getCurrentUserId(), "DELETE_TEMPLATE", "ADVICE_TEMPLATE", id, null, null);
        return Result.success("已删除");
    }
}
