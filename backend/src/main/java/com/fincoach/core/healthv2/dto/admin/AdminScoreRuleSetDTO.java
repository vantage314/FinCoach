package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminScoreRuleSetDTO {
    private Long id;
    private String code;
    private String name;
    private Integer version;
    private Integer enabled;
    private LocalDateTime publishedAt;
    private String source;
    private List<String> missingParams = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<AdminScoreRuleParamDTO> params = new ArrayList<>();
}
