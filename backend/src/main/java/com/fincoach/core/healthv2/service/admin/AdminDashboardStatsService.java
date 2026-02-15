package com.fincoach.core.healthv2.service.admin;

import com.fincoach.core.healthv2.dto.admin.AdminDashboardStatsQueryDTO;
import com.fincoach.core.healthv2.vo.admin.AdminDashboardStatsVO;

public interface AdminDashboardStatsService {
    AdminDashboardStatsVO getStats(AdminDashboardStatsQueryDTO query, Long actorUserId);
}
