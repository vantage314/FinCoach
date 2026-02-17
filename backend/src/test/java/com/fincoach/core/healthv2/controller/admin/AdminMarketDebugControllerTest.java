package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.debug.AdminMarketDebugMapper;
import com.fincoach.core.healthv2.debug.PortfolioDebugContextHolder;
import com.fincoach.core.healthv2.debug.PortfolioMarketDebugSnapshot;
import com.fincoach.core.rbac.RbacPermissionCodes;
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

import java.time.Instant;
import java.util.ArrayList;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminMarketDebugControllerTest {

    @Test
    public void testAdminAccessOk() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        AdminMarketDebugController controller = new AdminMarketDebugController(new AdminMarketDebugMapper());
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect aspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(1L, RbacPermissionCodes.ADMIN_MARKET_DEBUG_VIEW)).thenReturn(true);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(aspect);
        factory.addAspect(permissionAspect);
        AdminMarketDebugController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        PortfolioMarketDebugSnapshot snapshot = new PortfolioMarketDebugSnapshot();
        snapshot.setHistorySource("MARKET_DATA_DAILY_CLOSE");
        snapshot.setGeneratedAt(Instant.now());
        snapshot.setWarnings(new ArrayList<>());
        PortfolioDebugContextHolder.enableCapture();
        PortfolioDebugContextHolder.set(snapshot);

        UserContext.setUserId(1L);
        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.history.source").value("MARKET_DATA_DAILY_CLOSE"))
                .andExpect(jsonPath("$.data.warnings").isArray())
                .andExpect(jsonPath("$.data.requestId").exists())
                .andExpect(jsonPath("$.data.timestamp").exists());
        UserContext.clear();
        PortfolioDebugContextHolder.clear();
    }

    @Test
    public void testNonAdminForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User user = new User();
        user.setId(2L);
        user.setRole("USER");
        when(userMapper.selectById(2L)).thenReturn(user);

        AdminMarketDebugController controller = new AdminMarketDebugController(new AdminMarketDebugMapper());
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect aspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(aspect);
        factory.addAspect(permissionAspect);
        AdminMarketDebugController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(2L);
        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
        PortfolioDebugContextHolder.clear();
    }

    @Test
    public void testNoUserContextForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);

        AdminMarketDebugController controller = new AdminMarketDebugController(new AdminMarketDebugMapper());
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect aspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(aspect);
        factory.addAspect(permissionAspect);
        AdminMarketDebugController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        PortfolioDebugContextHolder.clear();
    }

    @Test
    public void testEmptySnapshotReturnsStableSchema() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        AdminMarketDebugController controller = new AdminMarketDebugController(new AdminMarketDebugMapper());
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect aspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(1L, RbacPermissionCodes.ADMIN_MARKET_DEBUG_VIEW)).thenReturn(true);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(aspect);
        factory.addAspect(permissionAspect);
        AdminMarketDebugController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warnings").isArray())
                .andExpect(jsonPath("$.data.warnings[0]").value("DEBUG_SNAPSHOT_EMPTY"))
                .andExpect(jsonPath("$.data.resolvedTickers").isArray());
        UserContext.clear();
    }

    @Test
    public void testAdminWithoutPermissionForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(3L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(3L)).thenReturn(admin);

        AdminMarketDebugController controller = new AdminMarketDebugController(new AdminMarketDebugMapper());
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(3L, RbacPermissionCodes.ADMIN_MARKET_DEBUG_VIEW)).thenReturn(false);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        factory.addAspect(permissionAspect);
        AdminMarketDebugController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(3L);
        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
    }
}
