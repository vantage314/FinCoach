package com.fincoach.core.service;

import com.fincoach.core.controller.dto.AuthDTO;

public interface AuthService {
    void register(AuthDTO authDTO);
    String login(AuthDTO authDTO);
}
