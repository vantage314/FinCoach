package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminJobActionResultDTO {
    private String jobName;
    private String status;
    private String message;
}
