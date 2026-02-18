package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminRealtimeHealthDTO {
    private String status;
    private Boolean stale;
    private String lastHeartbeatAt;
    private Long secondsSinceHeartbeat;
    private String lastStartAt;
    private String lastEndAt;
}
