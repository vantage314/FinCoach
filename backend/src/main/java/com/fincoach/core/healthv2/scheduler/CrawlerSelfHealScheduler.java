package com.fincoach.core.healthv2.scheduler;

import com.fincoach.core.healthv2.dto.admin.AdminDataSourceStatusDTO;
import com.fincoach.core.healthv2.dto.admin.AdminJobStatusDTO;
import com.fincoach.core.healthv2.service.AlertService;
import com.fincoach.core.healthv2.service.admin.DataSourceAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CrawlerSelfHealScheduler {
    private final DataSourceAdminService dataSourceAdminService;
    private final AlertService alertService;

    @Value("${crawler.selfHealEnabled:true}")
    private boolean selfHealEnabled = true;

    public CrawlerSelfHealScheduler(DataSourceAdminService dataSourceAdminService,
                                    AlertService alertService) {
        this.dataSourceAdminService = dataSourceAdminService;
        this.alertService = alertService;
    }

    @Scheduled(fixedDelayString = "${crawler.selfHealIntervalMs:15000}")
    public void checkAndHeal() {
        if (!selfHealEnabled) {
            return;
        }
        try {
            AdminDataSourceStatusDTO status = dataSourceAdminService.getStatus(null);
            AdminJobStatusDTO job = status != null ? status.getJob() : null;
            if (job == null) {
                return;
            }
            if (Boolean.TRUE.equals(job.getStale())
                    && "RUNNING".equalsIgnoreCase(job.getStatus())) {
                log.warn("event=PY_CRAWLER_STALE_DETECTED secondsSinceHeartbeat={}",
                        job.getSecondsSinceHeartbeat());
                if (alertService != null) {
                    java.util.Map<String, Object> meta = new java.util.LinkedHashMap<>();
                    if (job.getSecondsSinceHeartbeat() != null) {
                        meta.put("secondsSinceHeartbeat", job.getSecondsSinceHeartbeat());
                    }
                    if (job.getLastHeartbeatAt() != null) {
                        meta.put("lastHeartbeatAt", job.getLastHeartbeatAt());
                    }
                    alertService.raiseAlert(null, "CRAWLER_STALE_DETECTED", "DANGER",
                            "爬虫心跳超时", "检测到抓取任务心跳超时", "CRAWLER", meta);
                }
                dataSourceAdminService.startRealtime(null);
                if (alertService != null) {
                    java.util.Map<String, Object> meta = new java.util.LinkedHashMap<>();
                    if (job.getSecondsSinceHeartbeat() != null) {
                        meta.put("secondsSinceHeartbeat", job.getSecondsSinceHeartbeat());
                    }
                    if (job.getLastHeartbeatAt() != null) {
                        meta.put("lastHeartbeatAt", job.getLastHeartbeatAt());
                    }
                    alertService.raiseAlert(null, "CRAWLER_STALE_RECOVERED", "WARN",
                            "爬虫已尝试恢复", "已触发自愈重启抓取任务", "CRAWLER", meta);
                }
            }
        } catch (Exception e) {
            log.warn("event=PY_CRAWLER_SELF_HEAL_FAILED error={}", e.getMessage());
        }
    }
}
