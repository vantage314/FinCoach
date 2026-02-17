package com.fincoach.core.healthv2.dto.admin;

import java.util.ArrayList;
import java.util.List;

public class AdminAlertListDTO {
    private int total;
    private List<AlertItem> items = new ArrayList<>();

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public List<AlertItem> getItems() { return items; }
    public void setItems(List<AlertItem> items) { this.items = items; }

    public static class AlertItem {
        private Long alertId;
        private String code;
        private String severity;
        private String title;
        private String status;
        private String createdAt;
        private Long reportId;

        public Long getAlertId() { return alertId; }
        public void setAlertId(Long alertId) { this.alertId = alertId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public Long getReportId() { return reportId; }
        public void setReportId(Long reportId) { this.reportId = reportId; }
    }
}
