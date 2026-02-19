package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcNotificationEntity;
import com.fincoach.core.healthv2.service.NotificationService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/app/notifications")
@Tag(name = "App-Notifications", description = "通知中心")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取通知列表")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1") int page) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        if (isRead != null && isRead != 0 && isRead != 1) {
            return Result.error(400, "isRead 仅支持 0/1");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        IPage<FcNotificationEntity> pageData = notificationService.list(userId, isRead, safePage, safeSize);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", pageData.getRecords());
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("total", pageData.getTotal());
        data.put("unreadCount", notificationService.countUnread(userId));
        return Result.success(data);
    }

    @GetMapping("/list")
    @Operation(summary = "获取通知列表（别名）")
    public Result<Map<String, Object>> listAlias(
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1") int page) {
        return list(isRead, size, page);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条通知已读")
    public Result<String> markRead(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        FcNotificationEntity entity = notificationService.getById(id);
        if (entity == null) {
            return Result.error(404, "通知不存在");
        }
        if (!userId.equals(entity.getUserId())) {
            return Result.error(403, "无权操作此通知");
        }
        notificationService.markRead(userId, id);
        return Result.success("已标记已读");
    }

    @PostMapping("/read")
    @Operation(summary = "标记单条通知已读（别名）")
    public Result<String> markReadAlias(@RequestBody ReadRequest request) {
        if (request == null || request.getId() == null) {
            return Result.error(400, "缺少通知ID");
        }
        return markRead(request.getId());
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public Result<String> markAllRead() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        notificationService.markAllRead(userId);
        return Result.success("已全部标记已读");
    }

    @PostMapping("/readAll")
    @Operation(summary = "全部标记已读（别名）")
    public Result<String> markAllReadAlias() {
        return markAllRead();
    }

    public static class ReadRequest {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
