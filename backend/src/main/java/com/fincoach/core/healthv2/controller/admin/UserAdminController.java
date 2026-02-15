package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin-Users", description = "用户管理")
public class UserAdminController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AuditService auditService;

    @GetMapping
    @Operation(summary = "用户列表（分页）")
    public Result<IPage<User>> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        IPage<User> result = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        // 脱敏：清除密码
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(result);
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "分配角色")
    public Result<String> assignRole(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null || (!role.equals("USER") && !role.equals("ADMIN") && !role.equals("OPS"))) {
            return Result.error(400, "角色必须为 USER/ADMIN/OPS");
        }
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error(404, "用户不存在");

        String oldRole = user.getRole();
        user.setRole(role);
        userMapper.updateById(user);

        auditService.log(UserContext.getCurrentUserId(), "ASSIGN_ROLE", "USER", userId, oldRole, role);
        log.info("[Admin] 角色变更: userId={}, {} -> {}, by={}", userId, oldRole, role, UserContext.getCurrentUserId());
        return Result.success("角色已更新为 " + role);
    }
}
