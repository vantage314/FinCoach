package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CashflowImportRowDTO;
import com.fincoach.core.healthv2.dto.CashflowMonthUpsertDTO;
import com.fincoach.core.healthv2.dto.CsvImportFailureDTO;
import com.fincoach.core.healthv2.dto.CsvImportResultDTO;
import com.fincoach.core.healthv2.util.CsvReaderHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
public class CashflowMonthCsvImportService {

    private final CsvReaderHelper csvReaderHelper;
    private final FcCashflowMonthService cashflowMonthService;

    public CashflowMonthCsvImportService(CsvReaderHelper csvReaderHelper,
                                         FcCashflowMonthService cashflowMonthService) {
        this.csvReaderHelper = csvReaderHelper;
        this.cashflowMonthService = cashflowMonthService;
    }

    public CsvImportResultDTO importCsv(MultipartFile file, Long userId) {
        CsvImportResultDTO result = new CsvImportResultDTO();
        try {
            List<CsvReaderHelper.CsvRow> rows = csvReaderHelper.readRows(file);
            for (CsvReaderHelper.CsvRow row : rows) {
                try {
                    CashflowImportRowDTO dto = toRowDto(row.columns());
                    CashflowMonthUpsertDTO upsertDTO = toUpsertDTO(dto);
                    cashflowMonthService.upsert(userId, upsertDTO);
                    result.increaseSuccess();
                } catch (RowException e) {
                    result.addFailure(toFailure(row, e.getField(), e.getMessage()));
                } catch (Exception e) {
                    result.addFailure(toFailure(row, "", e.getMessage()));
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

    private CashflowImportRowDTO toRowDto(String[] cols) {
        if (cols.length < 3) {
            throw new RowException("row", "列数不足，至少需要 month,income,expense");
        }
        CashflowImportRowDTO dto = new CashflowImportRowDTO();
        dto.setMonth(normalizeMonth(cols[0].trim()));
        dto.setIncome(parseDecimal(cols[1], "income"));
        dto.setExpense(parseDecimal(cols[2], "expense"));

        if (dto.getIncome().compareTo(BigDecimal.ZERO) < 0) {
            throw new RowException("income", "income 必须大于等于0");
        }
        if (dto.getExpense().compareTo(BigDecimal.ZERO) < 0) {
            throw new RowException("expense", "expense 必须大于等于0");
        }
        return dto;
    }

    private CashflowMonthUpsertDTO toUpsertDTO(CashflowImportRowDTO dto) {
        CashflowMonthUpsertDTO upsert = new CashflowMonthUpsertDTO();
        upsert.setMonth(dto.getMonth());
        upsert.setIncome(dto.getIncome());
        upsert.setExpense(dto.getExpense());
        return upsert;
    }

    private String normalizeMonth(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new RowException("month", "month 不能为空");
        }
        if (raw.matches("^\\d{4}-\\d{2}$")) {
            return raw;
        }
        if (raw.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return raw.substring(0, 7);
        }
        try {
            YearMonth.parse(raw);
            return raw;
        } catch (Exception e) {
            throw new RowException("month", "month 格式需为 YYYY-MM 或 YYYY-MM-DD");
        }
    }

    private BigDecimal parseDecimal(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new RowException(field, field + " 不能为空");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            throw new RowException(field, field + " 非法");
        }
    }

    private CsvImportFailureDTO toFailure(CsvReaderHelper.CsvRow row, String field, String message) {
        CsvImportFailureDTO failure = new CsvImportFailureDTO();
        failure.setRow(row.row());
        failure.setField(field);
        failure.setReason(message);
        failure.setRaw(row.raw());
        return failure;
    }

    private static class RowException extends RuntimeException {
        private final String field;

        RowException(String field, String message) {
            super(message);
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }
}
