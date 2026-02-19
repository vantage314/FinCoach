package com.fincoach.core.healthv2.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcAlertEntity;
import com.fincoach.core.healthv2.mapper.FcAlertRecordMapper;
import com.fincoach.core.healthv2.service.AlertService;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import com.fincoach.core.security.AdminChecker;
import com.fincoach.core.security.AdminOnlyAspect;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminAlertsCenterControllerTest {

    @Test
    public void testListCenterAlerts() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        FcAlertRecordMapper recordMapper = Mockito.mock(FcAlertRecordMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);
        AlertService alertService = Mockito.mock(AlertService.class);

        FcAlertEntity a = new FcAlertEntity();
        a.setId(1L);
        a.setAlertType("DTI_DANGER");
        a.setSeverity("DANGER");
        a.setTitle("负债压力过高");
        a.setStatus("OPEN");
        a.setLastSeenAt(LocalDateTime.now().minusMinutes(1));

        FcAlertEntity b = new FcAlertEntity();
        b.setId(2L);
        b.setAlertType("CASHFLOW_NEGATIVE");
        b.setSeverity("WARN");
        b.setTitle("现金流为负");
        b.setStatus("OPEN");
        b.setLastSeenAt(LocalDateTime.now());

        when(alertService.listAdminAlerts(any(), any(), any(), any())).thenReturn(List.of(a, b));

        AdminAlertsController controller = new AdminAlertsController(recordMapper, objectMapper, auditService);
        ReflectionTestUtils.setField(controller, "alertService", alertService);
        AdminAlertsController proxy = wrapAdmin(controller);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/admin/api/alerts/list").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].code").value("DTI_DANGER"));
        UserContext.clear();
    }

    @Test
    public void testResolveAlerts() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        FcAlertRecordMapper recordMapper = Mockito.mock(FcAlertRecordMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);
        AlertService alertService = Mockito.mock(AlertService.class);
        when(alertService.resolveAlert(9L)).thenReturn(true);

        AdminAlertsController controller = new AdminAlertsController(recordMapper, objectMapper, auditService);
        ReflectionTestUtils.setField(controller, "alertService", alertService);
        AdminAlertsController proxy = wrapAdmin(controller);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(post("/admin/api/alerts/resolve")
                        .contentType("application/json")
                        .content("{\"alerts\":[{\"alertId\":9}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        UserContext.clear();
    }

    private AdminAlertsController wrapAdmin(AdminAlertsController controller) {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        return factory.getProxy();
    }
}
