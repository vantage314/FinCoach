package com.fincoach.core.healthv2.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminScoreRulePublishRequest;
import com.fincoach.core.healthv2.dto.admin.AdminScoreRuleSaveParamsRequest;
import com.fincoach.core.healthv2.entity.FcScoreRuleSetEntity;
import com.fincoach.core.healthv2.rules.ScoreRuleDefaults;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;
import com.fincoach.core.healthv2.service.ScoreRuleSetService;
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
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminScoreRuleControllerTest {

    @Test
    public void testAdminAccessOk() throws Exception {
        ScoreRuleSetService service = Mockito.mock(ScoreRuleSetService.class);
        FcScoreRuleSetEntity active = new FcScoreRuleSetEntity();
        active.setId(1L);
        active.setCode(ScoreRuleDefaults.DEFAULT_CODE);
        active.setVersion(1);
        when(service.getActiveRuleSet()).thenReturn(active);
        when(service.getActiveSnapshot()).thenReturn(new ScoreRuleSnapshot(null,
                ScoreRuleDefaults.DEFAULT_CODE,
                1,
                ScoreRuleSnapshot.SOURCE_FALLBACK_DEFAULT,
                null,
                List.of(),
                List.of()));
        when(service.listParams(1L)).thenReturn(List.of());
        when(service.draftNewVersion(any())).thenReturn(active);
        doNothing().when(service).upsertParams(eq(1L), any());
        doNothing().when(service).publish(eq(1L));
        doNothing().when(service).reload();

        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        ScoreRuleController controller = new ScoreRuleController(service);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(eq(1L), any())).thenReturn(true);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        factory.addAspect(permissionAspect);
        ScoreRuleController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/api/admin/score-rules/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        AdminScoreRuleSaveParamsRequest saveReq = new AdminScoreRuleSaveParamsRequest();
        saveReq.setRuleSetId(1L);
        ObjectMapper mapper = new ObjectMapper();
        mockMvc.perform(post("/api/admin/score-rules/save-params")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(saveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        AdminScoreRulePublishRequest publishReq = new AdminScoreRulePublishRequest();
        publishReq.setRuleSetId(1L);
        mockMvc.perform(post("/api/admin/score-rules/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(publishReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/admin/score-rules/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        UserContext.clear();
    }

    @Test
    public void testAdminWithoutPermissionForbidden() throws Exception {
        ScoreRuleSetService service = Mockito.mock(ScoreRuleSetService.class);
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(2L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(2L)).thenReturn(admin);

        ScoreRuleController controller = new ScoreRuleController(service);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(2L, RbacPermissionCodes.ADMIN_SCORE_RULE_VIEW)).thenReturn(false);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        factory.addAspect(permissionAspect);
        ScoreRuleController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(2L);
        mockMvc.perform(get("/api/admin/score-rules/active"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
    }
}
