package com.fincoach.controller;

import com.fincoach.controller.dto.LoginRequest;
import com.fincoach.controller.dto.RegisterRequest;
import com.fincoach.controller.vo.LoginVO;
import com.fincoach.controller.vo.UserInfoVO;
import com.fincoach.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录、注册、获取用户信息等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        log.info("[AuthController] 登录请求: {}", req.getUsername());
        
        try {
            LoginVO loginVO = userService.login(req);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "登录成功");
            result.put("data", loginVO);
            return result;
        } catch (Exception e) {
            log.error("[AuthController] 登录失败: {}", e.getMessage());
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req) {
        log.info("[AuthController] 注册请求: {}", req.getUsername());
        
        try {
            userService.register(req);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "注册成功");
            return result;
        } catch (Exception e) {
            log.error("[AuthController] 注册失败: {}", e.getMessage());
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 获取当前用户信息
     * GET /api/auth/info
     * 🔥 需要登录（从 UserContext 获取用户ID）
     */
    @GetMapping("/info")
    public Map<String, Object> getUserInfo() {
        log.debug("[AuthController] 获取用户信息");
        
        try {
            UserInfoVO userInfo = userService.getCurrentUserInfo();
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", userInfo);
            return result;
        } catch (Exception e) {
            log.error("[AuthController] 获取用户信息失败: {}", e.getMessage());
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 退出登录
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Map<String, Object> logout() {
        log.info("[AuthController] 退出登录");
        
        // 🔥 客户端清除 Token 即可，服务端无状态
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "退出成功");
        return result;
    }
}
