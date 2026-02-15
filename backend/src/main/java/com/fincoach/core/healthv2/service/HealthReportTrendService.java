package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.HealthReportTrendVO;

public interface HealthReportTrendService {

    HealthReportTrendVO getTrend(Long userId, int points);
}
