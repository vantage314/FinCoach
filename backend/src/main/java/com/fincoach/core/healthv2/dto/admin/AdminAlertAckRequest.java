package com.fincoach.core.healthv2.dto.admin;

import java.util.ArrayList;
import java.util.List;

public class AdminAlertAckRequest {
    private List<AlertRef> alerts = new ArrayList<>();
    private Long reportId;
    private List<String> codes = new ArrayList<>();

    public List<AlertRef> getAlerts() { return alerts; }
    public void setAlerts(List<AlertRef> alerts) { this.alerts = alerts; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public List<String> getCodes() { return codes; }
    public void setCodes(List<String> codes) { this.codes = codes; }

    public static class AlertRef {
        private Long alertId;
        private String code;

        public Long getAlertId() { return alertId; }
        public void setAlertId(Long alertId) { this.alertId = alertId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
