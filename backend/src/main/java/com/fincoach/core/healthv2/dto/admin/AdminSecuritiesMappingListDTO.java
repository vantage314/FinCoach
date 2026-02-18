package com.fincoach.core.healthv2.dto.admin;

import com.fincoach.core.ticker.dto.admin.AdminTickerMappingDTO;
import lombok.Data;

import java.util.List;

@Data
public class AdminSecuritiesMappingListDTO {
    private List<AdminTickerMappingDTO> items;
    private long total;
    private int page;
    private int size;
}
