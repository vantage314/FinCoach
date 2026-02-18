package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminJobStatusDTO {
    private String jobName;
    private String status;
    private String lastStartAt;
    private String lastHeartbeatAt;
    private String lastEndAt;
    private String lastError;
    private String lastLog;
    private String updatedAt;
}
