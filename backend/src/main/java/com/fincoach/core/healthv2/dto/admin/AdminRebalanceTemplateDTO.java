package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminRebalanceTemplateDTO {
    private Long id;
    private String code;
    private String name;
    private Integer version;
    private Integer enabled;
    private String templateJson;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
