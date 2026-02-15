package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.*;
import com.fincoach.core.healthv2.mapper.*;
import com.fincoach.core.healthv2.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/data")
@Tag(name = "Admin-Data", description = "数据查看与修正")
public class DataAdminController {

    @Autowired
    private FcAssetMapper assetMapper;
    @Autowired
    private FcLiabilityMapper liabilityMapper;
    @Autowired
    private FcCashflowMapper cashflowMapper;
    @Autowired
    private FcHealthReportMapper reportMapper;
    @Autowired
    private AuditService auditService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "按用户查看资产/负债/现金流")
    public Result<Map<String, Object>> getUserData(@PathVariable Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assets", assetMapper.selectList(
                new LambdaQueryWrapper<FcAssetEntity>().eq(FcAssetEntity::getUserId, userId)));
        data.put("liabilities", liabilityMapper.selectList(
                new LambdaQueryWrapper<FcLiabilityEntity>().eq(FcLiabilityEntity::getUserId, userId)));
        data.put("cashflows", cashflowMapper.selectList(
                new LambdaQueryWrapper<FcCashflowEntity>().eq(FcCashflowEntity::getUserId, userId)
                        .orderByDesc(FcCashflowEntity::getMonth)));
        data.put("reports", reportMapper.selectList(
                new LambdaQueryWrapper<FcHealthReportEntity>().eq(FcHealthReportEntity::getUserId, userId)
                        .orderByDesc(FcHealthReportEntity::getReportDate)));
        return Result.success(data);
    }

    @PutMapping("/assets/{id}")
    @Operation(summary = "修正资产（审计记录）")
    public Result<String> updateAsset(@PathVariable Long id, @RequestBody FcAssetEntity dto) {
        FcAssetEntity existing = assetMapper.selectById(id);
        if (existing == null) return Result.error(404, "资产不存在");
        dto.setId(id);
        assetMapper.updateById(dto);
        auditService.log(UserContext.getCurrentUserId(), "ADMIN_UPDATE_ASSET", "ASSET", id, existing, dto);
        return Result.success("修正成功");
    }

    @PutMapping("/liabilities/{id}")
    @Operation(summary = "修正负债（审计记录）")
    public Result<String> updateLiability(@PathVariable Long id, @RequestBody FcLiabilityEntity dto) {
        FcLiabilityEntity existing = liabilityMapper.selectById(id);
        if (existing == null) return Result.error(404, "负债不存在");
        dto.setId(id);
        liabilityMapper.updateById(dto);
        auditService.log(UserContext.getCurrentUserId(), "ADMIN_UPDATE_LIABILITY", "LIABILITY", id, existing, dto);
        return Result.success("修正成功");
    }
}
