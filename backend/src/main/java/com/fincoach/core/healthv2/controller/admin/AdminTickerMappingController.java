package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fincoach.core.common.Result;
import com.fincoach.core.security.AdminOnly;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingQueryDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingSaveDTO;
import com.fincoach.core.ticker.service.admin.AdminTickerMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ticker-mapping")
@Tag(name = "Admin-Ticker-Mapping", description = "Ticker Mapping 管理")
@AdminOnly
public class AdminTickerMappingController {

    private final AdminTickerMappingService service;

    public AdminTickerMappingController(AdminTickerMappingService service) {
        this.service = service;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询 Ticker Mapping")
    public Result<IPage<AdminTickerMappingDTO>> page(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String ticker,
                                                     @RequestParam(required = false) Integer enabled,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        AdminTickerMappingQueryDTO query = new AdminTickerMappingQueryDTO();
        query.setKeyword(keyword);
        query.setTicker(ticker);
        query.setEnabled(enabled);
        query.setPage(page);
        query.setSize(size);
        return Result.success(service.page(query));
    }

    @PostMapping("/save")
    @Operation(summary = "新增/更新 Ticker Mapping")
    public Result<Long> save(@RequestBody AdminTickerMappingSaveDTO dto) {
        return Result.success(service.save(dto));
    }

    @PostMapping("/enable")
    @Operation(summary = "启用 Ticker Mapping")
    public Result<String> enable(@RequestParam Long id) {
        service.enable(id);
        return Result.success("enabled");
    }

    @PostMapping("/disable")
    @Operation(summary = "停用 Ticker Mapping")
    public Result<String> disable(@RequestParam Long id) {
        service.disable(id);
        return Result.success("disabled");
    }

    @PostMapping("/reload")
    @Operation(summary = "重载 Ticker Mapping Registry")
    public Result<String> reload() {
        service.reload();
        return Result.success("reloaded");
    }
}
