package com.fincoach.core.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.repository.entity.CompanyProfile;
import com.fincoach.core.repository.entity.FinancialNews;
import com.fincoach.core.repository.entity.FinancialReport;
import com.fincoach.core.repository.entity.CompanyNotice;
import com.fincoach.core.service.InvestmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "智能投资驾驶舱API")
@RestController
@RequestMapping("/api/invest")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    @Operation(summary = "获取最新财经新闻")
    @GetMapping("/news/list")
    public Result<List<FinancialNews>> getLatestNews(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(investmentService.getLatestNews(limit));
    }

    @Operation(summary = "获取新闻详情")
    @GetMapping("/news/{id}")
    public Result<FinancialNews> getNewsDetail(@PathVariable Long id) {
        return Result.success(investmentService.getNewsDetail(id));
    }

    @Operation(summary = "获取公司F10资料")
    @GetMapping("/profile/{code}")
    public Result<CompanyProfile> getCompanyProfile(@PathVariable String code) {
        return Result.success(investmentService.getCompanyProfile(code));
    }

    @Operation(summary = "获取当前用户自选股列表")
    @GetMapping("/watchlist")
    public Result<List<String>> getWatchlist() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(investmentService.getWatchlist(userId));
    }

    @Operation(summary = "切换自选状态(添加/移除)")
    @PostMapping("/watchlist/toggle")
    public Result<Boolean> toggleWatchlist(@RequestBody Map<String, String> body) {
        Long userId = UserContext.getCurrentUserId();
        String code = body.get("code");
        if (code == null) {
            return Result.error(400, "股票代码不能为空");
        }
        boolean isAdded = investmentService.toggleWatchlist(userId, code);
        return Result.success(isAdded); // true=已添加, false=已移除
    }

    @Operation(summary = "获取公司财务报表")
    @GetMapping("/finance/{code}")
    public Result<List<FinancialReport>> getFinancialReports(@PathVariable String code) {
        return Result.success(investmentService.getFinancialReports(code));
    }

    @Operation(summary = "获取公司公告列表")
    @GetMapping("/notice/{code}")
    public Result<List<CompanyNotice>> getCompanyNotices(@PathVariable String code) {
        return Result.success(investmentService.getCompanyNotices(code));
    }
}

