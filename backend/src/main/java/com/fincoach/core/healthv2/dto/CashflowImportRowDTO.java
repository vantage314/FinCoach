package com.fincoach.core.healthv2.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CashflowImportRowDTO {
    private String month;
    private BigDecimal income;
    private BigDecimal expense;
}
