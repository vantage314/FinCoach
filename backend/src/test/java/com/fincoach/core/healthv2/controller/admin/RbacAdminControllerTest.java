package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.rbac.RbacPermissionCodes;
import com.fincoach.core.rbac.dto.RbacMeDTO;
import com.fincoach.core.rbac.service.RbacAdminService;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import com.fincoach.core.security.AdminChecker;
import com.fincoach.core.security.AdminOnlyAspect;
import com.fincoach.core.security.PermissionAspect;
import com.fincoach.core.security.PermissionChecker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RbacAdminControllerTest {

    @Test
    public void testMeWithPermissionOk() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        RbacAdminService rbacAdminService = Mockito.mock(RbacAdminService.class);
        RbacMeDTO meDTO = new RbacMeDTO();
        meDTO.setUserId(1L);
        meDTO.setRoles(List.of("ADMIN"));
        meDTO.setPermissions(List.of(RbacPermissionCodes.ADMIN_MENU_VIEW));
        meDTO.setMenuTree(new ArrayList<>());
        when(rbacAdminService.buildMe(1L)).thenReturn(meDTO);

        RbacAdminController controller = new RbacAdminController(rbacAdminService);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(eq(1L), eq(RbacPermissionCodes.ADMIN_MENU_VIEW))).thenReturn(true);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        factory.addAspect(permissionAspect);
        RbacAdminController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/api/admin/rbac/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.menuTree").isArray());
        UserContext.clear();
    }

    @Test
    public void testAdminWithoutPermissionForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(2L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(2L)).thenReturn(admin);

        RbacAdminService rbacAdminService = Mockito.mock(RbacAdminService.class);
        RbacAdminController controller = new RbacAdminController(rbacAdminService);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(eq(2L), eq(RbacPermissionCodes.ADMIN_MENU_VIEW))).thenReturn(false);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        factory.addAspect(permissionAspect);
        RbacAdminController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(2L);
        mockMvc.perform(get("/api/admin/rbac/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
    }

    @Test
    public void testNoUserContextForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        RbacAdminService rbacAdminService = Mockito.mock(RbacAdminService.class);
        RbacAdminController controller = new RbacAdminController(rbacAdminService);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        factory.addAspect(permissionAspect);
        RbacAdminController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/api/admin/rbac/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
    }
}
