package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class AdminRealtimeHealthDTO {
    private String status;
    private Boolean stale;
    private String lastHeartbeatAt;
    private Long secondsSinceHeartbeat;
    private String lastStartAt;
    private String lastEndAt;
    private String lastErrorAt;
    private Integer staleCount;
    private Integer recoverCount;
    private Integer restartCount;
    private List<AdminCrawlerEventDTO> recentEvents;
}
