package com.fincoach.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.controller.dto.AuthDTO;
import com.fincoach.core.security.UnauthorizedException;
import com.fincoach.core.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthControllerTest {

    private MockMvc buildMockMvc(AuthService authService) {
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authService", authService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    public void loginSuccess_returnsToken() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        when(authService.login(any(AuthDTO.class))).thenReturn("token-123");
        MockMvc mockMvc = buildMockMvc(authService);

        AuthDTO authDTO = new AuthDTO();
        authDTO.setUsername("admin");
        authDTO.setPassword("123456");
        ObjectMapper mapper = new ObjectMapper();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(authDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("token-123"));
    }

    @Test
    public void loginWrongPassword_returns401() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        when(authService.login(any(AuthDTO.class)))
                .thenThrow(new UnauthorizedException("用户名或密码错误"));
        MockMvc mockMvc = buildMockMvc(authService);

        AuthDTO authDTO = new AuthDTO();
        authDTO.setUsername("admin");
        authDTO.setPassword("bad-pass");
        ObjectMapper mapper = new ObjectMapper();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(authDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    public void loginMissingFields_returns400() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        MockMvc mockMvc = buildMockMvc(authService);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(authService);
    }
}
