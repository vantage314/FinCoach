package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminAdviceWarningStatsDTO {
    private int totalReports;
    private List<CodeStat> byCode = new ArrayList<>();
    private List<LatestSample> latestSamples = new ArrayList<>();

    @Data
    public static class CodeStat {
        private String code;
        private int count;
        private double ratio;
    }

    @Data
    public static class LatestSample {
        private Long reportId;
        private List<String> codes = new ArrayList<>();
    }
}
