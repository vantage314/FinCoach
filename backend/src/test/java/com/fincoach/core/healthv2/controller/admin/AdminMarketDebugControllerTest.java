package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.healthv2.debug.PortfolioDebugContextHolder;
import com.fincoach.core.healthv2.debug.PortfolioMarketDebugSnapshot;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.repository.entity.User;
import com.fincoach.core.repository.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.ArrayList;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminMarketDebugControllerTest {

    @Test
    public void testAdminAccessOk() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        AdminMarketDebugController controller = new AdminMarketDebugController(userMapper);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        PortfolioMarketDebugSnapshot snapshot = new PortfolioMarketDebugSnapshot();
        snapshot.setHistorySource("MARKET_DATA_DAILY_CLOSE");
        snapshot.setGeneratedAt(Instant.now());
        snapshot.setWarnings(new ArrayList<>());
        PortfolioDebugContextHolder.set(snapshot);

        UserContext.setUserId(1L);
        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.historySource").value("MARKET_DATA_DAILY_CLOSE"));
        UserContext.clear();
    }

    @Test
    public void testNonAdminForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User user = new User();
        user.setId(2L);
        user.setRole("USER");
        when(userMapper.selectById(2L)).thenReturn(user);

        AdminMarketDebugController controller = new AdminMarketDebugController(userMapper);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(2L);
        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
    }
}
