package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.InsuranceProfileUpsertV2DTO;
import com.fincoach.core.healthv2.entity.FcInsuranceProfileEntity;
import com.fincoach.core.healthv2.service.FcInsuranceProfileV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/app/insurance/profile")
@Tag(name = "体检v2-保险档案V2", description = "保险档案V2(upsert)")
public class InsuranceProfileV2Controller {

    @Autowired
    private FcInsuranceProfileV2Service profileService;

    @GetMapping
    @Operation(summary = "获取保险档案V2")
    public Result<FcInsuranceProfileEntity> get() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(profileService.getByUserId(userId));
    }

    @PostMapping("/upsert")
    @Operation(summary = "录入/更新保险档案V2")
    public Result<FcInsuranceProfileEntity> upsert(@Valid @RequestBody InsuranceProfileUpsertV2DTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(profileService.upsert(userId, dto));
    }
}
