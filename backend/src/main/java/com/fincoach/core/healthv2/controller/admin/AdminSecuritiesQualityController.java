package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesQualityDTO;
import com.fincoach.core.healthv2.service.admin.AdminSecuritiesQualityService;
import com.fincoach.core.security.AdminOnly;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/api/securities/quality")
@Tag(name = "Admin-Securities-Quality", description = "Securities data quality")
@AdminOnly
public class AdminSecuritiesQualityController {

    private final AdminSecuritiesQualityService service;

    public AdminSecuritiesQualityController(AdminSecuritiesQualityService service) {
        this.service = service;
    }

    @GetMapping
    public Result<AdminSecuritiesQualityDTO> evaluate() {
        AdminSecuritiesQualityDTO dto = service.evaluate();
        int missing = dto.getMissingMappings() == null ? 0 : dto.getMissingMappings().size();
        int anomalies = dto.getAnomalies() == null ? 0 : dto.getAnomalies().size();
        int issues = dto.getSnapshotCoverage() != null && dto.getSnapshotCoverage().getIssues() != null
                ? dto.getSnapshotCoverage().getIssues().size()
                : 0;
        log.info("event=SEC_DATA_QUALITY userId={} missingMappings={} anomalies={} issues={}",
                UserContext.getCurrentUserId(), missing, anomalies, issues);
        return Result.success(dto);
    }
}
