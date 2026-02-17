package com.fincoach.core.healthv2.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.dto.admin.AdminRebalanceTemplatePublishRequest;
import com.fincoach.core.healthv2.dto.admin.AdminRebalanceTemplateSaveRequest;
import com.fincoach.core.healthv2.entity.FcRebalanceTemplateEntity;
import com.fincoach.core.healthv2.service.RebalanceTemplateService;
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

public class AdminRebalanceTemplateControllerTest {

    @Test
    public void testAdminAccessOk() throws Exception {
        RebalanceTemplateService service = Mockito.mock(RebalanceTemplateService.class);
        FcRebalanceTemplateEntity template = new FcRebalanceTemplateEntity();
        template.setId(1L);
        template.setCode("BALANCED");
        template.setVersion(1);
        when(service.list()).thenReturn(List.of(template));
        when(service.save(any())).thenReturn(1L);
        doNothing().when(service).publish(eq(1L));
        doNothing().when(service).reload();

        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        AdminRebalanceTemplateController controller = new AdminRebalanceTemplateController(service);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(eq(1L), any())).thenReturn(true);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        factory.addAspect(permissionAspect);
        AdminRebalanceTemplateController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/api/admin/rebalance-templates/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        AdminRebalanceTemplateSaveRequest saveReq = new AdminRebalanceTemplateSaveRequest();
        saveReq.setCode("BALANCED");
        saveReq.setTemplateJson("{\"CASH\":0.2}");
        ObjectMapper mapper = new ObjectMapper();

        mockMvc.perform(post("/api/admin/rebalance-templates/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(saveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        AdminRebalanceTemplatePublishRequest publishReq = new AdminRebalanceTemplatePublishRequest();
        publishReq.setTemplateId(1L);
        mockMvc.perform(post("/api/admin/rebalance-templates/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(publishReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/admin/rebalance-templates/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        UserContext.clear();
    }

    @Test
    public void testAdminWithoutPermissionForbidden() throws Exception {
        RebalanceTemplateService service = Mockito.mock(RebalanceTemplateService.class);
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(2L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(2L)).thenReturn(admin);

        AdminRebalanceTemplateController controller = new AdminRebalanceTemplateController(service);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(2L, RbacPermissionCodes.ADMIN_REBALANCE_TEMPLATE_VIEW)).thenReturn(false);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        factory.addAspect(permissionAspect);
        AdminRebalanceTemplateController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(2L);
        mockMvc.perform(get("/api/admin/rebalance-templates/list"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
    }
}
