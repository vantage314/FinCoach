package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.DebtDeleteDTO;
import com.fincoach.core.healthv2.dto.DebtUpsertDTO;
import com.fincoach.core.healthv2.entity.FcDebtEntity;
import com.fincoach.core.healthv2.dto.CsvImportResultDTO;
import com.fincoach.core.healthv2.service.DebtCsvImportService;
import com.fincoach.core.healthv2.service.FcDebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/app/debt")
@Tag(name = "体检v2-债务", description = "债务 CRUD")
public class FcDebtController {

    @Autowired
    private FcDebtService debtService;

    @Autowired
    private DebtCsvImportService debtCsvImportService;

    @GetMapping("/list")
    @Operation(summary = "查询用户债务列表")
    public Result<List<FcDebtEntity>> list() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        List<FcDebtEntity> list = debtService.listActiveByUserId(userId);
        return Result.success(list == null ? Collections.emptyList() : list);
    }

    @PostMapping("/upsert")
    @Operation(summary = "新增/更新债务")
    public Result<FcDebtEntity> upsert(@Valid @RequestBody DebtUpsertDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(debtService.upsert(userId, dto));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除债务(软删除)")
    public Result<String> delete(@Valid @RequestBody DebtDeleteDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        debtService.softDelete(userId, dto.getId());
        return Result.success("删除成功");
    }

    @PostMapping("/importCsv")
    @Operation(summary = "导入债务 CSV")
    public Result<CsvImportResultDTO> importCsv(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        if (file == null || file.isEmpty()) {
            return Result.error(400, "file 不能为空");
        }
        CsvImportResultDTO result = debtCsvImportService.importCsv(file, userId);
        return Result.success(result);
    }
}
