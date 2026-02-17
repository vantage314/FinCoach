package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.debug.PortfolioDebugContextHolder;
import com.fincoach.core.healthv2.debug.PortfolioMarketDebugSnapshot;
import com.fincoach.core.healthv2.debug.AdminMarketDebugMapper;
import com.fincoach.core.healthv2.dto.admin.AdminMarketDebugLatestDTO;
import com.fincoach.core.rbac.RbacPermissionCodes;
import com.fincoach.core.security.AdminOnly;
import com.fincoach.core.security.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/admin/portfolio/market-debug")
@Tag(name = "Admin-Portfolio-Debug", description = "Portfolio Debug (Admin)")
@AdminOnly
public class AdminMarketDebugController {
    private final AdminMarketDebugMapper mapper;

    public AdminMarketDebugController(AdminMarketDebugMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/latest")
    @Operation(summary = "获取最新 Portfolio Market Debug 快照")
    @Permission(RbacPermissionCodes.ADMIN_MARKET_DEBUG_VIEW)
    public ResponseEntity<Result<AdminMarketDebugLatestDTO>> latest() {
        PortfolioMarketDebugSnapshot snapshot = PortfolioDebugContextHolder.get();
        Long userId = UserContext.getCurrentUserId();
        AdminMarketDebugLatestDTO dto = mapper.toDto(snapshot, userId);
        if (snapshot == null) {
            if (dto.getWarnings() == null) {
                dto.setWarnings(new ArrayList<>());
            }
            if (!dto.getWarnings().contains("DEBUG_SNAPSHOT_EMPTY")) {
                dto.getWarnings().add("DEBUG_SNAPSHOT_EMPTY");
            }
        }
        return ResponseEntity.ok(Result.success(dto));
    }
}
