package com.fincoach.core.healthv2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcNotificationEntity;
import com.fincoach.core.healthv2.service.NotificationService;
import com.fincoach.core.interceptor.AppUserRoleInterceptor;
import com.fincoach.core.interceptor.JwtInterceptor;
import com.fincoach.core.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NotificationControllerTest {

    @Test
    public void testMarkReadAlias() throws Exception {
        NotificationService notificationService = Mockito.mock(NotificationService.class);
        NotificationController controller = new NotificationController();
        ReflectionTestUtils.setField(controller, "notificationService", notificationService);

        FcNotificationEntity entity = new FcNotificationEntity();
        entity.setId(1L);
        entity.setUserId(1L);
        when(notificationService.getById(1L)).thenReturn(entity);
        when(notificationService.markRead(1L, 1L)).thenReturn(true);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(post("/api/app/notifications/read")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(new NotificationController.ReadRequest() {{
                            setId(1L);
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        UserContext.clear();
    }

    @Test
    public void testMarkAllReadAlias() throws Exception {
        NotificationService notificationService = Mockito.mock(NotificationService.class);
        NotificationController controller = new NotificationController();
        ReflectionTestUtils.setField(controller, "notificationService", notificationService);
        when(notificationService.markAllRead(anyLong())).thenReturn(1);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(post("/api/app/notifications/readAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        UserContext.clear();
    }

    @Test
    public void testAdminTokenForbiddenForAppNotifications() throws Exception {
        NotificationService notificationService = Mockito.mock(NotificationService.class);
        NotificationController controller = new NotificationController();
        ReflectionTestUtils.setField(controller, "notificationService", notificationService);

        JwtUtils jwtUtils = new JwtUtils();
        JwtInterceptor jwtInterceptor = buildJwtInterceptor(jwtUtils);
        AppUserRoleInterceptor appUserRoleInterceptor = buildAppUserRoleInterceptor(jwtUtils);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(jwtInterceptor, appUserRoleInterceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        String token = jwtUtils.generateToken(10L, List.of("ADMIN"));
        mockMvc.perform(get("/api/app/notifications/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    public void testUserTokenCanListNotifications() throws Exception {
        NotificationService notificationService = Mockito.mock(NotificationService.class);
        NotificationController controller = new NotificationController();
        ReflectionTestUtils.setField(controller, "notificationService", notificationService);

        Page<FcNotificationEntity> pageData = new Page<>(1, 20);
        pageData.setTotal(0);
        pageData.setRecords(List.of());
        when(notificationService.list(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(pageData);
        when(notificationService.countUnread(Mockito.anyLong())).thenReturn(0L);

        JwtUtils jwtUtils = new JwtUtils();
        JwtInterceptor jwtInterceptor = buildJwtInterceptor(jwtUtils);
        AppUserRoleInterceptor appUserRoleInterceptor = buildAppUserRoleInterceptor(jwtUtils);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(jwtInterceptor, appUserRoleInterceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        String token = jwtUtils.generateToken(11L, List.of("USER"));
        mockMvc.perform(get("/api/app/notifications/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    private JwtInterceptor buildJwtInterceptor(JwtUtils jwtUtils) {
        JwtInterceptor interceptor = new JwtInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(interceptor, "objectMapper", new ObjectMapper());
        return interceptor;
    }

    private AppUserRoleInterceptor buildAppUserRoleInterceptor(JwtUtils jwtUtils) {
        AppUserRoleInterceptor interceptor = new AppUserRoleInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(interceptor, "objectMapper", new ObjectMapper());
        return interceptor;
    }
}
