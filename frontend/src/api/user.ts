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
    // Note: Backend expects 'nickname', Frontend UI form uses 'username' but stores in 'nickname'?
    // User requested: `username?: string; email?: string`
    // Backend UserDTO: `nickname`, `email`, `username`? 
    // Wait, UserDTO has `nickname`. User entity has `username`.
    // My UserServiceImpl.updateProfile maps: 
    // .set(userDTO.getNickname() != null, User::getUsername, userDTO.getNickname())
    // So backend expects `nickname` in JSON to update `username` in DB.
    // I will map frontend `username` to backend `nickname` if needed, or just use `nickname`.
    // User instruction: `export const updateUserProfile = (data: { username?: string; email?: string })`
    // I will adhere to the instruction but ensure the PAYLOAD sent matches UserDTO.
    // Actually, let's look at UserDTO again: `private String nickname;`
    // And Service: `set(userDTO.getNickname() != null, User::getUsername, userDTO.getNickname())`
    // This implies `nickname` in DTO updates `username` in DB.
    // So I should send `nickname`.
    // But user asks for `username`. I will adapt.
    return request.post('/user/update', {
        nickname: data.username, // Auto-map username to nickname for backend compatibility
        email: data.email
    });
};

// 修改密码
export const changePassword = (data: { oldPassword: string; newPassword: string }) => {
    return request.post('/user/change-password', data);
};
