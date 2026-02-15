package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.CsvImportResultDTO;
import com.fincoach.core.healthv2.service.AssetCsvImportService;
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

    public ImportController(AssetCsvImportService assetCsvImportService) {
        this.assetCsvImportService = assetCsvImportService;
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
}
