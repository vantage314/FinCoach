package com.fincoach.core.healthv2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcNotificationEntity;
import com.fincoach.core.healthv2.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
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
}
