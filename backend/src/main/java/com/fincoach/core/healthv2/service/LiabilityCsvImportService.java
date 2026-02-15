package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CreateLiabilityDTO;
import com.fincoach.core.healthv2.dto.CsvImportFailureDTO;
import com.fincoach.core.healthv2.dto.CsvImportResultDTO;
import com.fincoach.core.healthv2.util.CsvReaderHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LiabilityCsvImportService {

    private final CsvReaderHelper csvReaderHelper;
    private final FcLiabilityService fcLiabilityService;

    public LiabilityCsvImportService(CsvReaderHelper csvReaderHelper,
                                     FcLiabilityService fcLiabilityService) {
        this.csvReaderHelper = csvReaderHelper;
        this.fcLiabilityService = fcLiabilityService;
    }

    public CsvImportResultDTO importCsv(MultipartFile file, Long userId) {
        CsvImportResultDTO result = new CsvImportResultDTO();
        try {
            List<CsvReaderHelper.CsvRow> rows = csvReaderHelper.readRows(file);
            for (CsvReaderHelper.CsvRow row : rows) {
                try {
                    CreateLiabilityDTO dto = toCreateLiabilityDTO(row.columns());
                    fcLiabilityService.create(userId, dto);
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

    private CreateLiabilityDTO toCreateLiabilityDTO(String[] cols) {
        if (cols.length < 5) {
            throw new IllegalArgumentException("列数不足，至少需要 type,principal,interestRate,remainingMonths,monthlyPayment");
        }

        String type = cols[0].trim();
        BigDecimal principal = parseDecimal(cols[1], "principal 非法");
        BigDecimal interestRate = parseDecimal(cols[2], "interestRate 非法");
        Integer remainingMonths = parseInt(cols[3], "remainingMonths 非法");
        BigDecimal monthlyPayment = parseDecimal(cols[4], "monthlyPayment 非法");
        String prepayPenaltyJson = cols.length > 5 ? cols[5].trim() : "";

        if (type.isEmpty()) throw new IllegalArgumentException("type 不能为空");
        if (principal.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("principal 必须大于0");
        if (interestRate.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("interestRate 必须大于等于0");
        if (remainingMonths <= 0) throw new IllegalArgumentException("remainingMonths 必须大于0");
        if (monthlyPayment.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("monthlyPayment 必须大于等于0");

        CreateLiabilityDTO dto = new CreateLiabilityDTO();
        dto.setType(type);
        dto.setPrincipal(principal);
        dto.setInterestRate(interestRate);
        dto.setRemainingMonths(remainingMonths);
        dto.setMonthlyPayment(monthlyPayment);
        dto.setPrepayPenaltyJson(prepayPenaltyJson.isEmpty() ? null : prepayPenaltyJson);
        return dto;
    }

    private BigDecimal parseDecimal(String value, String message) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(message);
        }
    }

    private Integer parseInt(String value, String message) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(message);
        }
    }
}
