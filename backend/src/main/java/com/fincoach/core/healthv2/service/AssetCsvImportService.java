package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CreateAssetDTO;
import com.fincoach.core.healthv2.dto.CsvImportFailureDTO;
import com.fincoach.core.healthv2.dto.CsvImportResultDTO;
import com.fincoach.core.healthv2.util.CsvReaderHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AssetCsvImportService {

    private final CsvReaderHelper csvReaderHelper;
    private final FcAssetService fcAssetService;

    public AssetCsvImportService(CsvReaderHelper csvReaderHelper,
                                 FcAssetService fcAssetService) {
        this.csvReaderHelper = csvReaderHelper;
        this.fcAssetService = fcAssetService;
    }

    public CsvImportResultDTO importCsv(MultipartFile file, Long userId) {
        CsvImportResultDTO result = new CsvImportResultDTO();
        try {
            List<CsvReaderHelper.CsvRow> rows = csvReaderHelper.readRows(file);
            for (CsvReaderHelper.CsvRow row : rows) {
                try {
                    CreateAssetDTO dto = toCreateAssetDTO(row.columns());
                    fcAssetService.create(userId, dto);
                    result.increaseSuccess();
                } catch (Exception e) {
                    CsvImportFailureDTO failure = new CsvImportFailureDTO();
                    failure.setRow(row.row());
                    failure.setReason(e.getMessage());
                    failure.setRaw(row.raw());
                    result.addFailure(failure);
                }
            }
        } catch (Exception e) {
            CsvImportFailureDTO failure = new CsvImportFailureDTO();
            failure.setRow(0);
            failure.setReason("文件读取失败: " + e.getMessage());
            failure.setRaw("");
            result.addFailure(failure);
        }
        return result;
    }

    private CreateAssetDTO toCreateAssetDTO(String[] cols) {
        if (cols.length < 3) {
            throw new IllegalArgumentException("列数不足，至少需要 type,name,amount");
        }

        String type = cols[0].trim();
        String name = cols[1].trim();
        String amountText = cols[2].trim();
        String currency = cols.length > 3 ? cols[3].trim() : "";
        String riskLevel = cols.length > 4 ? cols[4].trim() : "";
        String asOfDate = cols.length > 5 ? cols[5].trim() : "";
        String metaJson = cols.length > 6 ? cols[6].trim() : "";

        if (type.isEmpty()) {
            throw new IllegalArgumentException("type 不能为空");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
        } catch (Exception e) {
            throw new IllegalArgumentException("amount 非法");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount 必须大于0");
        }

        CreateAssetDTO dto = new CreateAssetDTO();
        dto.setType(type);
        dto.setName(name.isEmpty() ? type : name);
        dto.setAmount(amount);
        dto.setCurrency(currency.isEmpty() ? "CNY" : currency);
        dto.setRiskLevel(riskLevel.isEmpty() ? "MEDIUM" : riskLevel);
        dto.setAsOfDate(asOfDate.isEmpty() ? null : asOfDate);
        dto.setMetaJson(metaJson.isEmpty() ? null : metaJson);
        return dto;
    }
}
