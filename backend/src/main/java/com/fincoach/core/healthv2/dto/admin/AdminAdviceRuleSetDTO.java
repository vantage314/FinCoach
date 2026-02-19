package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAdviceRuleSetDTO {
    private Long id;
    private String code;
    private Integer version;
    private Integer enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
