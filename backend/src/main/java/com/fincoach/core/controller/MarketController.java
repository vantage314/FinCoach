package com.fincoach.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.common.Result;
import com.fincoach.core.repository.entity.MarketSecurity;
import com.fincoach.core.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/market")
@Tag(name = "市场接口", description = "提供行情、新闻及指数数据")
public class MarketController {

    @Autowired
    private MarketService marketService;

    @Operation(summary = "获取证券列表 (分页)")
    @GetMapping("/securities")
    public Result<IPage<MarketSecurity>> getSecurities(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        
        Page<MarketSecurity> pageParam = new Page<>(page, size);
        IPage<MarketSecurity> result = marketService.getSecurities(pageParam, type, keyword);
        return Result.success(result);
    }
}
