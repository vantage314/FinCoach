package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import com.fincoach.core.security.AdminChecker;
import com.fincoach.core.security.AdminOnlyAspect;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminTickerMappingControllerTest {

    @Test
    public void testAdminPageSaveReloadOk() throws Exception {
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
        when(service.page(any())).thenReturn(page);
        when(service.save(any())).thenReturn(1L);
        doNothing().when(service).reload();

        AdminTickerMappingController controller = new AdminTickerMappingController(service);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect aspect = new AdminOnlyAspect(adminChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(aspect);
        AdminTickerMappingController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/api/admin/ticker-mapping/page?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].keyword").value("AAPL"));

        AdminTickerMappingSaveDTO saveDTO = new AdminTickerMappingSaveDTO();
        saveDTO.setKeyword("AAPL");
        saveDTO.setTicker("AAPL.US");
        ObjectMapper mapper = new ObjectMapper();

        mockMvc.perform(post("/api/admin/ticker-mapping/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(saveDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(post("/api/admin/ticker-mapping/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        UserContext.clear();
    }

    @Test
    public void testNonAdminForbidden() throws Exception {
        AdminTickerMappingService service = Mockito.mock(AdminTickerMappingService.class);
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User user = new User();
        user.setId(2L);
        user.setRole("USER");
        when(userMapper.selectById(2L)).thenReturn(user);

        AdminTickerMappingController controller = new AdminTickerMappingController(service);
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect aspect = new AdminOnlyAspect(adminChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(aspect);
        AdminTickerMappingController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(2L);
        mockMvc.perform(get("/api/admin/ticker-mapping/page?page=1&size=20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
    }
}
