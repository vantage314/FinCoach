package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminSecuritiesQualityCoverageDTO {
    private long assetsCount;
    private long daysCovered;
    private String latestDate;
    private long lagDays;
    private List<String> issues;
}
