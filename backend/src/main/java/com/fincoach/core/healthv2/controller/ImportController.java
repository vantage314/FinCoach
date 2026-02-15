package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.CsvImportResultDTO;
import com.fincoach.core.healthv2.service.AssetCsvImportService;
import com.fincoach.core.healthv2.service.CashflowCsvImportService;
import com.fincoach.core.healthv2.service.LiabilityCsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/app/import")
@Tag(name = "App-Import", description = "CSV 导入")
public class ImportController {

    private final AssetCsvImportService assetCsvImportService;
    private final LiabilityCsvImportService liabilityCsvImportService;
    private final CashflowCsvImportService cashflowCsvImportService;

    public ImportController(AssetCsvImportService assetCsvImportService,
                            LiabilityCsvImportService liabilityCsvImportService,
                            CashflowCsvImportService cashflowCsvImportService) {
        this.assetCsvImportService = assetCsvImportService;
        this.liabilityCsvImportService = liabilityCsvImportService;
        this.cashflowCsvImportService = cashflowCsvImportService;
    }

    @PostMapping("/assets")
    @Operation(summary = "导入资产 CSV")
    public Result<CsvImportResultDTO> importAssets(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        if (file == null || file.isEmpty()) {
            return Result.error(400, "file 不能为空");
        }
        CsvImportResultDTO result = assetCsvImportService.importCsv(file, userId);
        return Result.success(result);
    }

    @PostMapping("/liabilities")
    @Operation(summary = "导入负债 CSV")
    public Result<CsvImportResultDTO> importLiabilities(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        if (file == null || file.isEmpty()) {
            return Result.error(400, "file 不能为空");
        }
        CsvImportResultDTO result = liabilityCsvImportService.importCsv(file, userId);
        return Result.success(result);
    }

    @PostMapping("/cashflows")
    @Operation(summary = "导入现金流 CSV")
    public Result<CsvImportResultDTO> importCashflows(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        if (file == null || file.isEmpty()) {
            return Result.error(400, "file 不能为空");
        }
        CsvImportResultDTO result = cashflowCsvImportService.importCsv(file, userId);
        return Result.success(result);
    }
}
