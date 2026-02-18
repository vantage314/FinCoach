package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class AdminJobStatusDTO {
    private String jobName;
    private String status;
    private String lastStartAt;
    private String lastHeartbeatAt;
    private String lastEndAt;
    private String lastError;
    private String lastErrorAt;
    private String lastLog;
    private String updatedAt;
    private Boolean stale;
    private Long secondsSinceHeartbeat;
    private Integer staleCount;
    private Integer recoverCount;
    private Integer restartCount;
    private List<AdminCrawlerEventDTO> recentEvents;
}
