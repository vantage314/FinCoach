package com.fincoach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.controller.dto.LoginRequest;
import com.fincoach.controller.dto.RegisterRequest;
import com.fincoach.controller.vo.LoginVO;
import com.fincoach.controller.vo.UserInfoVO;
import com.fincoach.entity.SysUser;
import com.fincoach.mapper.SysUserMapper;
import com.fincoach.core.common.UserContext;
import com.fincoach.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务
 * 处理登录、注册等业务逻辑
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     * @param req 登录请求
     * @return 登录响应（含 Token）
     */
    public LoginVO login(LoginRequest req) {
        log.info("[UserService] 用户登录: {}", req.getUsername());

        // 1. 根据用户名查库
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername())
        );

        if (user == null) {
            log.warn("[UserService] 用户不存在: {}", req.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 校验密码
        // 支持明文比对（演示）和 BCrypt 加密比对
        boolean passwordMatch = false;
        if (req.getPassword().equals(user.getPassword())) {
            // 明文比对（演示阶段）
            passwordMatch = true;
        } else if (passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // BCrypt 加密比对
            passwordMatch = true;
        }

        if (!passwordMatch) {
            log.warn("[UserService] 密码错误: {}", req.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("[UserService] 用户已禁用: {}", req.getUsername());
            throw new RuntimeException("账号已被禁用");
        }

        // 4. 生成 Token
        String token = jwtUtil.createToken(user.getId(), user.getUsername());

        log.info("[UserService] 登录成功: {}", req.getUsername());

        // 5. 返回 LoginVO
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname() != null ? user.getNickname() : user.getUsername())
                .avatar(user.getAvatar())
                .build();
    }

    /**
     * 用户注册
     * @param req 注册请求
     */
    @Transactional
    public void register(RegisterRequest req) {
        log.info("[UserService] 用户注册: {}", req.getUsername());

        // 1. 校验用户名唯一性
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername())
        );

        if (count > 0) {
            log.warn("[UserService] 用户名已存在: {}", req.getUsername());
            throw new RuntimeException("用户名已存在");
        }

        // 2. 创建用户
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        // 🔥 使用 BCrypt 加密密码
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setEmail(req.getEmail());
        user.setStatus(1); // 正常状态
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        sysUserMapper.insert(user);

        log.info("[UserService] 注册成功: {}, userId={}", req.getUsername(), user.getId());
    }

    /**
     * 获取当前用户信息
     * @return 用户信息 VO
     */
    public UserInfoVO getCurrentUserInfo() {
        Long userId = UserContext.getCurrentUserId();
        log.debug("[UserService] 获取用户信息: userId={}", userId);

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    /**
     * 根据ID获取用户
     */
    public SysUser getById(Long userId) {
        return sysUserMapper.selectById(userId);
    }
}
