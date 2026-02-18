package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminCrawlerEventDTO {
    private String ts;
    private String type;
    private String msg;
}
