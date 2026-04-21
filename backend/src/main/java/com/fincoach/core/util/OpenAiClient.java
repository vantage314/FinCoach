package com.fincoach.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * DeepSeek API 客户端 (基于 OpenAI 协议标准)
 * 文档: https://api-docs.deepseek.com/zh-cn/
 */
@Slf4j
@Component
public class OpenAiClient {

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    /** 专用 RestTemplate：连接超时 10s，读取超时 60s */
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);  // 连接超时 10 秒
        factory.setReadTimeout(60_000);     // 读取超时 60 秒
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 调用 DeepSeek Chat API
     * @param systemPrompt 系统提示词（设定人设）
     * @param userMessage 用户消息（包含上下文和问题）
     * @return AI 回复内容
     */
    public String callChat(String systemPrompt, String userMessage) {
        try {
            // DeepSeek 聊天补全接口
            String url = baseUrl + "/chat/completions";

            // 1. 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 2. 构建消息体
            List<Map<String, String>> messages = new ArrayList<>();
            // 系统角色 (设定人设)
            messages.add(Map.of("role", "system", "content", systemPrompt));
            // 用户角色 (实际问题)
            messages.add(Map.of("role", "user", "content", userMessage));

            // 3. 构建请求参数
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", 1.3); // DeepSeek 建议 V3 设置 1.3 以获得更有创造性的回答
            body.put("stream", false);    // 暂不流式，简化前端对接

            // 4. 发送请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            log.info("[OpenAiClient] 正在调用 DeepSeek API, model: {}", model);
            
            // 发送 POST
            String response = restTemplate.postForObject(url, entity, String.class);

            // 5. 解析响应 (OpenAI 标准格式)
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                // 路径: choices[0].message.content
                String content = root.path("choices").get(0).path("message").path("content").asText();
                log.info("[OpenAiClient] 成功获取 AI 回复，长度: {} 字符", content.length());
                return content;
            }
        } catch (Exception e) {
            log.error("[OpenAiClient] DeepSeek API 调用失败", e);
            return "⚠️ **AI 服务暂时不可用**\n\n错误详情: " + e.getMessage() + "\n\n请检查 API Key 是否欠费或过期。";
        }
        return "⚠️ AI 未返回任何内容，请重试。";
    }
}
