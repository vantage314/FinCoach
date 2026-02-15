package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.AdminExecutionStatsVO;

public interface AdminDashboardAnalyticsService {

    AdminExecutionStatsVO getExecutionStats(int days);
}
