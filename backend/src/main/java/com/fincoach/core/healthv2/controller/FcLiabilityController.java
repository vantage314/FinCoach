package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.CreateLiabilityDTO;
import com.fincoach.core.healthv2.dto.UpdateLiabilityDTO;
import com.fincoach.core.healthv2.entity.FcLiabilityEntity;
import com.fincoach.core.healthv2.service.FcLiabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 体检v2-负债管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/app/liabilities")
@Tag(name = "体检v2-负债", description = "负债 CRUD")
public class FcLiabilityController {

    @Autowired
    private FcLiabilityService liabilityService;

    @PostMapping
    @Operation(summary = "新增负债")
    public Result<FcLiabilityEntity> create(@Valid @RequestBody CreateLiabilityDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(liabilityService.create(userId, dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新负债")
    public Result<FcLiabilityEntity> update(@PathVariable Long id, @Valid @RequestBody UpdateLiabilityDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        dto.setId(id);
        return Result.success(liabilityService.update(userId, dto));
    }

    @GetMapping
    @Operation(summary = "查询用户负债列表")
    public Result<List<FcLiabilityEntity>> list() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(liabilityService.listByUserId(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除负债")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        liabilityService.delete(userId, id);
        return Result.success("删除成功");
    }
}
