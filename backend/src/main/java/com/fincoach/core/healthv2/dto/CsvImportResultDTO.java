package com.fincoach.core.healthv2.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CsvImportResultDTO {
    private int successCount;
    private int failCount;
    private List<CsvImportFailureDTO> failures = new ArrayList<>();

    public void addFailure(CsvImportFailureDTO failure) {
        failures.add(failure);
        failCount = failures.size();
    }

    public void increaseSuccess() {
        successCount++;
    }

    public int getTotalRows() {
        return successCount + failCount;
    }

    public int getSuccessRows() {
        return successCount;
    }

    public int getErrorRows() {
        return failCount;
    }

    public List<CsvImportFailureDTO> getErrors() {
        return failures;
    }
}
