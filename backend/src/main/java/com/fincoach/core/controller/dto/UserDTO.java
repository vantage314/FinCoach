package com.fincoach.core.controller.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String nickname;
    private String email;
    private String phone;
    
    // For password change
    private String oldPassword;
    private String newPassword;
}
