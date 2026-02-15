package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.GenerateReportRequestDTO;
import com.fincoach.core.healthv2.dto.HealthReportTrendVO;
import com.fincoach.core.healthv2.dto.HealthReportV2VO;
import com.fincoach.core.healthv2.service.HealthReportTrendService;
import com.fincoach.core.healthv2.service.HealthReportV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 体检v2-体检报告控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/app/health-reports")
@Tag(name = "体检v2-体检报告", description = "报告生成与查询")
public class HealthReportV2Controller {

    @Autowired
    private HealthReportV2Service reportService;
    @Autowired
    private HealthReportTrendService trendService;

    @PostMapping("/generate")
    @Operation(summary = "生成体检报告", description = "基于用户当前数据生成体检报告骨架")
    public Result<HealthReportV2VO> generate(@RequestBody(required = false) GenerateReportRequestDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");

        // 如果 DTO 中指定了 userId（管理后台场景），优先使用；否则用当前登录用户
        // M1 阶段仅使用当前登录用户
        HealthReportV2VO vo = reportService.generate(userId);
        return Result.success(vo, "报告生成成功");
    }

    @GetMapping("/latest")
    @Operation(summary = "获取最新报告")
    public Result<HealthReportV2VO> getLatest() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");

        HealthReportV2VO vo = reportService.getLatest(userId);
        if (vo == null) {
            return Result.error(404, "暂无体检报告，请先生成");
        }
        return Result.success(vo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "按ID获取报告")
    public Result<HealthReportV2VO> getById(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");

        HealthReportV2VO vo = reportService.getById(id);
        if (vo == null) {
            return Result.error(404, "报告不存在");
        }
        // 越权防御：只能查看自己的报告
        if (!vo.getUserId().equals(userId)) {
            return Result.error(403, "无权访问此报告");
        }
        return Result.success(vo);
    }

    @GetMapping("/trend")
    @Operation(summary = "获取报告趋势")
    public Result<HealthReportTrendVO> getTrend(@RequestParam(required = false, defaultValue = "6") Integer points) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        int safePoints = points == null ? 6 : points;
        if (safePoints < 2 || safePoints > 24) {
            return Result.error(400, "points 仅支持 2..24");
        }
        return Result.success(trendService.getTrend(userId, safePoints));
    }
}
