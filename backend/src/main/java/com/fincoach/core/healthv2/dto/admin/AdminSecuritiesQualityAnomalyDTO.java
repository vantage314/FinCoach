package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminSecuritiesQualityAnomalyDTO {
    private String assetKey;
    private String date;
    private double changePct;
    private String reason;
}
