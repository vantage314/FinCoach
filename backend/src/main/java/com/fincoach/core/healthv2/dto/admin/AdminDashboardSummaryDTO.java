package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminDashboardSummaryDTO {
    private Long usersTotal;
    private Long alertsOpen;
    private LocalDateTime latestCrawlerHeartbeatAt;
    private LocalDateTime latestHealthReportAt;
    private Long notificationsUnreadTotal;
    private String systemStatus;
    private List<String> warnings = new ArrayList<>();
}
