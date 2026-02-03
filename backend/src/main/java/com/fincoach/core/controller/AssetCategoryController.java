package com.fincoach.core.controller;

import com.fincoach.core.common.AssetCategoryEnum;
import com.fincoach.core.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/asset")
@Tag(name = "数据字典模块", description = "提供资产分类等基础数据查询")
public class AssetCategoryController {

    @GetMapping("/categories")
    @Operation(summary = "获取资产分类列表", description = "返回所有资产分类的 ID、名称、表单类型和图标")
    public Result<List<Map<String, Object>>> getCategories() {
        List<Map<String, Object>> categories = Arrays.stream(AssetCategoryEnum.values())
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("name", c.getName());
                    map.put("formType", c.getFormType());
                    map.put("iconSlug", c.getIconSlug());
                    return map;
                })
                .collect(Collectors.toList());
        return Result.success(categories);
    }
}
