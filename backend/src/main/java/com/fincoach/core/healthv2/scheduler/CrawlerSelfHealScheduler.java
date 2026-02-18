package com.fincoach.core.healthv2.scheduler;

import com.fincoach.core.healthv2.dto.admin.AdminDataSourceStatusDTO;
import com.fincoach.core.healthv2.dto.admin.AdminJobStatusDTO;
import com.fincoach.core.healthv2.service.admin.DataSourceAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
public class CrawlerSelfHealScheduler {
    private final DataSourceAdminService dataSourceAdminService;

    @Value("${crawler.selfHealEnabled:true}")
    private boolean selfHealEnabled = true;

    @Value("${crawler.staleThresholdSeconds:30}")
    private long staleThresholdSeconds = 30;

    private long delayMillis = 15000L;

    public CrawlerSelfHealScheduler(DataSourceAdminService dataSourceAdminService) {
        this.dataSourceAdminService = dataSourceAdminService;
    }

    @PostConstruct
    public void init() {
        long half = staleThresholdSeconds / 2;
        long bounded = Math.max(5, Math.min(30, half));
        delayMillis = bounded * 1000L;
    }

    public long getDelayMillis() {
        return delayMillis > 0 ? delayMillis : 15000L;
    }

    @Scheduled(fixedDelayString = "#{@crawlerSelfHealScheduler.delayMillis}")
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
                dataSourceAdminService.startRealtime(null);
            }
        } catch (Exception e) {
            log.warn("event=PY_CRAWLER_SELF_HEAL_FAILED error={}", e.getMessage());
        }
    }
}
