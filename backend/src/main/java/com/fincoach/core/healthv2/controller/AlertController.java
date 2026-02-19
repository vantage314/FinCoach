package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcAlertEntity;
import com.fincoach.core.healthv2.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/app/alerts")
@Tag(name = "App-Alerts", description = "用户预警列表")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping("/list")
    @Operation(summary = "获取用户预警列表")
    public Result<List<FcAlertEntity>> list(@RequestParam(required = false) String status) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(alertService.listUserAlerts(userId, status));
    }
}
