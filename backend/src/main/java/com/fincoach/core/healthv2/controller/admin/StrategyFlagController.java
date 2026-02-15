package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcStrategyFlagEntity;
import com.fincoach.core.healthv2.mapper.FcStrategyFlagMapper;
import com.fincoach.core.healthv2.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/strategy-flags")
@Tag(name = "Admin-StrategyFlag", description = "策略开关管理")
public class StrategyFlagController {

    @Autowired
    private FcStrategyFlagMapper flagMapper;
    @Autowired
    private AuditService auditService;

    @GetMapping
    @Operation(summary = "全部开关")
    public Result<List<FcStrategyFlagEntity>> list() {
        return Result.success(flagMapper.selectList(null));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换开关")
    public Result<String> toggle(@PathVariable Long id) {
        FcStrategyFlagEntity entity = flagMapper.selectById(id);
        if (entity == null) return Result.error(404, "开关不存在");
        boolean oldVal = entity.getEnabled();
        entity.setEnabled(!oldVal);
        entity.setUpdatedAt(LocalDateTime.now());
        flagMapper.updateById(entity);
        auditService.log(UserContext.getCurrentUserId(), "TOGGLE_FLAG", "STRATEGY_FLAG", id, oldVal, entity.getEnabled());
        log.info("[Admin] 策略开关: {}={}, by={}", entity.getFlagKey(), entity.getEnabled(), UserContext.getCurrentUserId());
        return Result.success(entity.getFlagKey() + " -> " + (entity.getEnabled() ? "启用" : "停用"));
    }
}
