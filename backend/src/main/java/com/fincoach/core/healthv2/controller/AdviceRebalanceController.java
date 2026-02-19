package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.AdviceRebalanceResponseDTO;
import com.fincoach.core.healthv2.service.AdviceRebalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advice")
@Tag(name = "Advice", description = "Advice & Rebalance")
public class AdviceRebalanceController {

    private final AdviceRebalanceService adviceRebalanceService;

    public AdviceRebalanceController(AdviceRebalanceService adviceRebalanceService) {
        this.adviceRebalanceService = adviceRebalanceService;
    }

    @GetMapping("/rebalance")
    @Operation(summary = "获取再平衡建议")
    public Result<AdviceRebalanceResponseDTO> rebalance() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(adviceRebalanceService.buildRebalanceAdvice(userId));
    }
}
