package com.fincoach.core.healthv2.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.dto.HealthReportV2VO;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class HealthReportMarkdownRenderer {

    private final ObjectMapper objectMapper;

    public HealthReportMarkdownRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render(HealthReportV2VO report) {
        StringBuilder md = new StringBuilder();
        String dateText = report.getReportDate() == null
                ? "N/A"
                : report.getReportDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        md.append("# Health Report ").append(dateText).append("\n\n");
        md.append("## Scores\n");
        md.append("- Health: ").append(report.getHealthScore()).append("\n");
        md.append("- Risk: ").append(report.getRiskScore()).append("\n");
        md.append("- Behavior: ").append(report.getBehaviorScore()).append("\n\n");

        md.append("## Metrics\n");
        appendSection(md, report.getMetrics());
        md.append("\n## Advice\n");
        appendSection(md, report.getAdvice());
        return md.toString();
    }

    private void appendSection(StringBuilder md, Map<String, Object> section) {
        if (section == null || section.isEmpty()) {
            md.append("- Empty\n");
            return;
        }
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            md.append("### ").append(entry.getKey()).append("\n");
            md.append("```json\n");
            md.append(toJson(entry.getValue())).append("\n");
            md.append("```\n");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
