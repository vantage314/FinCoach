package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminUserToggleRequest {
    private Long userId;
    private Boolean enabled;
}
