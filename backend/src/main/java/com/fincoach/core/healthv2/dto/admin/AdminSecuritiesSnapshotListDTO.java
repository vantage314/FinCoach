package com.fincoach.core.healthv2.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminSecuritiesSnapshotListDTO {
    private List<AdminSecuritiesSnapshotItemDTO> items;
    private long total;
    private AdminSecuritiesSnapshotSummaryDTO summary;
}
