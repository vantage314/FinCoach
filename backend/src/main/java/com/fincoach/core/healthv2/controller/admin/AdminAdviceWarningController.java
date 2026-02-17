package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.Result;
import com.fincoach.core.healthv2.dto.admin.AdminAdviceWarningStatsDTO;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import com.fincoach.core.security.AdminOnly;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/advice/warnings")
@Tag(name = "Admin-AdviceWarnings", description = "Advice warnings stats")
@AdminOnly
public class AdminAdviceWarningController {
    private final FcHealthReportMapper reportMapper;
    private final ObjectMapper objectMapper;

    public AdminAdviceWarningController(FcHealthReportMapper reportMapper, ObjectMapper objectMapper) {
        this.reportMapper = reportMapper;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/stats")
    public Result<AdminAdviceWarningStatsDTO> stats(@RequestParam(required = false) Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 200 : Math.min(limit, 1000);
        List<FcHealthReportEntity> reports = reportMapper.selectList(
                new LambdaQueryWrapper<FcHealthReportEntity>()
                        .orderByDesc(FcHealthReportEntity::getReportDate)
                        .last("LIMIT " + safeLimit));
        AdminAdviceWarningStatsDTO dto = buildStats(reports);
        return Result.success(dto);
    }

    private AdminAdviceWarningStatsDTO buildStats(List<FcHealthReportEntity> reports) {
        AdminAdviceWarningStatsDTO dto = new AdminAdviceWarningStatsDTO();
        if (reports == null) {
            return dto;
        }

        int total = reports.size();
        dto.setTotalReports(total);

        Map<String, Integer> counts = new LinkedHashMap<>();
        List<AdminAdviceWarningStatsDTO.LatestSample> latestSamples = new ArrayList<>();

        for (int i = 0; i < reports.size(); i++) {
            FcHealthReportEntity report = reports.get(i);
            List<String> codes = extractWarnings(report == null ? null : report.getAdviceJson());
            for (String code : codes) {
                counts.put(code, counts.getOrDefault(code, 0) + 1);
            }
            if (i < 5) {
                AdminAdviceWarningStatsDTO.LatestSample sample = new AdminAdviceWarningStatsDTO.LatestSample();
                sample.setReportId(report == null ? null : report.getId());
                sample.setCodes(new ArrayList<>(codes));
                latestSamples.add(sample);
            }
        }

        List<AdminAdviceWarningStatsDTO.CodeStat> stats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            AdminAdviceWarningStatsDTO.CodeStat stat = new AdminAdviceWarningStatsDTO.CodeStat();
            stat.setCode(entry.getKey());
            stat.setCount(entry.getValue());
            stat.setRatio(calcRatio(entry.getValue(), total));
            stats.add(stat);
        }
        stats.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));

        dto.setByCode(stats);
        dto.setLatestSamples(latestSamples);
        return dto;
    }

    private double calcRatio(int count, int total) {
        if (total <= 0) return 0.0;
        BigDecimal ratio = BigDecimal.valueOf((double) count / (double) total)
                .setScale(4, RoundingMode.HALF_UP);
        return ratio.doubleValue();
    }

    private List<String> extractWarnings(String adviceJson) {
        if (adviceJson == null || adviceJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> adviceMap = objectMapper.readValue(adviceJson,
                    new TypeReference<Map<String, Object>>() {});
            Object adviceV2 = adviceMap.get("adviceV2");
            if (adviceV2 instanceof Map<?, ?> v2Map) {
                Object meta = v2Map.get("meta");
                if (meta instanceof Map<?, ?> metaMap) {
                    Object warnings = metaMap.get("warnings");
                    if (warnings instanceof List<?> list) {
                        List<String> result = new ArrayList<>();
                        for (Object item : list) {
                            if (item == null) continue;
                            result.add(item.toString());
                        }
                        return result;
                    }
                }
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
