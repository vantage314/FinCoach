import request from '@/utils/request';

// 发送对话请求
export const sendChatRequest = (data: { message: string }) => {
    return request.post('/ai/chat', data);
};
