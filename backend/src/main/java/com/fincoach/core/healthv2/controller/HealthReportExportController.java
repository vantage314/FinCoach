package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.HealthReportV2VO;
import com.fincoach.core.healthv2.service.HealthReportV2Service;
import com.fincoach.core.healthv2.util.HealthReportMarkdownRenderer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/health-reports")
@Tag(name = "App-HealthReportExport", description = "体检报告导出")
public class HealthReportExportController {

    private final HealthReportV2Service healthReportV2Service;
    private final HealthReportMarkdownRenderer markdownRenderer;

    public HealthReportExportController(HealthReportV2Service healthReportV2Service,
                                        HealthReportMarkdownRenderer markdownRenderer) {
        this.healthReportV2Service = healthReportV2Service;
        this.markdownRenderer = markdownRenderer;
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "导出体检报告", description = "支持 json/md 两种格式")
    public ResponseEntity<?> export(@PathVariable Long id,
                                    @RequestParam(defaultValue = "json") String format) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Result.error(401, "请先登录"));
        }

        HealthReportV2VO report = healthReportV2Service.getById(id);
        if (report == null) {
            return ResponseEntity.status(404).body(Result.error(404, "报告不存在"));
        }
        if (!userId.equals(report.getUserId())) {
            return ResponseEntity.status(403).body(Result.error(403, "无权访问此报告"));
        }

        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(report);
        }
        if ("md".equalsIgnoreCase(format)) {
            String markdown = markdownRenderer.render(report);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"health-report-" + id + ".md\"")
                    .contentType(MediaType.parseMediaType("text/markdown; charset=utf-8"))
                    .body(markdown);
        }
        return ResponseEntity.badRequest().body(Result.error(400, "format 仅支持 json 或 md"));
    }
}
