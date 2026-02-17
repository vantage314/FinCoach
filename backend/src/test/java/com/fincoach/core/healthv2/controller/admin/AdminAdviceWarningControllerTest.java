package com.fincoach.core.healthv2.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcHealthReportEntity;
import com.fincoach.core.healthv2.mapper.FcHealthReportMapper;
import com.fincoach.core.repository.entity.User;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminAdviceWarningControllerTest {

    @Test
    public void testStatsAggregatesWarnings() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        FcHealthReportMapper reportMapper = Mockito.mock(FcHealthReportMapper.class);

        FcHealthReportEntity report1 = buildReport(101L, objectMapper, List.of("MISSING_CASHFLOW", "MISSING_ASSETS"));
        FcHealthReportEntity report2 = buildReport(102L, objectMapper, List.of("MISSING_CASHFLOW"));
        FcHealthReportEntity report3 = buildReport(103L, objectMapper, List.of());
        when(reportMapper.selectList(Mockito.any())).thenReturn(List.of(report1, report2, report3));

        AdminAdviceWarningController controller = new AdminAdviceWarningController(reportMapper, objectMapper);

        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminOnlyAspect = new AdminOnlyAspect(adminChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminOnlyAspect);
        AdminAdviceWarningController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/admin/api/advice/warnings/stats").param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalReports").value(3))
                .andExpect(jsonPath("$.data.byCode[0].code").value("MISSING_CASHFLOW"))
                .andExpect(jsonPath("$.data.byCode[0].count").value(2))
                .andExpect(jsonPath("$.data.byCode[0].ratio").value(0.6667))
                .andExpect(jsonPath("$.data.byCode[1].code").value("MISSING_ASSETS"))
                .andExpect(jsonPath("$.data.byCode[1].count").value(1))
                .andExpect(jsonPath("$.data.byCode[1].ratio").value(0.3333))
                .andExpect(jsonPath("$.data.latestSamples[0].reportId").value(101))
                .andExpect(jsonPath("$.data.latestSamples[0].codes").isArray());
        UserContext.clear();
    }

    private FcHealthReportEntity buildReport(Long id, ObjectMapper mapper, List<String> warnings) throws Exception {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("warnings", warnings);
        Map<String, Object> adviceV2 = new LinkedHashMap<>();
        adviceV2.put("meta", meta);
        Map<String, Object> adviceRoot = new LinkedHashMap<>();
        adviceRoot.put("adviceV2", adviceV2);

        FcHealthReportEntity entity = new FcHealthReportEntity();
        entity.setId(id);
        entity.setReportDate(LocalDateTime.now());
        entity.setAdviceJson(mapper.writeValueAsString(adviceRoot));
        return entity;
    }
}
