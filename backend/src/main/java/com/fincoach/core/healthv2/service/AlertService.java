package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.entity.FcAlertEntity;

import java.util.List;
import java.util.Map;

public interface AlertService {

    FcAlertEntity raiseAlert(Long userId,
                             String alertType,
                             String severity,
                             String title,
                             String message,
                             String source,
                             Map<String, Object> meta);

    boolean ackAlert(Long alertId);

    boolean resolveAlert(Long alertId);

    List<FcAlertEntity> listUserAlerts(Long userId, String status);

    List<FcAlertEntity> listAdminAlerts(String status, String alertType, String severity, Long userId);
}
