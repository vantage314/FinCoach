package com.fincoach.core.healthv2.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin dashboard stats")
public class AdminDashboardStatsVO {

    private Overview overview;

    private ScoreDistribution scoreDistribution;

    private Recent recent;

    private Meta meta;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        private Long userCount;
        private Long reportCount;
        private Long alertTriggerCount;
        private Long notificationUnreadCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDistribution {
        private List<BucketVO> healthScoreBuckets;
        private List<BucketVO> riskScoreBuckets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recent {
        private List<RecentReportVO> recentReports;
        private List<RecentAlertVO> recentAlerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private LocalDateTime generatedAt;
        private Integer windowDays;
    }
}
