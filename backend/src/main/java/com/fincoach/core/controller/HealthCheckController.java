package com.fincoach.core.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.controller.vo.HealthReportVO;
import com.fincoach.core.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资产健康度检测控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@Tag(name = "资产健康度", description = "四维健康模型体检")
public class HealthCheckController {

    @Autowired
    private HealthCheckService healthCheckService;

    @GetMapping("/check")
    @Operation(summary = "执行资产健康度体检", 
               description = "基于四维模型（流动性、风险匹配、保障力、分散度）计算健康得分")
    public Result<HealthReportVO> checkHealth(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        HealthReportVO report = healthCheckService.checkHealth(userId);
        return Result.success(report);
    }
}
