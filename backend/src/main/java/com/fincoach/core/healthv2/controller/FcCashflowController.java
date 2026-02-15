package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.CreateCashflowDTO;
import com.fincoach.core.healthv2.entity.FcCashflowEntity;
import com.fincoach.core.healthv2.service.FcCashflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 体检v2-现金流管理控制器
 * upsert 语义：同 user_id + month 存在则更新
 */
@Slf4j
@RestController
@RequestMapping("/api/app/cashflows")
@Tag(name = "体检v2-现金流", description = "现金流录入(upsert)")
public class FcCashflowController {

    @Autowired
    private FcCashflowService cashflowService;

    @PostMapping
    @Operation(summary = "录入/更新现金流（同月份自动更新）")
    public Result<FcCashflowEntity> upsert(@Valid @RequestBody CreateCashflowDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(cashflowService.upsert(userId, dto));
    }

    @GetMapping
    @Operation(summary = "查询用户现金流列表")
    public Result<List<FcCashflowEntity>> list() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(cashflowService.listByUserId(userId));
    }
}
