package com.fincoach.core.healthv2.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class ConfigJsonHelper {

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public ConfigJsonHelper(ObjectMapper objectMapper, AuditService auditService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    public void validateJsonOrThrow(String json, String fieldName) {
        if (json == null) {
            return;
        }
        String trimmed = json.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            objectMapper.readTree(trimmed);
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " JSON invalid");
        }
    }

    public String parseOrDefault(String json,
                                 String defaultJson,
                                 Long actorUserId,
                                 String targetType,
                                 Long targetId,
                                 String fieldName) {
        if (json == null) {
            return defaultJson;
        }
        String trimmed = json.trim();
        if (trimmed.isEmpty()) {
            logParseFail(actorUserId, targetType, targetId, fieldName, json, defaultJson, null);
            return defaultJson;
        }
        try {
            objectMapper.readTree(trimmed);
            return json;
        } catch (Exception e) {
            logParseFail(actorUserId, targetType, targetId, fieldName, json, defaultJson, e);
            return defaultJson;
        }
    }

    private void logParseFail(Long actorUserId,
                              String targetType,
                              Long targetId,
                              String fieldName,
                              String value,
                              String fallback,
                              Exception exception) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("field", fieldName);
        before.put("value", value);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("fallback", fallback);
        auditService.log(actorUserId, "CONFIG_PARSE_FAIL", targetType, targetId, before, after);
        if (exception != null) {
            log.warn("[ConfigJson] parse failed, targetType={}, targetId={}, field={}", targetType, targetId, fieldName, exception);
        } else {
            log.warn("[ConfigJson] empty value, targetType={}, targetId={}, field={}", targetType, targetId, fieldName);
        }
    }
}
