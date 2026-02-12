package com.fincoach.core.controller;

import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.controller.dto.UserDTO;
import com.fincoach.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "个人中心及系统设置接口")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/update")
    @Operation(summary = "更新资料", description = "修改昵称、邮箱等信息")
    public Result<Void> updateProfile(@RequestBody UserDTO userDTO) {
        userService.updateProfile(userDTO);
        return Result.success(null, "资料更新成功");
    }

    @GetMapping("/profile")
    @Operation(summary = "获取个人资料", description = "返回用户名、昵称与邮箱")
    public Result<UserDTO> getProfile() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(userService.getProfile());
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "验证旧密码并更新新密码")
    public Result<Void> changePassword(@RequestBody UserDTO userDTO) {
        userService.changePassword(userDTO.getOldPassword(), userDTO.getNewPassword());
        return Result.success(null, "密码修改成功");
    }

    @PostMapping("/reset-data")
    @Operation(summary = "重置演示数据", description = "危险操作：清空所有资产、计划和测评记录")
    public Result<Void> resetData() {
        userService.resetData();
        return Result.success(null, "数据已重置，请重新登录");
    }
}
