package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminDataSourceStatusDTO {
    private String mode;
    private String crawlerMode;
    private Integer crawlerIntervalSeconds;
    private Integer crawlerMaxBatches;
    private AdminJobStatusDTO job;
}
