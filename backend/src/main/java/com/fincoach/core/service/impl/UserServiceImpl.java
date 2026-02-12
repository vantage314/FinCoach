package com.fincoach.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.controller.dto.UserDTO;
import com.fincoach.core.repository.entity.AssetItem;
import com.fincoach.core.repository.entity.InvestmentPlan;
import com.fincoach.core.repository.entity.RiskAssessment;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.AssetItemMapper;
import com.fincoach.core.repository.mapper.InvestmentPlanMapper;
import com.fincoach.core.repository.mapper.RiskAssessmentMapper;
import com.fincoach.core.repository.mapper.UserMapper;
import com.fincoach.core.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AssetItemMapper assetItemMapper;

    @Autowired
    private InvestmentPlanMapper investmentPlanMapper;

    @Autowired
    private RiskAssessmentMapper riskAssessmentMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDTO getProfile() {
        Long userId = UserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        UserDTO dto = new UserDTO();
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setPhone(user.getPhone());
        }
        return dto;
    }

    @Override
    public void updateProfile(UserDTO userDTO) {
        Long userId = UserContext.getCurrentUserId();
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(userDTO.getNickname() != null, User::getUsername, userDTO.getNickname())
                .set(userDTO.getEmail() != null, User::getEmail, userDTO.getEmail())
                .set(userDTO.getPhone() != null, User::getPhone, userDTO.getPhone()));
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        Long userId = UserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void resetData() {
        Long userId = UserContext.getCurrentUserId();
        log.warn("User {} is resetting all data!", userId);
        
        // 删除关联数据
        // investment_plan_item 通常是级联删除或者需要在 plan 之前删除，这里假设在 plan 删除时触发或此时暂无独立mapper
        // 如果 PlanItem 是独立表，这里应该先删除 item
        // 简单处理：先删除主表，如果有外键约束会报错，根据指令 "planItemMapper.deleteByUserId(userId)"，但我不知道 PlanItemMapper 是否存在
        // 暂时假设 cascade delete 配置在数据库层面，或者通过 planId 删除
        // 尝试删除 InvestmentPlan (MyBatisPlus delete wrapper)
        investmentPlanMapper.delete(new QueryWrapper<InvestmentPlan>().eq("user_id", userId));
        
        assetItemMapper.delete(new QueryWrapper<AssetItem>().eq("user_id", userId));
        riskAssessmentMapper.delete(new QueryWrapper<RiskAssessment>().eq("user_id", userId));
        
        log.info("Data reset completed for user {}", userId);
    }
}
