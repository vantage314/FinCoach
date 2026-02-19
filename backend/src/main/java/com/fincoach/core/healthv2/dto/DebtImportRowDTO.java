package com.fincoach.core.healthv2.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebtImportRowDTO {
    private String debtType;
    private BigDecimal apr;
    private BigDecimal remainingBalance;
    private BigDecimal monthlyPayment;
    private Integer termMonths;
    private BigDecimal principal;
    private String startDate;
    private String endDate;
    private String externalKey;
}
