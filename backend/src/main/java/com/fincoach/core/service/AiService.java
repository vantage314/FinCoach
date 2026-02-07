package com.fincoach.core.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fincoach.core.repository.entity.FinancialNews;
import com.fincoach.core.repository.entity.MarketSecurity;
import com.fincoach.core.repository.mapper.FinancialNewsMapper;
import com.fincoach.core.repository.mapper.MarketSecurityMapper;
import com.fincoach.core.util.OpenAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 智能投顾服务
 * 实现 RAG (检索增强生成) 逻辑
 */
@Slf4j
@Service
public class AiService {

    @Autowired
    private MarketSecurityMapper securityMapper;
    
    @Autowired
    private FinancialNewsMapper newsMapper;
    
    @Autowired
    private OpenAiClient openAiClient;

    /**
     * 系统提示词 (System Prompt)
     * 定义 AI 的人设和回答规则
     */
    private static final String SYSTEM_PROMPT = """
            你是一个专业的金融投资顾问 'FinCoach AI'。
            你的任务是基于【本地数据库上下文】回答用户关于股票、基金或市场的问题。
            
            规则：
            1. 必须优先使用上下文中的数据（现价、涨跌幅、新闻等）。
            2. 如果上下文没有数据，可以使用你的通用知识，但要声明"数据库中暂无此标的数据"。
            3. 回答风格要专业、客观，使用 Markdown 格式（加粗关键数据）。
            4. 风险提示：文末必须简短提示"投资有风险"。
            """;

    /**
     * 处理用户对话，返回 AI 回复
     * @param userMessage 用户消息
     * @return AI 回复（Markdown 格式）
     */
    public String chat(String userMessage) {
        log.info("[AiService] 收到用户消息: {}", userMessage);
        
        // --- 1. RAG 检索阶段 (Retrieval) ---
        StringBuilder context = new StringBuilder();
        context.append("【本地数据库检索结果】\n");

        boolean hasContext = false;

        // A. 检索行情 (模糊匹配用户提到的股票)
        List<MarketSecurity> securities = securityMapper.selectList(
            new QueryWrapper<MarketSecurity>().last("LIMIT 100")
        );
        
        for (MarketSecurity s : securities) {
            // 如果用户的话里包含股票名称或代码
            if (userMessage.contains(s.getName()) || userMessage.contains(s.getCode())) {
                context.append(String.format("- 标的: **%s** (%s)\n", s.getName(), s.getCode()));
                context.append(String.format("  - 最新价: %s (涨跌: %s%%)\n", s.getCurrentPrice(), s.getChangePercent()));
                context.append(String.format("  - 行业: %s | 风险: %s | 描述: %s\n", s.getSector(), s.getRiskLevel(), s.getDescription()));
                
                // B. 顺便检索该股票的新闻
                List<FinancialNews> relatedNews = newsMapper.selectList(
                    new QueryWrapper<FinancialNews>()
                        .eq("related_code", s.getCode())
                        .orderByDesc("publish_time")
                        .last("LIMIT 2")
                );
                if (!relatedNews.isEmpty()) {
                    context.append("  - 关联新闻:\n");
                    for (FinancialNews n : relatedNews) {
                        context.append(String.format("    * [%s] %s\n", 
                            n.getPublishTime().toLocalDate(), n.getTitle()));
                    }
                }
                hasContext = true;
                context.append("\n");
            }
        }

        // C. 如果没有匹配到个股，且用户问市场/宏观，注入最近 3 条宏观新闻
        if (!hasContext && (userMessage.contains("市场") || userMessage.contains("新闻") || 
                           userMessage.contains("大盘") || userMessage.contains("建议"))) {
            List<FinancialNews> latestNews = newsMapper.selectList(
                new QueryWrapper<FinancialNews>()
                    .orderByDesc("publish_time")
                    .last("LIMIT 3")
            );
            context.append("- 最近市场热点:\n");
            for (FinancialNews n : latestNews) {
                context.append(String.format("  * %s: %s\n", n.getSource(), n.getTitle()));
            }
        }

        // --- 2. 生成阶段 (Generation) ---
        String finalContext = context.toString();
        
        String prompt = String.format("""
                %s
                
                【用户问题】
                %s
                """, finalContext, userMessage);

        log.info("[AiService] 构建 Prompt 长度: {} 字符", prompt.length());
        
        // 调用 DeepSeek
        return openAiClient.callChat(SYSTEM_PROMPT, prompt);
    }
}
