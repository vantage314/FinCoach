package com.fincoach.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.controller.dto.AuthDTO;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import com.fincoach.core.service.AuthService;
import com.fincoach.core.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void register(AuthDTO authDTO) {
        log.info("开始注册用户: {}", authDTO.getUsername());
        
        // 检查用户名是否存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, authDTO.getUsername()));
        if (count > 0) {
            log.warn("注册失败，用户名已存在: {}", authDTO.getUsername());
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(authDTO.getUsername());
        String encodedPassword = passwordEncoder.encode(authDTO.getPassword());
        user.setPassword(encodedPassword);
        
        log.info("加密后的密码: {}", encodedPassword);
        
        // 手动设置时间，确保即使数据库默认值失效也能写入
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        log.info("准备执行数据库插入操作，用户信息: {}", user);
        int rows = userMapper.insert(user);
        boolean saved = rows > 0;
        log.info("MyBatis Plus 返回的保存结果: {}, 影响行数: {}, 用户ID: {}", saved, rows, user.getId());
        
        if (!saved) {
            log.error("数据库写入失败，但未抛出异常！");
            throw new RuntimeException("数据库写入失败");
        }
    }

    @Override
    public String login(AuthDTO authDTO) {
        log.info("用户尝试登录: {}", authDTO.getUsername());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, authDTO.getUsername()));
        
        if (user == null || !passwordEncoder.matches(authDTO.getPassword(), user.getPassword())) {
            log.warn("登录失败，用户名或密码错误: {}", authDTO.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        log.info("登录成功: {}", authDTO.getUsername());
        return jwtUtils.generateToken(user.getId());
    }
}
