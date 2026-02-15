package com.fincoach.core.healthv2.controller.app;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.HealthReportV2VO;
import com.fincoach.core.healthv2.service.HealthReportV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/app/portfolio/metrics")
@Tag(name = "APP - Portfolio Metrics", description = "组合分析指标接口")
public class PortfolioMetricsController {

    @Autowired
    private HealthReportV2Service healthReportService;

    @GetMapping("/latest")
    @Operation(summary = "获取最新组合分析指标")
    public Result<Map<String, Object>> getLatestMetrics() {
        Long userId = UserContext.getCurrentUserId();
        HealthReportV2VO report = healthReportService.getLatest(userId);
        
        if (report == null || report.getMetrics() == null) {
            return Result.success(Collections.emptyMap());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> portfolioMetrics = (Map<String, Object>) report.getMetrics().get("portfolio");
        
        if (portfolioMetrics == null) {
            return Result.success(Collections.emptyMap());
        }

        return Result.success(portfolioMetrics);
    }
}
