package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.CsvImportFailureDTO;
import com.fincoach.core.healthv2.dto.CsvImportResultDTO;
import com.fincoach.core.healthv2.dto.DebtImportRowDTO;
import com.fincoach.core.healthv2.dto.DebtUpsertDTO;
import com.fincoach.core.healthv2.util.CsvReaderHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DebtCsvImportService {

    private final CsvReaderHelper csvReaderHelper;
    private final FcDebtService debtService;

    public DebtCsvImportService(CsvReaderHelper csvReaderHelper,
                                FcDebtService debtService) {
        this.csvReaderHelper = csvReaderHelper;
        this.debtService = debtService;
    }

    public CsvImportResultDTO importCsv(MultipartFile file, Long userId) {
        CsvImportResultDTO result = new CsvImportResultDTO();
        try {
            List<CsvReaderHelper.CsvRow> rows = csvReaderHelper.readRows(file);
            for (CsvReaderHelper.CsvRow row : rows) {
                try {
                    DebtImportRowDTO dto = toRowDto(row.columns());
                    DebtUpsertDTO upsertDTO = toUpsertDTO(dto);
                    debtService.upsert(userId, upsertDTO);
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

    private DebtImportRowDTO toRowDto(String[] cols) {
        if (cols.length < 3) {
            throw new RowException("row", "列数不足，至少需要 debtType,apr,remainingBalance");
        }
        DebtImportRowDTO dto = new DebtImportRowDTO();
        dto.setDebtType(cols[0].trim());
        dto.setApr(parseDecimal(cols[1], "apr"));
        dto.setRemainingBalance(parseDecimal(cols[2], "remainingBalance"));
        dto.setMonthlyPayment(parseDecimalOptional(cols, 3, "monthlyPayment"));
        dto.setTermMonths(parseIntOptional(cols, 4, "termMonths"));
        dto.setPrincipal(parseDecimalOptional(cols, 5, "principal"));
        dto.setStartDate(parseDateOptional(cols, 6, "startDate"));
        dto.setEndDate(parseDateOptional(cols, 7, "endDate"));
        dto.setExternalKey(parseStringOptional(cols, 8));

        if (!StringUtils.hasText(dto.getDebtType())) {
            throw new RowException("debtType", "debtType 不能为空");
        }
        if (dto.getApr() == null || dto.getApr().compareTo(BigDecimal.ZERO) < 0) {
            throw new RowException("apr", "apr 必须大于等于0");
        }
        if (dto.getRemainingBalance() == null || dto.getRemainingBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new RowException("remainingBalance", "remainingBalance 必须大于等于0");
        }
        if (dto.getPrincipal() != null && dto.getPrincipal().compareTo(BigDecimal.ZERO) < 0) {
            throw new RowException("principal", "principal 必须大于等于0");
        }
        if (dto.getMonthlyPayment() != null && dto.getMonthlyPayment().compareTo(BigDecimal.ZERO) < 0) {
            throw new RowException("monthlyPayment", "monthlyPayment 必须大于等于0");
        }
        if (dto.getTermMonths() != null && dto.getTermMonths() <= 0) {
            throw new RowException("termMonths", "termMonths 必须大于0");
        }
        return dto;
    }

    private DebtUpsertDTO toUpsertDTO(DebtImportRowDTO dto) {
        DebtUpsertDTO upsert = new DebtUpsertDTO();
        upsert.setDebtType(dto.getDebtType());
        upsert.setApr(dto.getApr());
        upsert.setRemainingBalance(dto.getRemainingBalance());
        upsert.setMonthlyPayment(dto.getMonthlyPayment());
        upsert.setTermMonths(dto.getTermMonths());
        BigDecimal principal = dto.getPrincipal();
        if (principal == null) {
            principal = dto.getRemainingBalance();
        }
        upsert.setPrincipal(principal);
        upsert.setStartDate(dto.getStartDate());
        upsert.setEndDate(dto.getEndDate());
        upsert.setExternalKey(dto.getExternalKey());
        return upsert;
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

    private BigDecimal parseDecimalOptional(String[] cols, int index, String field) {
        if (cols.length <= index) {
            return null;
        }
        String raw = cols[index].trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(raw);
        } catch (Exception e) {
            throw new RowException(field, field + " 非法");
        }
    }

    private Integer parseIntOptional(String[] cols, int index, String field) {
        if (cols.length <= index) {
            return null;
        }
        String raw = cols[index].trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            throw new RowException(field, field + " 非法");
        }
    }

    private String parseDateOptional(String[] cols, int index, String field) {
        if (cols.length <= index) {
            return null;
        }
        String raw = cols[index].trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            LocalDate.parse(raw);
            return raw;
        } catch (Exception e) {
            throw new RowException(field, field + " 格式需为 YYYY-MM-DD");
        }
    }

    private String parseStringOptional(String[] cols, int index) {
        if (cols.length <= index) {
            return null;
        }
        String raw = cols[index].trim();
        return raw.isEmpty() ? null : raw;
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
