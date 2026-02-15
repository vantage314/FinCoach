package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.common.Result;
import com.fincoach.core.healthv2.entity.FcAuditLogEntity;
import com.fincoach.core.healthv2.mapper.FcAuditLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/audit-logs")
@Tag(name = "Admin-AuditLog", description = "审计日志查询")
public class AuditLogController {

    @Autowired
    private FcAuditLogMapper auditLogMapper;

    @GetMapping
    @Operation(summary = "审计日志（分页+筛选）")
    public Result<IPage<FcAuditLogEntity>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<FcAuditLogEntity> qw = new LambdaQueryWrapper<>();
        if (userId != null) qw.eq(FcAuditLogEntity::getActorUserId, userId);
        if (action != null) qw.eq(FcAuditLogEntity::getAction, action);
        if (targetType != null) qw.eq(FcAuditLogEntity::getTargetType, targetType);
        qw.orderByDesc(FcAuditLogEntity::getCreatedAt);
        return Result.success(auditLogMapper.selectPage(new Page<>(page, size), qw));
    }
}
