package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.CreateGoalDTO;
import com.fincoach.core.healthv2.dto.UpdateGoalDTO;
import com.fincoach.core.healthv2.entity.FcGoalEntity;
import com.fincoach.core.healthv2.service.FcGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 体检v2-目标管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/app/goals")
@Tag(name = "体检v2-目标", description = "目标 CRUD")
public class FcGoalController {

    @Autowired
    private FcGoalService goalService;

    @PostMapping
    @Operation(summary = "新增目标")
    public Result<FcGoalEntity> create(@Valid @RequestBody CreateGoalDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(goalService.create(userId, dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新目标")
    public Result<FcGoalEntity> update(@PathVariable Long id, @Valid @RequestBody UpdateGoalDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        dto.setId(id);
        return Result.success(goalService.update(userId, dto));
    }

    @GetMapping
    @Operation(summary = "查询用户目标列表")
    public Result<List<FcGoalEntity>> list() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(goalService.listByUserId(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除目标")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        goalService.delete(userId, id);
        return Result.success("删除成功");
    }
}
