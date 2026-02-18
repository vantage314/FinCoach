package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminSecuritiesSnapshotSummaryDTO {
    private long assetsCount;
    private long sampleSize;
    private String startDate;
    private String endDate;
    private String latestDate;
}
