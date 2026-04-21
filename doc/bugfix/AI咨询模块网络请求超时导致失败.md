# AI 咨询模块网络请求超时导致失败

**日期**: 2026-03-05

## Bug 描述

用户在 AI 咨询页面发送复杂金融分析请求（如"分析宁德时代投资价值"）时，等待约 10 秒后页面显示"❌ 网络请求失败，请检查后端服务。"误导用户认为后端宕机。

## Bug 原因

1. **前端全局超时过短**：`src/api/request.ts` 中 axios 全局 `timeout: 10000`（10 秒），而 DeepSeek API 处理复杂请求需要 20-40 秒
2. **后端 RestTemplate 无超时**：`OpenAiClient` 使用默认 `new RestTemplate()`，无连接/读取超时限制，理论上可无限等待
3. **误导性错误信息**：前端 catch 块统一显示"网络请求失败"，没有区分超时和真正的网络故障

## 解决方案

### 前端 `src/api/ai.ts`
- 为 AI 请求单独配置 `timeout: 60000`（60 秒），不影响其他接口的 10 秒全局超时

### 后端 `src/main/java/com/fincoach/core/util/OpenAiClient.java`
- 使用 `SimpleClientHttpRequestFactory` 设置连接超时 10s + 读取超时 60s
- 保证后端不会因外部 API 无响应而线程阻塞

## 经验教训

1. 涉及外部 AI API 调用的接口，前后端超时配置需要特别考虑，不能使用普通接口的默认超时
2. 前端错误提示应区分 **超时** 和 **网络不可达**，给用户更准确的反馈
3. 长耗时接口（AI、数据导出等）应优先考虑流式传输（SSE/WebSocket），避免长时间同步等待
