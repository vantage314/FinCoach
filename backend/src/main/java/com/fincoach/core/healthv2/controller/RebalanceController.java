package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.HealthReportV2VO;
import com.fincoach.core.healthv2.dto.RebalanceConfirmRequestDTO;
import com.fincoach.core.healthv2.service.BehaviorEventService;
import com.fincoach.core.healthv2.service.HealthReportV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 体检v2-再平衡确认控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/app/rebalance")
@Tag(name = "体检v2-再平衡", description = "再平衡建议确认")
public class RebalanceController {

    @Autowired
    private BehaviorEventService behaviorEventService;
    @Autowired
    private HealthReportV2Service healthReportV2Service;

    @PostMapping("/confirm")
    @Operation(summary = "确认再平衡建议", description = "用户确认执行再平衡建议，记录行为事件")
    public Result<String> confirm(@Valid @RequestBody RebalanceConfirmRequestDTO body) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        HealthReportV2VO report = healthReportV2Service.getById(body.getReportId());
        if (report == null) {
            return Result.error(404, "报告不存在");
        }
        if (!userId.equals(report.getUserId())) {
            return Result.error(403, "无权访问此报告");
        }
        String expectedHash = extractActionsHash(report);
        if (expectedHash == null || !expectedHash.equals(body.getActionsHash())) {
            return Result.error(400, "REBALANCE_HASH_MISMATCH");
        }

        BigDecimal amount = BigDecimal.ZERO;
        if (body.getExecutedActions() != null) {
            for (RebalanceConfirmRequestDTO.ExecutedAction action : body.getExecutedActions()) {
                if (action != null && action.getAmount() != null) {
                    amount = amount.add(action.getAmount());
                }
            }
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            amount = null;
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("reportId", body.getReportId());
        meta.put("notes", body.getNotes());
        meta.put("executedActions", body.getExecutedActions());
        meta.put("actionsHash", body.getActionsHash());

        behaviorEventService.recordEvent(userId, "REBALANCE_CONFIRM", amount, meta);

        log.info("[Rebalance] 用户确认再平衡: userId={}, reportId={}", userId, body.getReportId());
        return Result.success("再平衡确认已记录");
    }

    @SuppressWarnings("unchecked")
    private String extractActionsHash(HealthReportV2VO report) {
        if (report == null || report.getAdvice() == null) {
            return null;
        }
        Object rebalance = report.getAdvice().get("rebalance");
        if (!(rebalance instanceof Map<?, ?> rebalanceMap)) {
            return null;
        }
        Object hash = ((Map<String, Object>) rebalanceMap).get("actionsHash");
        return hash == null ? null : hash.toString();
    }
}
