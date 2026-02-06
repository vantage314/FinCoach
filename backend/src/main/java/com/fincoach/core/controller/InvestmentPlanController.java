package com.fincoach.core.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.controller.vo.InvestmentPlanVO;
import com.fincoach.core.service.InvestmentPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 调仓计划控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/plan")
@Tag(name = "调仓计划", description = "智能调仓建议系统")
public class InvestmentPlanController {

    @Autowired
    private InvestmentPlanService planService;

    @PostMapping("/generate")
    @Operation(summary = "生成调仓计划", description = "支持 CONTRIBUTION (增量模式) 和 REBALANCE (存量模式)")
    public Result<InvestmentPlanVO> generatePlan(
            HttpServletRequest request,
            @RequestParam(defaultValue = "REBALANCE") String planType,
            @RequestParam(required = false) BigDecimal investMoney) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        InvestmentPlanVO plan = planService.generatePlan(userId, planType, investMoney);
        return Result.success(plan);
    }

    @PostMapping("/save")
    @Operation(summary = "保存调仓计划", description = "用户确认后保存计划到数据库")
    public Result<Long> savePlan(HttpServletRequest request, @RequestBody InvestmentPlanVO plan) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        Long planId = planService.savePlan(userId, plan);
        return Result.success(planId);
    }

    @GetMapping("/list")
    @Operation(summary = "查询计划列表", description = "获取用户历史调仓计划列表")
    public Result<List<InvestmentPlanVO>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        try {
            List<InvestmentPlanVO> history = planService.getHistory(userId);
            return Result.success(history);
        } catch (Exception e) {
            return Result.error(500, "Service Error: " + e.getMessage());
        }
    }

    @PostMapping("/execute")
    @Operation(summary = "执行计划", description = "根据ID执行指定的调仓计划")
    public Result<Void> executePlan(HttpServletRequest request, @RequestParam Long planId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        planService.executePlan(userId, planId);
        return Result.success(null, "执行成功，资产已更新");
    }
}
