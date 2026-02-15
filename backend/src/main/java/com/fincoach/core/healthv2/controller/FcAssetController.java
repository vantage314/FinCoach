package com.fincoach.core.healthv2.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.CreateAssetDTO;
import com.fincoach.core.healthv2.dto.UpdateAssetDTO;
import com.fincoach.core.healthv2.entity.FcAssetEntity;
import com.fincoach.core.healthv2.service.FcAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 体检v2-资产管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/app/assets")
@Tag(name = "体检v2-资产", description = "资产 CRUD")
public class FcAssetController {

    @Autowired
    private FcAssetService assetService;

    @PostMapping
    @Operation(summary = "新增资产")
    public Result<FcAssetEntity> create(@Valid @RequestBody CreateAssetDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(assetService.create(userId, dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新资产")
    public Result<FcAssetEntity> update(@PathVariable Long id, @Valid @RequestBody UpdateAssetDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        dto.setId(id);
        return Result.success(assetService.update(userId, dto));
    }

    @GetMapping
    @Operation(summary = "查询用户资产列表")
    public Result<List<FcAssetEntity>> list() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        return Result.success(assetService.listByUserId(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除资产")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error(401, "请先登录");
        assetService.delete(userId, id);
        return Result.success("删除成功");
    }
}
