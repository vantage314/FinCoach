package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

@Data
public class AdminSecuritiesSnapshotItemDTO {
    private String date;
    private String assetKey;
    private String source;
    private String currency;
    private String price;
}
