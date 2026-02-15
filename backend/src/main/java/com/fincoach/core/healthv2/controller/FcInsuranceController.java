package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.InsuranceProfileDTO;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;
import com.fincoach.core.healthv2.service.FcInsuranceProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 体检v2-保险档案控制器
 * 一个用户仅一条记录，POST 为 upsert
 */
@Slf4j
@RestController
@RequestMapping("/api/app/insurance-profile")
@Tag(name = "体检v2-保险档案", description = "保险档案(upsert)")
public class FcInsuranceController {

    @Autowired
    private FcInsuranceProfileService insuranceService;

    @PostMapping
    @Operation(summary = "录入/更新保险档案（一个用户一条）")
    public Result<FcInsuranceProfileEntity> upsert(@Valid @RequestBody InsuranceProfileDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(insuranceService.upsert(userId, dto));
    }

    @GetMapping
    @Operation(summary = "获取保险档案")
    public Result<FcInsuranceProfileEntity> get() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        FcInsuranceProfileEntity profile = insuranceService.getByUserId(userId);
        return Result.success(profile);
    }
}
