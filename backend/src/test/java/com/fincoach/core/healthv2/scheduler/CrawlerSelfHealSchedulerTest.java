package com.fincoach.core.healthv2.scheduler;

import com.fincoach.core.healthv2.dto.admin.AdminDataSourceStatusDTO;
import com.fincoach.core.healthv2.dto.admin.AdminJobStatusDTO;
import com.fincoach.core.healthv2.service.AlertService;
import com.fincoach.core.healthv2.service.admin.DataSourceAdminService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.times;

public class CrawlerSelfHealSchedulerTest {

    @Test
    public void schedulerRestartsWhenStale() {
        DataSourceAdminService service = Mockito.mock(DataSourceAdminService.class);
        AlertService alertService = Mockito.mock(AlertService.class);
        AdminJobStatusDTO job = new AdminJobStatusDTO();
        job.setStatus("RUNNING");
        job.setStale(true);
        job.setSecondsSinceHeartbeat(120L);
        AdminDataSourceStatusDTO status = new AdminDataSourceStatusDTO();
        status.setJob(job);
        Mockito.when(service.getStatus(Mockito.any())).thenReturn(status);

        CrawlerSelfHealScheduler scheduler = new CrawlerSelfHealScheduler(service, alertService);
        scheduler.checkAndHeal();

        Mockito.verify(service, times(1)).startRealtime(Mockito.any());
    }
}
