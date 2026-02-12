package com.fincoach.core.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.controller.dto.ChatRequest;
import com.fincoach.core.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * AI 咨询控制器
 * Phase 13.2：DeepSeek API 深度集成
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        // 简单校验
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Result.error(400, "请输入问题");
        }
        
        log.info("[AiController] 收到聊天请求: {}", request.getMessage());
        
        // 调用 AI
        Long userId = UserContext.getCurrentUserId();
        String response = aiService.chat(userId, request.getMessage());
        return Result.success(response);
    }
}
