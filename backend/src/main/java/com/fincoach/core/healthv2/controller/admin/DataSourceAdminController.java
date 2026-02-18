package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceImportResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceStatusDTO;
import com.fincoach.core.healthv2.dto.admin.AdminDataSourceSwitchRequest;
import com.fincoach.core.healthv2.dto.admin.AdminJobActionResultDTO;
import com.fincoach.core.healthv2.dto.admin.AdminRealtimeHealthDTO;
import com.fincoach.core.healthv2.service.admin.DataSourceAdminService;
import com.fincoach.core.security.AdminOnly;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/api/data-source")
@Tag(name = "Admin-Data-Source", description = "Data source mode and crawler control")
@AdminOnly
public class DataSourceAdminController {

    private final DataSourceAdminService dataSourceAdminService;

    public DataSourceAdminController(DataSourceAdminService dataSourceAdminService) {
        this.dataSourceAdminService = dataSourceAdminService;
    }

    @GetMapping("/status")
    public Result<AdminDataSourceStatusDTO> status() {
        Long actorUserId = UserContext.getCurrentUserId();
        AdminDataSourceStatusDTO dto = dataSourceAdminService.getStatus(actorUserId);
        return Result.success(dto);
    }

    @PostMapping("/mode")
    public Result<AdminDataSourceStatusDTO> switchMode(@RequestBody AdminDataSourceSwitchRequest request) {
        if (request == null) {
            return Result.error(400, "empty request");
        }
        Long actorUserId = UserContext.getCurrentUserId();
        AdminDataSourceStatusDTO dto = dataSourceAdminService.switchMode(request.getMode(), actorUserId);
        return Result.success(dto);
    }

    @PostMapping("/demo/import")
    public Result<AdminDataSourceImportResultDTO> importDemo() {
        Long actorUserId = UserContext.getCurrentUserId();
        AdminDataSourceImportResultDTO dto = dataSourceAdminService.importDemoData(actorUserId);
        return Result.success(dto);
    }

    @PostMapping("/realtime/start")
    public Result<AdminJobActionResultDTO> startRealtime() {
        Long actorUserId = UserContext.getCurrentUserId();
        AdminJobActionResultDTO dto = dataSourceAdminService.startRealtime(actorUserId);
        return Result.success(dto);
    }

    @PostMapping("/realtime/stop")
    public Result<AdminJobActionResultDTO> stopRealtime() {
        Long actorUserId = UserContext.getCurrentUserId();
        AdminJobActionResultDTO dto = dataSourceAdminService.stopRealtime(actorUserId);
        return Result.success(dto);
    }

    @GetMapping("/realtime/health")
    public Result<AdminRealtimeHealthDTO> health() {
        Long actorUserId = UserContext.getCurrentUserId();
        AdminRealtimeHealthDTO dto = dataSourceAdminService.getRealtimeHealth(actorUserId);
        return Result.success(dto);
    }

    @PostMapping("/realtime/recover")
    public Result<AdminJobActionResultDTO> recover() {
        Long actorUserId = UserContext.getCurrentUserId();
        AdminJobActionResultDTO dto = dataSourceAdminService.recoverRealtime(actorUserId);
        return Result.success(dto);
    }
}
