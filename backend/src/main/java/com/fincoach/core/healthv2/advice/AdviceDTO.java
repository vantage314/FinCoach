package com.fincoach.core.healthv2.advice;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class AdviceDTO {
    private String code;
    private String title;
    private String priority;
    private String reason;
    private String impact;
    private String action;
    private Map<String, Object> evidence;
    private List<String> tags = new ArrayList<>();
}
