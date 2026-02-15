package com.fincoach.core.healthv2.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class HealthReportTrendPointVO {

    private Long reportId;

    private LocalDateTime reportDate;

    private Map<String, Integer> scores;

    private Map<String, Object> metrics;
}
