package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesSnapshotItemDTO;
import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesSnapshotListDTO;
import com.fincoach.core.healthv2.dto.admin.AdminSecuritiesSnapshotSummaryDTO;
import com.fincoach.core.healthv2.entity.FcPortfolioPriceSnapshotEntity;
import com.fincoach.core.healthv2.mapper.FcPortfolioPriceSnapshotMapper;
import com.fincoach.core.security.AdminOnly;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/api/securities/snapshots")
@Tag(name = "Admin-Securities-Snapshots", description = "Portfolio snapshots list")
@AdminOnly
public class AdminSecuritiesSnapshotController {

    private final FcPortfolioPriceSnapshotMapper snapshotMapper;

    public AdminSecuritiesSnapshotController(FcPortfolioPriceSnapshotMapper snapshotMapper) {
        this.snapshotMapper = snapshotMapper;
    }

    @GetMapping
    public Result<AdminSecuritiesSnapshotListDTO> list(@RequestParam(required = false) String assetKey,
                                                       @RequestParam(required = false) String startDate,
                                                       @RequestParam(required = false) String endDate,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);

        QueryWrapper<FcPortfolioPriceSnapshotEntity> qw = new QueryWrapper<>();
        applyFilters(qw, assetKey, start, end);
        qw.orderByDesc("snap_date");

        IPage<FcPortfolioPriceSnapshotEntity> result = snapshotMapper.selectPage(new Page<>(page, size), qw);
        List<AdminSecuritiesSnapshotItemDTO> items = new ArrayList<>();
        for (FcPortfolioPriceSnapshotEntity entity : result.getRecords()) {
            if (entity == null) continue;
            AdminSecuritiesSnapshotItemDTO item = new AdminSecuritiesSnapshotItemDTO();
            item.setDate(entity.getAsOfDate() == null ? null : entity.getAsOfDate().toString());
            item.setAssetKey(entity.getDataSource());
            item.setSource(entity.getDataSource());
            item.setCurrency(entity.getBaseCurrency());
            item.setPrice(entity.getEquity() == null ? null : entity.getEquity().toPlainString());
            items.add(item);
        }

        SnapshotSummary summaryRow = fetchSummary(assetKey, start, end);

        AdminSecuritiesSnapshotSummaryDTO summary = new AdminSecuritiesSnapshotSummaryDTO();
        summary.setAssetsCount(summaryRow.assetsCount);
        summary.setSampleSize(result.getTotal());
        summary.setStartDate(start != null ? start.toString() : toDateString(summaryRow.minDate));
        summary.setEndDate(end != null ? end.toString() : toDateString(summaryRow.maxDate));
        summary.setLatestDate(toDateString(summaryRow.maxDate));

        AdminSecuritiesSnapshotListDTO dto = new AdminSecuritiesSnapshotListDTO();
        dto.setItems(items);
        dto.setTotal(result.getTotal());
        dto.setSummary(summary);

        log.info("event=SEC_SNAPSHOT_QUERY userId={} assetKey={} startDate={} endDate={} total={}",
                UserContext.getCurrentUserId(), assetKey, startDate, endDate, result.getTotal());

        return Result.success(dto);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void applyFilters(QueryWrapper<FcPortfolioPriceSnapshotEntity> qw, String assetKey, LocalDate start, LocalDate end) {
        if (assetKey != null && !assetKey.trim().isEmpty()) {
            qw.eq("source", assetKey.trim());
        }
        if (start != null) {
            qw.ge("snap_date", start);
        }
        if (end != null) {
            qw.le("snap_date", end);
        }
    }

    private SnapshotSummary fetchSummary(String assetKey, LocalDate start, LocalDate end) {
        QueryWrapper<FcPortfolioPriceSnapshotEntity> summaryQw = new QueryWrapper<>();
        applyFilters(summaryQw, assetKey, start, end);
        summaryQw.select("COUNT(DISTINCT source) AS assetsCount",
                "MIN(snap_date) AS minDate",
                "MAX(snap_date) AS maxDate");
        List<Map<String, Object>> rows = snapshotMapper.selectMaps(summaryQw);
        SnapshotSummary summary = new SnapshotSummary();
        if (rows == null || rows.isEmpty()) {
            return summary;
        }
        Map<String, Object> row = rows.get(0);
        summary.assetsCount = toLong(row.get("assetsCount"));
        summary.minDate = toLocalDate(row.get("minDate"));
        summary.maxDate = toLocalDate(row.get("maxDate"));
        return summary;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String toDateString(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private static class SnapshotSummary {
        private long assetsCount;
        private LocalDate minDate;
        private LocalDate maxDate;
    }
}
