package com.fincoach.core.healthv2.dto;

import lombok.Data;

@Data
public class CsvImportFailureDTO {
    private int row;
    private String reason;
    private String raw;
}
