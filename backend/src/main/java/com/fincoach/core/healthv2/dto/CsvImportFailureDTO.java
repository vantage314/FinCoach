package com.fincoach.core.healthv2.dto;

import lombok.Data;

@Data
public class CsvImportFailureDTO {
    private int row;
    private String field;
    private String reason;
    private String raw;

    public String getMessage() {
        return reason;
    }

    public void setMessage(String message) {
        this.reason = message;
    }
}
