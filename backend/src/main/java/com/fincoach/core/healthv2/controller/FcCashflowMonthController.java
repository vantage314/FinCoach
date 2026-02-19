package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.CashflowMonthUpsertDTO;
import com.fincoach.core.healthv2.entity.FcCashflowMonthEntity;
import com.fincoach.core.healthv2.service.FcCashflowMonthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/app/cashflow")
@Tag(name = "体检v2-现金流(月度)", description = "现金流月度 CRUD")
public class FcCashflowMonthController {

    @Autowired
    private FcCashflowMonthService cashflowMonthService;

    @GetMapping("/months")
    @Operation(summary = "查询现金流月度范围")
    public Result<List<FcCashflowMonthEntity>> list(@RequestParam(required = false) String from,
                                                    @RequestParam(required = false) String to) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        List<FcCashflowMonthEntity> list = cashflowMonthService.listByUserIdAndRange(userId, from, to);
        return Result.success(list == null ? Collections.emptyList() : list);
    }

    @PostMapping("/upsertMonth")
    @Operation(summary = "新增/更新现金流月度")
    public Result<FcCashflowMonthEntity> upsert(@Valid @RequestBody CashflowMonthUpsertDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(cashflowMonthService.upsert(userId, dto));
    }
}
