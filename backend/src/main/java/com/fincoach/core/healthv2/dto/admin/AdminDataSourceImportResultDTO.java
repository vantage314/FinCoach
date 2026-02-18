package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminDataSourceImportResultDTO {
    private int insertedSnapshots;
    private int skippedSnapshots;
    private int insertedMappings;
    private String message;
}
