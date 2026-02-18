package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminSecuritiesQualityDTO {
    private List<AdminSecuritiesQualityMissingMappingDTO> missingMappings;
    private List<String> legacyDemoKeyIssues;
    private AdminSecuritiesQualityCoverageDTO snapshotCoverage;
    private List<AdminSecuritiesQualityAnomalyDTO> anomalies;
    private List<String> recommendations;
}
