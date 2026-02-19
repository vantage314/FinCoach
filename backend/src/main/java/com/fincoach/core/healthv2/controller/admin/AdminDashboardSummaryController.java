package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fincoach.core.common.Result;
import com.fincoach.core.healthv2.dto.admin.AdminDashboardSummaryDTO;
import com.fincoach.core.healthv2.entity.FcAlertRecordEntity;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;
import com.fincoach.core.healthv2.entity.FcNotificationEntity;
import com.fincoach.core.healthv2.mapper.FcAlertRecordMapper;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import com.fincoach.core.healthv2.mapper.FcNotificationMapper;
import com.fincoach.core.repository.entity.FcJobStatusEntity;
import com.fincoach.core.repository.mapper.FcJobStatusMapper;
import com.fincoach.core.repository.mapper.UserMapper;
import com.fincoach.core.security.AdminOnly;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/api/dashboard")
@Tag(name = "Admin-Dashboard", description = "Admin dashboard summary")
@AdminOnly
public class AdminDashboardSummaryController {

    private static final String JOB_NAME = "PY_MARKET_CRAWLER";

    private final UserMapper userMapper;
    private final FcAlertRecordMapper alertRecordMapper;
    private final FcJobStatusMapper jobStatusMapper;
    private final FcHealthReportMapper healthReportMapper;
    private final FcNotificationMapper notificationMapper;

    public AdminDashboardSummaryController(UserMapper userMapper,
                                           FcAlertRecordMapper alertRecordMapper,
                                           FcJobStatusMapper jobStatusMapper,
                                           FcHealthReportMapper healthReportMapper,
                                           FcNotificationMapper notificationMapper) {
        this.userMapper = userMapper;
        this.alertRecordMapper = alertRecordMapper;
        this.jobStatusMapper = jobStatusMapper;
        this.healthReportMapper = healthReportMapper;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping("/summary")
    public Result<AdminDashboardSummaryDTO> summary() {
        AdminDashboardSummaryDTO dto = new AdminDashboardSummaryDTO();
        List<String> warnings = dto.getWarnings();
        dto.setSystemStatus("OK");

        try {
            dto.setUsersTotal(userMapper.selectCount(new QueryWrapper<>()));
        } catch (Exception e) {
            warnings.add("USERS_COUNT_UNAVAILABLE");
            log.warn("[AdminDashboard] usersTotal query failed: {}", e.getMessage());
        }

        try {
            dto.setAlertsOpen(alertRecordMapper.selectCount(
                    new LambdaQueryWrapper<FcAlertRecordEntity>()
                            .in(FcAlertRecordEntity::getStatus, List.of("OPEN", "ACTIVE"))));
        } catch (Exception e) {
            warnings.add("ALERTS_OPEN_UNAVAILABLE");
            log.warn("[AdminDashboard] alertsOpen query failed: {}", e.getMessage());
        }

        try {
            FcJobStatusEntity job = jobStatusMapper.selectOne(
                    new LambdaQueryWrapper<FcJobStatusEntity>()
                            .eq(FcJobStatusEntity::getJobName, JOB_NAME)
                            .last("LIMIT 1"));
            if (job == null || job.getLastHeartbeatAt() == null) {
                warnings.add("CRAWLER_HEARTBEAT_MISSING");
            }
            dto.setLatestCrawlerHeartbeatAt(job != null ? job.getLastHeartbeatAt() : null);
        } catch (Exception e) {
            warnings.add("CRAWLER_HEARTBEAT_UNAVAILABLE");
            log.warn("[AdminDashboard] crawler heartbeat query failed: {}", e.getMessage());
        }

        try {
            FcHealthReportEntity latest = healthReportMapper.selectOne(
                    new LambdaQueryWrapper<FcHealthReportEntity>()
                            .orderByDesc(FcHealthReportEntity::getReportDate)
                            .last("LIMIT 1"));
            if (latest == null || latest.getReportDate() == null) {
                warnings.add("HEALTH_REPORT_MISSING");
            }
            dto.setLatestHealthReportAt(latest != null ? latest.getReportDate() : null);
        } catch (Exception e) {
            warnings.add("HEALTH_REPORT_UNAVAILABLE");
            log.warn("[AdminDashboard] health report query failed: {}", e.getMessage());
        }

        try {
            dto.setNotificationsUnreadTotal(notificationMapper.selectCount(
                    new LambdaQueryWrapper<FcNotificationEntity>()
                            .eq(FcNotificationEntity::getIsRead, 0)));
        } catch (Exception e) {
            warnings.add("NOTIFICATION_UNREAD_UNAVAILABLE");
            log.warn("[AdminDashboard] notification unread query failed: {}", e.getMessage());
        }

        if (!warnings.isEmpty()) {
            dto.setSystemStatus("WARN");
        }

        return Result.success(dto);
    }
}
