package com.fincoach.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.common.Result;
import com.fincoach.core.repository.entity.TransactionRecord;
import com.fincoach.core.service.TransactionService;
import com.fincoach.core.common.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "交易流水", description = "资金变动记录管理")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Operation(summary = "获取交易流水", description = "分页查询当前用户的资金变动记录")
    @GetMapping
    public Result<Page<TransactionRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getCurrentUserId();
        Page<TransactionRecord> result = transactionService.getUserTransactions(userId, page, size);
        return Result.success(result);
    }
}
