package com.fincoach.core.healthv2.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminUserToggleRequest;
import com.fincoach.core.rbac.service.RbacQueryService;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import com.fincoach.core.security.AdminChecker;
import com.fincoach.core.security.AdminOnlyAspect;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminUserManagementControllerTest {

    @Test
    public void testAdminListOk() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        User user1 = new User();
        user1.setId(10L);
        user1.setUsername("alice");
        user1.setEmail("alice@example.com");
        user1.setCreateTime(LocalDateTime.of(2026, 2, 1, 12, 0, 0));
        user1.setLastLoginAt(LocalDateTime.of(2026, 2, 18, 8, 0, 0));
        user1.setEnabled(1);

        User user2 = new User();
        user2.setId(11L);
        user2.setUsername("bob");
        user2.setEmail("bob@example.com");
        user2.setCreateTime(LocalDateTime.of(2026, 2, 2, 9, 30, 0));
        user2.setEnabled(0);

        Page<User> page = new Page<>(1, 20);
        page.setTotal(2);
        page.setRecords(List.of(user1, user2));
        when(userMapper.selectPage(Mockito.any(), Mockito.any())).thenReturn(page);

        RbacQueryService rbacQueryService = Mockito.mock(RbacQueryService.class);
        when(rbacQueryService.getUserRoleCodes(10L)).thenReturn(List.of("ADMIN"));
        when(rbacQueryService.getUserRoleCodes(11L)).thenReturn(List.of("USER"));

        AdminUserManagementController controller = new AdminUserManagementController(userMapper, rbacQueryService);
        AdminUserManagementController proxy = wrapAdmin(controller, userMapper);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/admin/api/users/list").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(10))
                .andExpect(jsonPath("$.data.items[0].roles").isArray())
                .andExpect(jsonPath("$.data.items[0].enabled").value(true));
        UserContext.clear();
    }

    @Test
    public void testToggleUpdatesEnabled() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        User target = new User();
        target.setId(9L);
        target.setEnabled(1);
        when(userMapper.selectById(9L)).thenReturn(target);
        when(userMapper.updateById(Mockito.any())).thenReturn(1);

        RbacQueryService rbacQueryService = Mockito.mock(RbacQueryService.class);

        AdminUserManagementController controller = new AdminUserManagementController(userMapper, rbacQueryService);
        AdminUserManagementController proxy = wrapAdmin(controller, userMapper);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        AdminUserToggleRequest request = new AdminUserToggleRequest();
        request.setUserId(9L);
        request.setEnabled(false);

        UserContext.setUserId(1L);
        mockMvc.perform(post("/admin/api/users/toggle")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        UserContext.clear();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getEnabled());
    }

    @Test
    public void testNonAdminForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User user = new User();
        user.setId(2L);
        user.setRole("USER");
        when(userMapper.selectById(2L)).thenReturn(user);

        RbacQueryService rbacQueryService = Mockito.mock(RbacQueryService.class);
        AdminUserManagementController controller = new AdminUserManagementController(userMapper, rbacQueryService);
        AdminUserManagementController proxy = wrapAdmin(controller, userMapper);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(2L);
        mockMvc.perform(get("/admin/api/users/list"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
    }

    private AdminUserManagementController wrapAdmin(AdminUserManagementController controller, UserMapper userMapper) {
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        return factory.getProxy();
    }
}
