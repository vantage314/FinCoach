package com.fincoach.core.healthv2.util;

import com.fincoach.core.healthv2.dto.admin.AdminAlertListDTO;

import java.util.List;

public final class AlertExportUtil {
    private AlertExportUtil() {}

    public static String toCsv(List<AdminAlertListDTO.AlertItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("alertId,code,severity,title,status,createdAt,reportId\n");
        if (items == null) return sb.toString();
        for (AdminAlertListDTO.AlertItem item : items) {
            if (item == null) continue;
            sb.append(nullSafe(item.getAlertId()))
                    .append(',')
                    .append(escape(item.getCode()))
                    .append(',')
                    .append(escape(item.getSeverity()))
                    .append(',')
                    .append(escape(item.getTitle()))
                    .append(',')
                    .append(escape(item.getStatus()))
                    .append(',')
                    .append(escape(item.getCreatedAt()))
                    .append(',')
                    .append(nullSafe(item.getReportId()))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
