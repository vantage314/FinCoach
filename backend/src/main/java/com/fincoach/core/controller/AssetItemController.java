package com.fincoach.core.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.controller.dto.AssetItemDTO;
import com.fincoach.core.controller.dto.AssetQueryDTO;
import com.fincoach.core.controller.vo.PortfolioSummaryVO;
import com.fincoach.core.repository.entity.AssetItem;
import com.fincoach.core.service.AssetItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/asset")
@Tag(name = "资产管理模块", description = "资产录入、查询与统计")
public class AssetItemController {

    @Autowired
    private AssetItemService assetItemService;

    @PostMapping("/add")
    @Operation(summary = "录入新资产", description = "userId 从 Token 中解析，严禁前端传入")
    public Result<String> addAsset(@Valid @RequestBody AssetItemDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            log.warn("资产录入失败：未获取到用户ID");
            return Result.error(401, "请先登录");
        }
        
        assetItemService.addAsset(userId, dto);
        return Result.success("资产录入成功");
    }

    @GetMapping("/list")
    @Operation(summary = "获取用户资产列表", description = "支持按分类ID和资产名称过滤")
    public Result<List<AssetItem>> getUserAssets(
            @Parameter(description = "资产分类ID (1-现金, 2-金融投资, 3-固定资产)") @RequestParam(required = false) Integer categoryId,
            @Parameter(description = "资产名称关键字") @RequestParam(required = false) String assetName,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        AssetQueryDTO query = new AssetQueryDTO();
        query.setCategoryId(categoryId);
        query.setAssetName(assetName);
        
        return Result.success(assetItemService.getUserAssets(userId, query));
    }

    @GetMapping("/summary")
    @Operation(summary = "获取资产组合统计", description = "返回总金额、各分类占比及投资红线")
    public Result<PortfolioSummaryVO> getPortfolioSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        return Result.success(assetItemService.getPortfolioSummary(userId));
    }

    @DeleteMapping
    @Operation(summary = "批量删除资产", description = "传入 ID 数组，支持单删或多删。带越权防御，只能删除自己的资产。")
    public Result<String> deleteAssets(@RequestBody List<Long> ids, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        if (ids == null || ids.isEmpty()) {
            return Result.error(400, "请选择要删除的资产");
        }
        
        int rows = assetItemService.deleteAssets(userId, ids);
        return Result.success("成功删除 " + rows + " 笔资产");
    }

    @GetMapping("/analysis")
    @Operation(summary = "资产全景分析与健康体检", description = "返回实时估值、盈亏、健康分和投资建议")
    public Result<com.fincoach.core.controller.vo.AssetAnalysisVO> getAnalysis(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(assetItemService.analyze(userId));
    }
}
