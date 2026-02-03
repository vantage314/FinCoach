package com.fincoach.core.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.controller.dto.AuthDTO;
import com.fincoach.core.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "身份认证", description = "负责用户注册、登录及 Token 验证相关接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新账户，密码采用 BCrypt 加密")
    public Result<String> register(@RequestBody AuthDTO authDTO) {
        System.out.println(">>> 收到注册请求: " + authDTO);
        log.info("开始处理用户注册请求: {}", authDTO.getUsername());
        authService.register(authDTO);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "验证凭据并返回 JWT Token")
    public Result<String> login(@RequestBody AuthDTO authDTO) {
        String token = authService.login(authDTO);
        return Result.success(token);
    }

    @org.springframework.web.bind.annotation.GetMapping("/test")
    public Result<String> test() {
        return Result.success("Backend is running (v2 with logs)");
    }
}
