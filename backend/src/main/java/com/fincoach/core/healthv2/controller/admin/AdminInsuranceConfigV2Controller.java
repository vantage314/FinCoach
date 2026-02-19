package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.InsuranceConfigUpsertDTO;
import com.fincoach.core.healthv2.entity.FcInsuranceConfigEntity;
import com.fincoach.core.healthv2.service.FcInsuranceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/admin/api/insurance/config")
@Tag(name = "Admin-InsuranceConfigV2", description = "保险配置V2")
public class AdminInsuranceConfigV2Controller {

    @Autowired
    private FcInsuranceConfigService configService;

    @GetMapping
    @Operation(summary = "获取保险配置")
    public Result<FcInsuranceConfigEntity> get() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(configService.getDefaultConfig());
    }

    @PostMapping("/upsert")
    @Operation(summary = "更新保险配置")
    public Result<FcInsuranceConfigEntity> upsert(@Valid @RequestBody InsuranceConfigUpsertDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");

        if (!validRatio(dto.getPremiumRatioWarn()) || !validRatio(dto.getPremiumRatioDanger())) {
            return Result.error(400, "premiumRatio must be in (0,1]");
        }
        return Result.success(configService.upsertDefault(dto));
    }

    private boolean validRatio(BigDecimal value) {
        if (value == null) return true;
        return value.compareTo(BigDecimal.ZERO) > 0 && value.compareTo(BigDecimal.ONE) <= 0;
    }
}
