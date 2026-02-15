package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CreateCashflowDTO;
import com.fincoach.core.healthv2.dto.CsvImportFailureDTO;
import com.fincoach.core.healthv2.dto.CsvImportResultDTO;
import com.fincoach.core.healthv2.util.CsvReaderHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CashflowCsvImportService {

    private final CsvReaderHelper csvReaderHelper;
    private final FcCashflowService fcCashflowService;

    public CashflowCsvImportService(CsvReaderHelper csvReaderHelper,
                                    FcCashflowService fcCashflowService) {
        this.csvReaderHelper = csvReaderHelper;
        this.fcCashflowService = fcCashflowService;
    }

    public CsvImportResultDTO importCsv(MultipartFile file, Long userId) {
        CsvImportResultDTO result = new CsvImportResultDTO();
        try {
            List<CsvReaderHelper.CsvRow> rows = csvReaderHelper.readRows(file);
            for (CsvReaderHelper.CsvRow row : rows) {
                try {
                    CreateCashflowDTO dto = toCreateCashflowDTO(row.columns());
                    fcCashflowService.upsert(userId, dto);
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

    private CreateCashflowDTO toCreateCashflowDTO(String[] cols) {
        if (cols.length < 4) {
            throw new IllegalArgumentException("列数不足，至少需要 month,income,fixedExpense,variableExpense");
        }

        String month = normalizeMonth(cols[0].trim());
        BigDecimal income = parseDecimal(cols[1], "income 非法");
        BigDecimal fixedExpense = parseDecimal(cols[2], "fixedExpense 非法");
        BigDecimal variableExpense = parseDecimal(cols[3], "variableExpense 非法");
        BigDecimal monthlyDebtPayment = cols.length > 4 && !cols[4].trim().isEmpty()
                ? parseDecimal(cols[4], "monthlyDebtPayment 非法")
                : BigDecimal.ZERO;
        String notes = cols.length > 5 ? cols[5].trim() : "";

        if (income.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("income 必须大于等于0");
        if (fixedExpense.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("fixedExpense 必须大于等于0");
        if (variableExpense.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("variableExpense 必须大于等于0");
        if (monthlyDebtPayment.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("monthlyDebtPayment 必须大于等于0");

        CreateCashflowDTO dto = new CreateCashflowDTO();
        dto.setMonth(month);
        dto.setIncome(income);
        dto.setFixedExpense(fixedExpense);
        dto.setVariableExpense(variableExpense);
        dto.setMonthlyDebtPayment(monthlyDebtPayment);
        dto.setNotes(notes.isEmpty() ? null : notes);
        return dto;
    }

    private String normalizeMonth(String raw) {
        if (raw.matches("^\\d{4}-\\d{2}$")) {
            return raw;
        }
        if (raw.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return raw.substring(0, 7);
        }
        throw new IllegalArgumentException("month 格式需为 YYYY-MM 或 YYYY-MM-DD");
    }

    private BigDecimal parseDecimal(String value, String message) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(message);
        }
    }
}
