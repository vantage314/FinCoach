import request from '@/api/request';

// 重置用户所有数据
export const resetUserData = () => {
    return request.post('/user/reset-data');
};

// 获取用户信息
export const getUserProfile = () => {
    return request.get('/user/profile');
};

// 修改个人资料
export const updateUserProfile = (data: { nickname?: string; email?: string }) => {
    return request.post('/user/update', {
        nickname: data.nickname,
        email: data.email
    });
};

// 修改密码
export const changePassword = (data: { oldPassword: string; newPassword: string }) => {
    return request.post('/user/change-password', data);
};
