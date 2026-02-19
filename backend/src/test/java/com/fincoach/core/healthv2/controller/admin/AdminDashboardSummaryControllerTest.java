package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;
import com.fincoach.core.healthv2.mapper.FcAlertRecordMapper;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import com.fincoach.core.healthv2.mapper.FcNotificationMapper;
import com.fincoach.core.repository.entity.FcJobStatusEntity;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.FcJobStatusMapper;
import com.fincoach.core.repository.mapper.UserMapper;
import com.fincoach.core.security.AdminChecker;
import com.fincoach.core.security.AdminOnlyAspect;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminDashboardSummaryControllerTest {

    @Test
    public void testAdminSummaryOk() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);
        when(userMapper.selectCount(Mockito.any())).thenReturn(12L);

        FcAlertRecordMapper alertMapper = Mockito.mock(FcAlertRecordMapper.class);
        when(alertMapper.selectCount(Mockito.any())).thenReturn(7L);

        FcJobStatusMapper jobStatusMapper = Mockito.mock(FcJobStatusMapper.class);
        FcJobStatusEntity job = new FcJobStatusEntity();
        job.setJobName("PY_MARKET_CRAWLER");
        job.setLastHeartbeatAt(LocalDateTime.of(2026, 2, 19, 10, 0, 0));
        when(jobStatusMapper.selectOne(Mockito.any())).thenReturn(job);

        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);
        FcHealthReportEntity report = new FcHealthReportEntity();
        report.setReportDate(LocalDateTime.of(2026, 2, 18, 9, 30, 0));
        when(reportMapper.selectOne(Mockito.any())).thenReturn(report);

        FcNotificationMapper notificationMapper = Mockito.mock(FcNotificationMapper.class);
        when(notificationMapper.selectCount(Mockito.any())).thenReturn(4L);

        AdminDashboardSummaryController controller = new AdminDashboardSummaryController(
                userMapper, alertMapper, jobStatusMapper, reportMapper, notificationMapper);
        AdminDashboardSummaryController proxy = wrapAdmin(controller, userMapper);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/admin/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.usersTotal").value(12))
                .andExpect(jsonPath("$.data.alertsOpen").value(7))
                .andExpect(jsonPath("$.data.latestCrawlerHeartbeatAt").exists())
                .andExpect(jsonPath("$.data.latestHealthReportAt").exists())
                .andExpect(jsonPath("$.data.notificationsUnreadTotal").value(4))
                .andExpect(jsonPath("$.data.systemStatus").value("OK"))
                .andExpect(jsonPath("$.data.warnings").isArray());
        UserContext.clear();
    }

    @Test
    public void testNonAdminForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User user = new User();
        user.setId(2L);
        user.setRole("USER");
        when(userMapper.selectById(2L)).thenReturn(user);

        FcAlertRecordMapper alertMapper = Mockito.mock(FcAlertRecordMapper.class);
        FcJobStatusMapper jobStatusMapper = Mockito.mock(FcJobStatusMapper.class);
        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);
        FcNotificationMapper notificationMapper = Mockito.mock(FcNotificationMapper.class);

        AdminDashboardSummaryController controller = new AdminDashboardSummaryController(
                userMapper, alertMapper, jobStatusMapper, reportMapper, notificationMapper);
        AdminDashboardSummaryController proxy = wrapAdmin(controller, userMapper);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(2L);
        mockMvc.perform(get("/admin/api/dashboard/summary"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
    }

    private AdminDashboardSummaryController wrapAdmin(AdminDashboardSummaryController controller, UserMapper userMapper) {
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        return factory.getProxy();
    }
}
