package com.fincoach.core.service;

import com.fincoach.core.controller.dto.UserDTO;

public interface UserService {
    void updateProfile(UserDTO userDTO);
    void changePassword(String oldPassword, String newPassword);
    void resetData();
}
