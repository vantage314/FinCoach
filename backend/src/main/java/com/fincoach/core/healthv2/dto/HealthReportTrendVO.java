package com.fincoach.core.healthv2.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class HealthReportTrendVO {

    private Integer points;

    private List<HealthReportTrendPointVO> reports;

    private Map<String, Object> trend; // 包含 labels 与各指标/分数序列

    private Map<String, Object> execution;
}
