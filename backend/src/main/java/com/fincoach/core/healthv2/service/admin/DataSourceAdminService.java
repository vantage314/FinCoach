package com.fincoach.core.healthv2.service.admin;

import com.fincoach.core.healthv2.dto.admin.AdminDataSourceImportResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceStatusDTO;
import com.fincoach.core.healthv2.dto.admin.AdminJobActionResultDTO;

public interface DataSourceAdminService {
    AdminDataSourceStatusDTO getStatus(Long actorUserId);

    AdminDataSourceStatusDTO switchMode(String mode, Long actorUserId);

    AdminDataSourceImportResultDTO importDemoData(Long actorUserId);

    AdminJobActionResultDTO startRealtime(Long actorUserId);

    AdminJobActionResultDTO stopRealtime(Long actorUserId);
}
