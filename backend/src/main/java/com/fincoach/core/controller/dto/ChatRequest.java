package com.fincoach.core.controller.dto;

import lombok.Data;

/**
 * AI 对话请求 DTO
 */
@Data
public class ChatRequest {
    /**
     * 用户输入的消息内容
     */
    private String message;
}
