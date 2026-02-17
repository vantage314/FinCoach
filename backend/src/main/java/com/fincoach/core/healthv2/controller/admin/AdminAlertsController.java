package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminAlertAckRequest;
import com.fincoach.core.healthv2.dto.admin.AdminAlertListDTO;
import com.fincoach.core.healthv2.entity.FcAlertRecordEntity;
import com.fincoach.core.healthv2.mapper.FcAlertRecordMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.security.AdminOnly;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/api/alerts")
@Tag(name = "Admin-Alerts", description = "Alert list and ack")
@AdminOnly
public class AdminAlertsController {
    private final FcAlertRecordMapper recordMapper;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public AdminAlertsController(FcAlertRecordMapper recordMapper, ObjectMapper objectMapper, AuditService auditService) {
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @GetMapping
    public Result<AdminAlertListDTO> list(@RequestParam(required = false) Integer limit,
                                          @RequestParam(required = false) String status) {
        int safeLimit = limit == null || limit <= 0 ? 200 : Math.min(limit, 1000);
        String statusFilter = status == null ? "OPEN" : status.trim().toUpperCase();

        LambdaQueryWrapper<FcAlertRecordEntity> qw = new LambdaQueryWrapper<>();
        if ("OPEN".equals(statusFilter)) {
            qw.in(FcAlertRecordEntity::getStatus, List.of("OPEN", "ACTIVE"));
        } else if ("ACKED".equals(statusFilter)) {
            qw.eq(FcAlertRecordEntity::getStatus, "ACKED");
        }
        qw.orderByDesc(FcAlertRecordEntity::getCreatedAt).last("LIMIT " + safeLimit);

        List<FcAlertRecordEntity> records = recordMapper.selectList(qw);
        List<AdminAlertListDTO.AlertItem> items = new ArrayList<>();
        if (records != null) {
            for (FcAlertRecordEntity record : records) {
                if (record == null) continue;
                AdminAlertListDTO.AlertItem item = new AdminAlertListDTO.AlertItem();
                item.setAlertId(record.getId());
                item.setCode(record.getRuleKey());
                item.setSeverity(record.getSeverity());
                item.setTitle(record.getMessage());
                item.setStatus(normalizeStatus(record.getStatus()));
                item.setCreatedAt(record.getCreatedAt() == null ? null : record.getCreatedAt().toString());
                item.setReportId(extractReportId(record.getPayloadJson()));
                items.add(item);
            }
        }
        items.sort((a, b) -> {
            int sa = severityRank(a.getSeverity());
            int sb = severityRank(b.getSeverity());
            if (sa != sb) return Integer.compare(sb, sa);
            String ta = a.getCreatedAt() == null ? "" : a.getCreatedAt();
            String tb = b.getCreatedAt() == null ? "" : b.getCreatedAt();
            return tb.compareTo(ta);
        });
        if (items.size() > safeLimit) {
            items = new ArrayList<>(items.subList(0, safeLimit));
        }

        AdminAlertListDTO dto = new AdminAlertListDTO();
        dto.setTotal(items.size());
        dto.setItems(items);
        return Result.success(dto);
    }

    @PostMapping("/ack")
    public Result<String> ack(@RequestBody AdminAlertAckRequest request) {
        if (request == null) {
            return Result.error(400, "empty request");
        }
        int updated = 0;
        List<Long> ackedIds = new ArrayList<>();

        if (request.getAlerts() != null && !request.getAlerts().isEmpty()) {
            for (AdminAlertAckRequest.AlertRef ref : request.getAlerts()) {
                if (ref == null || ref.getAlertId() == null) continue;
                FcAlertRecordEntity entity = new FcAlertRecordEntity();
                entity.setId(ref.getAlertId());
                entity.setStatus("ACKED");
                recordMapper.updateById(entity);
                updated++;
                ackedIds.add(ref.getAlertId());
            }
        } else if (request.getReportId() != null && request.getCodes() != null && !request.getCodes().isEmpty()) {
            updated = ackByReportAndCodes(request.getReportId(), request.getCodes(), ackedIds);
        } else {
            return Result.error(400, "missing alerts or reportId+codes");
        }

        Long adminId = UserContext.getCurrentUserId();
        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("count", updated);
        auditPayload.put("alertIds", ackedIds);
        auditPayload.put("reportId", request.getReportId());
        auditPayload.put("codes", request.getCodes() == null ? Collections.emptyList() : request.getCodes());
        auditService.log(adminId, "ACK_ALERTS", "ALERT_RECORD", null, null, auditPayload);
        log.info("[Admin-Alert] event=ADMIN_ALERT_ACK userId={} count={}", adminId, updated);
        return Result.success("ACKED:" + updated);
    }

    private int ackByReportAndCodes(Long reportId, List<String> codes, List<Long> ackedIds) {
        LambdaQueryWrapper<FcAlertRecordEntity> qw = new LambdaQueryWrapper<>();
        qw.in(FcAlertRecordEntity::getStatus, List.of("OPEN", "ACTIVE"));
        qw.in(FcAlertRecordEntity::getRuleKey, codes);
        qw.orderByDesc(FcAlertRecordEntity::getCreatedAt).last("LIMIT 1000");
        List<FcAlertRecordEntity> records = recordMapper.selectList(qw);
        if (records == null || records.isEmpty()) return 0;
        int updated = 0;
        for (FcAlertRecordEntity record : records) {
            if (record == null) continue;
            Long payloadReportId = extractReportId(record.getPayloadJson());
            if (payloadReportId == null || !payloadReportId.equals(reportId)) continue;
            FcAlertRecordEntity entity = new FcAlertRecordEntity();
            entity.setId(record.getId());
            entity.setStatus("ACKED");
            recordMapper.updateById(entity);
            updated++;
            ackedIds.add(record.getId());
        }
        return updated;
    }

    private Long extractReportId(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return null;
        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
            Object reportId = payload.get("reportId");
            if (reportId instanceof Number number) return number.longValue();
            if (reportId != null) return Long.parseLong(reportId.toString());
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String normalizeStatus(String status) {
        if (status == null) return "OPEN";
        String normalized = status.trim().toUpperCase();
        if ("ACTIVE".equals(normalized)) return "OPEN";
        return normalized;
    }

    private int severityRank(String severity) {
        if (severity == null) return 0;
        String normalized = severity.toUpperCase();
        if ("CRITICAL".equals(normalized)) return 3;
        if ("WARN".equals(normalized)) return 2;
        if ("INFO".equals(normalized)) return 1;
        return 0;
    }
}
