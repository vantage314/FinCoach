package com.fincoach.core.ticker.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingQueryDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingSaveDTO;

public interface AdminTickerMappingService {
    IPage<AdminTickerMappingDTO> page(AdminTickerMappingQueryDTO query);
    Long save(AdminTickerMappingSaveDTO dto);
    void enable(Long id);
    void disable(Long id);
    void reload();
}
