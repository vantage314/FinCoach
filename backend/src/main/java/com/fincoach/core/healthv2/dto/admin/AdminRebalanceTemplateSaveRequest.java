package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminRebalanceTemplateSaveRequest {
    private Long id;
    private String code;
    private String name;
    private String templateJson;
}
