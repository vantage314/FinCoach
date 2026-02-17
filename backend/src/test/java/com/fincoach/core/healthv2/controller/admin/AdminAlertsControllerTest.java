package com.fincoach.core.healthv2.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminAlertAckRequest;
import com.fincoach.core.healthv2.entity.FcAlertRecordEntity;
import com.fincoach.core.healthv2.mapper.FcAlertRecordMapper;
import com.fincoach.core.healthv2.service.AuditService;
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

public class AdminAlertsControllerTest {

    @Test
    public void testListAlertsSortedBySeverity() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        FcAlertRecordMapper recordMapper = Mockito.mock(FcAlertRecordMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);

        FcAlertRecordEntity record1 = new FcAlertRecordEntity();
        record1.setId(1L);
        record1.setRuleKey("ALERT_DTI_HIGH");
        record1.setSeverity("WARN");
        record1.setMessage("DTI 高");
        record1.setStatus("OPEN");
        record1.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        record1.setPayloadJson("{\"reportId\":101}");

        FcAlertRecordEntity record2 = new FcAlertRecordEntity();
        record2.setId(2L);
        record2.setRuleKey("ALERT_RISK_HIGH");
        record2.setSeverity("CRITICAL");
        record2.setMessage("风险偏高");
        record2.setStatus("OPEN");
        record2.setCreatedAt(LocalDateTime.now());
        record2.setPayloadJson("{\"reportId\":102}");

        when(recordMapper.selectList(Mockito.any())).thenReturn(List.of(record1, record2));

        AdminAlertsController controller = new AdminAlertsController(recordMapper, objectMapper, auditService);
        AdminAlertsController proxy = wrapAdmin(controller);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/admin/api/alerts").param("limit", "200").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].code").value("ALERT_RISK_HIGH"))
                .andExpect(jsonPath("$.data.items[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.data.items[0].reportId").value(102))
                .andExpect(jsonPath("$.data.items[1].code").value("ALERT_DTI_HIGH"));
        UserContext.clear();
    }

    @Test
    public void testAckAlertsById() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        FcAlertRecordMapper recordMapper = Mockito.mock(FcAlertRecordMapper.class);
        AuditService auditService = Mockito.mock(AuditService.class);

        when(recordMapper.updateById(Mockito.any())).thenReturn(1);

        AdminAlertsController controller = new AdminAlertsController(recordMapper, objectMapper, auditService);
        AdminAlertsController proxy = wrapAdmin(controller);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        AdminAlertAckRequest request = new AdminAlertAckRequest();
        AdminAlertAckRequest.AlertRef ref = new AdminAlertAckRequest.AlertRef();
        ref.setAlertId(9L);
        request.setAlerts(List.of(ref));

        UserContext.setUserId(1L);
        mockMvc.perform(post("/admin/api/alerts/ack")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        UserContext.clear();

        ArgumentCaptor<FcAlertRecordEntity> captor = ArgumentCaptor.forClass(FcAlertRecordEntity.class);
        Mockito.verify(recordMapper).updateById(captor.capture());
        assertEquals("ACKED", captor.getValue().getStatus());
        Mockito.verify(auditService).log(Mockito.eq(1L), Mockito.eq("ACK_ALERTS"), Mockito.eq("ALERT_RECORD"),
                Mockito.isNull(), Mockito.isNull(), Mockito.any());
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
