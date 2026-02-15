package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.common.Result;
import com.fincoach.core.healthv2.entity.FcAlertRecordEntity;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;
import com.fincoach.core.healthv2.mapper.FcAlertRecordMapper;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import com.fincoach.core.repository.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin-Dashboard", description = "后台仪表盘统计")
public class DashboardController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FcHealthReportMapper reportMapper;
    @Autowired
    private FcAlertRecordMapper alertRecordMapper;

    @GetMapping("/stats")
    @Operation(summary = "全局统计")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("totalReports", reportMapper.selectCount(null));
        data.put("activeAlerts", alertRecordMapper.selectCount(
                new LambdaQueryWrapper<FcAlertRecordEntity>().eq(FcAlertRecordEntity::getStatus, "ACTIVE")));
        return Result.success(data);
    }
}
