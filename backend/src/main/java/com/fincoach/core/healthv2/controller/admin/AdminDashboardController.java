package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminDashboardStatsQueryDTO;
import com.fincoach.core.healthv2.service.admin.AdminDashboardStatsService;
import com.fincoach.core.healthv2.vo.admin.AdminDashboardStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin-Dashboard", description = "Dashboard Stats")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardStatsService statsService;

    @GetMapping("/stats")
    @Operation(summary = "Get Dashboard Stats")
    public Result<AdminDashboardStatsVO> stats(AdminDashboardStatsQueryDTO query) {
        Long userId = UserContext.getCurrentUserId();
        // Constraints are handled in service (or could be @Validated if added)
        // Here we just pass query
        AdminDashboardStatsVO vo = statsService.getStats(query, userId);
        return Result.success(vo);
    }
}
