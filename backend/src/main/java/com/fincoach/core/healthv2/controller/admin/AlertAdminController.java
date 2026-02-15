package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcAlertRecordEntity;
import com.fincoach.core.healthv2.entity.FcAlertRuleEntity;
import com.fincoach.core.healthv2.mapper.FcAlertRecordMapper;
import com.fincoach.core.healthv2.mapper.FcAlertRuleMapper;
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
@RequestMapping("/api/admin/alerts")
@Tag(name = "Admin-Alerts", description = "预警规则与记录管理")
public class AlertAdminController {

    @Autowired
    private FcAlertRuleMapper ruleMapper;
    @Autowired
    private FcAlertRecordMapper recordMapper;
    @Autowired
    private AuditService auditService;
    @Autowired
    private ConfigJsonHelper configJsonHelper;

    // ===== 规则管理 =====

    @GetMapping("/rules")
    @Operation(summary = "全部预警规则")
    public Result<List<FcAlertRuleEntity>> listRules() {
        List<FcAlertRuleEntity> list = ruleMapper.selectList(null);
        Long actorUserId = UserContext.getCurrentUserId();
        for (FcAlertRuleEntity rule : list) {
            rule.setThresholdsJson(configJsonHelper.parseOrDefault(
                    rule.getThresholdsJson(),
                    HealthV2ConfigDefaults.defaultAlertThresholds(rule.getRuleKey()),
                    actorUserId,
                    "ALERT_RULE",
                    rule.getId(),
                    "thresholdsJson"));
        }
        return Result.success(list);
    }

    @PutMapping("/rules/{id}")
    @Operation(summary = "更新预警规则")
    public Result<String> updateRule(@PathVariable Long id, @RequestBody FcAlertRuleEntity dto) {
        FcAlertRuleEntity existing = ruleMapper.selectById(id);
        if (existing == null) return Result.error(404, "规则不存在");
        try {
            configJsonHelper.validateJsonOrThrow(dto.getThresholdsJson(), "thresholdsJson");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
        dto.setId(id);
        dto.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(dto);
        auditService.log(UserContext.getCurrentUserId(), "UPDATE_ALERT_RULE", "ALERT_RULE", id, existing, dto);
        return Result.success("更新成功");
    }

    @PutMapping("/rules/{id}/toggle")
    @Operation(summary = "启用/停用规则")
    public Result<String> toggleRule(@PathVariable Long id) {
        FcAlertRuleEntity entity = ruleMapper.selectById(id);
        if (entity == null) return Result.error(404, "规则不存在");
        entity.setEnabled(!entity.getEnabled());
        entity.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(entity);
        auditService.log(UserContext.getCurrentUserId(), "TOGGLE_ALERT_RULE", "ALERT_RULE", id, null, entity.getEnabled());
        return Result.success(entity.getRuleKey() + " -> " + (entity.getEnabled() ? "启用" : "停用"));
    }

    // ===== 记录查询 =====

    @GetMapping("/records")
    @Operation(summary = "预警记录（分页）")
    public Result<IPage<FcAlertRecordEntity>> listRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String ruleKey,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<FcAlertRecordEntity> qw = new LambdaQueryWrapper<>();
        if (userId != null) qw.eq(FcAlertRecordEntity::getUserId, userId);
        if (ruleKey != null) qw.eq(FcAlertRecordEntity::getRuleKey, ruleKey);
        if (status != null) qw.eq(FcAlertRecordEntity::getStatus, status);
        qw.orderByDesc(FcAlertRecordEntity::getCreatedAt);
        return Result.success(recordMapper.selectPage(new Page<>(page, size), qw));
    }
}
