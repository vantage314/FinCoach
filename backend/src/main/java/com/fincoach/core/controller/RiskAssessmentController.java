package com.fincoach.core.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.controller.dto.RiskAssessmentDTO;
import com.fincoach.core.controller.vo.RiskAssessmentVO;
import com.fincoach.core.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 风险测评控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/risk")
@Tag(name = "风险测评模块", description = "沉浸式智能风险测评")
public class RiskAssessmentController {

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @PostMapping("/assess")
    @Operation(summary = "提交风险测评", description = "基于一票否决+分值映射算法计算风险等级")
    public Result<RiskAssessmentVO> assess(@Valid @RequestBody RiskAssessmentDTO dto,
                                           HttpServletRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        RiskAssessmentVO vo = riskAssessmentService.assess(userId, dto);
        return Result.success(vo);
    }

    @GetMapping("/latest")
    @Operation(summary = "获取最新测评结果", description = "返回用户最近一次风险测评结果")
    public Result<RiskAssessmentVO> getLatest(HttpServletRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        RiskAssessmentVO vo = riskAssessmentService.getLatest(userId);
        if (vo == null) {
            return Result.error(404, "尚未完成风险测评");
        }
        return Result.success(vo);
    }
}
