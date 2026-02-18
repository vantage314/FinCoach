package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import com.fincoach.core.security.AdminChecker;
import com.fincoach.core.security.AdminOnlyAspect;
import com.fincoach.core.security.PermissionAspect;
import com.fincoach.core.security.PermissionChecker;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingDTO;
import com.fincoach.core.ticker.dto.admin.AdminTickerMappingSaveDTO;
import com.fincoach.core.ticker.service.admin.AdminTickerMappingService;
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

public class AdminSecuritiesMappingControllerTest {

    @Test
    public void testCreateListToggleOk() throws Exception {
        AdminTickerMappingService service = Mockito.mock(AdminTickerMappingService.class);
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        AdminTickerMappingDTO dto = new AdminTickerMappingDTO();
        dto.setId(1L);
        dto.setKeyword("AAPL");
        dto.setTicker("AAPL.US");
        Page<AdminTickerMappingDTO> page = new Page<>(1, 20);
        page.setRecords(List.of(dto));
        page.setTotal(1);
        when(service.page(any())).thenReturn(page);
        when(service.save(any())).thenReturn(1L);
        doNothing().when(service).disable(1L);

        AdminSecuritiesMappingController controller = new AdminSecuritiesMappingController(service);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(eq(1L), any())).thenReturn(true);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        factory.addAspect(permissionAspect);
        AdminSecuritiesMappingController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/admin/api/securities/mapping?keyword=AAPL&enabled=1&page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].keyword").value("AAPL"));

        AdminTickerMappingSaveDTO saveDTO = new AdminTickerMappingSaveDTO();
        saveDTO.setKeyword("AAPL");
        saveDTO.setTicker("AAPL.US");
        ObjectMapper mapper = new ObjectMapper();

        mockMvc.perform(post("/admin/api/securities/mapping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(saveDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(post("/admin/api/securities/mapping/1/toggle?enabled=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("disabled"));

        UserContext.clear();
    }
}
