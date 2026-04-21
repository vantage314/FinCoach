import request from '@/api/request';

/**
 * 发送 AI 对话请求
 * 注意：AI 接口调用 DeepSeek API，复杂请求可能需要 20-40 秒
 * 因此单独设置 60 秒超时，避免全局 10 秒超时导致请求失败
 */
export const sendChatRequest = (data: { message: string }) => {
    return request.post('/ai/chat', data, {
        timeout: 60000, // 60 秒超时，覆盖全局 10 秒
    });
};
