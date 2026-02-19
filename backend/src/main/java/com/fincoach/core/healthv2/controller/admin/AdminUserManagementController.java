package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminUserDTO;
import com.fincoach.core.healthv2.dto.admin.AdminUserListDTO;
import com.fincoach.core.healthv2.dto.admin.AdminUserToggleRequest;
import com.fincoach.core.rbac.service.RbacQueryService;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import com.fincoach.core.security.AdminOnly;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/api/users")
@Tag(name = "Admin-Users", description = "Admin user management")
@AdminOnly
public class AdminUserManagementController {

    private final UserMapper userMapper;
    private final RbacQueryService rbacQueryService;

    public AdminUserManagementController(UserMapper userMapper, RbacQueryService rbacQueryService) {
        this.userMapper = userMapper;
        this.rbacQueryService = rbacQueryService;
    }

    @GetMapping("/list")
    public Result<AdminUserListDTO> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) String q) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime);
        if (q != null && !q.isBlank()) {
            qw.and(wrapper -> wrapper.like(User::getUsername, q).or().like(User::getEmail, q));
        }

        IPage<User> result = userMapper.selectPage(new Page<>(page, size), qw);
        List<AdminUserDTO> items = new ArrayList<>();
        if (result.getRecords() != null) {
            for (User user : result.getRecords()) {
                AdminUserDTO dto = new AdminUserDTO();
                dto.setId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setEmail(user.getEmail());
                dto.setCreatedAt(user.getCreateTime());
                dto.setLastLoginAt(user.getLastLoginAt());
                dto.setEnabled(toEnabled(user.getEnabled()));
                List<String> roles = rbacQueryService.getUserRoleCodes(user.getId());
                dto.setRoles(roles != null ? roles : new ArrayList<>());
                items.add(dto);
            }
        }

        AdminUserListDTO dto = new AdminUserListDTO();
        dto.setItems(items);
        dto.setTotal(result.getTotal());
        dto.setPage((int) result.getCurrent());
        dto.setSize((int) result.getSize());

        log.info("event=ADMIN_USER_LIST userId={} keyword={} total={}",
                UserContext.getCurrentUserId(), q, result.getTotal());
        return Result.success(dto);
    }

    @PostMapping("/toggle")
    public Result<String> toggle(@RequestBody AdminUserToggleRequest request) {
        if (request == null || request.getUserId() == null || request.getEnabled() == null) {
            return Result.error(400, "empty request");
        }
        User target = userMapper.selectById(request.getUserId());
        if (target == null) {
            return Result.error(404, "user not found");
        }
        target.setEnabled(request.getEnabled() ? 1 : 0);
        userMapper.updateById(target);
        log.info("event=ADMIN_USER_TOGGLE userId={} targetId={} enabled={}",
                UserContext.getCurrentUserId(), request.getUserId(), request.getEnabled());
        return Result.success(request.getEnabled() ? "enabled" : "disabled");
    }

    private Boolean toEnabled(Integer enabled) {
        if (enabled == null) {
            return null;
        }
        return enabled != 0;
    }
}
