package com.fincoach.core.healthv2.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcBehaviorEventEntity;
import com.fincoach.core.healthv2.service.BehaviorEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/app/behavior-events")
@Tag(name = "App-BehaviorEvents", description = "行为事件查询")
public class BehaviorEventController {

    @Autowired
    private BehaviorEventService behaviorEventService;

    @GetMapping
    @Operation(summary = "分页查询最近行为事件")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        IPage<FcBehaviorEventEntity> eventPage = behaviorEventService.listByUser(userId, page, size, type);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", eventPage.getRecords());
        data.put("page", page);
        data.put("size", size);
        data.put("total", eventPage.getTotal());
        return Result.success(data);
    }
}
