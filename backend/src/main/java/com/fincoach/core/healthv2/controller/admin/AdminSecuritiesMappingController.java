package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesMappingListDTO;
import com.fincoach.core.security.AdminOnly;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingQueryDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingSaveDTO;
import com.fincoach.core.ticker.service.admin.AdminTickerMappingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/api/securities/mapping")
@Tag(name = "Admin-Securities-Mapping", description = "Securities Ticker Mapping")
@AdminOnly
public class AdminSecuritiesMappingController {

    private final AdminTickerMappingService service;

    public AdminSecuritiesMappingController(AdminTickerMappingService service) {
        this.service = service;
    }

    @GetMapping
    public Result<AdminSecuritiesMappingListDTO> list(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Integer enabled,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        AdminTickerMappingQueryDTO query = new AdminTickerMappingQueryDTO();
        query.setKeyword(keyword);
        query.setEnabled(enabled);
        query.setPage(page);
        query.setSize(size);

        IPage<AdminTickerMappingDTO> result = service.page(query);
        AdminSecuritiesMappingListDTO dto = new AdminSecuritiesMappingListDTO();
        dto.setItems(result.getRecords());
        dto.setTotal(result.getTotal());
        dto.setPage((int) result.getCurrent());
        dto.setSize((int) result.getSize());

        log.info("event=SEC_MAPPING_LIST userId={} keyword={} enabled={} total={}",
                UserContext.getCurrentUserId(), keyword, enabled, result.getTotal());
        return Result.success(dto);
    }

    @PostMapping
    public Result<Long> create(@RequestBody AdminTickerMappingSaveDTO request) {
        Long id = service.save(request);
        log.info("event=SEC_MAPPING_CREATE userId={} id={}", UserContext.getCurrentUserId(), id);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody AdminTickerMappingSaveDTO request) {
        if (request == null) {
            return Result.error(400, "empty request");
        }
        request.setId(id);
        Long savedId = service.save(request);
        log.info("event=SEC_MAPPING_UPDATE userId={} id={}", UserContext.getCurrentUserId(), savedId);
        return Result.success(savedId);
    }

    @PostMapping("/{id}/toggle")
    public Result<String> toggle(@PathVariable Long id, @RequestParam boolean enabled) {
        if (enabled) {
            service.enable(id);
        } else {
            service.disable(id);
        }
        log.info("event=SEC_MAPPING_TOGGLE userId={} id={} enabled={}",
                UserContext.getCurrentUserId(), id, enabled);
        return Result.success(enabled ? "enabled" : "disabled");
    }

    @DeleteMapping("/{id}")
    public Result<String> disable(@PathVariable Long id) {
        service.disable(id);
        log.info("event=SEC_MAPPING_TOGGLE userId={} id={} enabled=false", UserContext.getCurrentUserId(), id);
        return Result.success("disabled");
    }
}
