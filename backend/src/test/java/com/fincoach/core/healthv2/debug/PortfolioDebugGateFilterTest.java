package com.fincoach.core.healthv2.debug;

import com.fincoach.core.security.AdminChecker;
import com.fincoach.core.security.PermissionChecker;
import com.fincoach.core.utils.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class PortfolioDebugGateFilterTest {

    @AfterEach
    public void tearDown() {
        PortfolioDebugContextHolder.clear();
    }

    @Test
    public void headerMissingDoesNotEnableCapture() throws Exception {
        AdminChecker adminChecker = Mockito.mock(AdminChecker.class);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        JwtUtils jwtUtils = Mockito.mock(JwtUtils.class);
        PortfolioDebugGateFilter filter = new PortfolioDebugGateFilter(adminChecker, permissionChecker, jwtUtils);
        filter.setCaptureEnabled(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/app/portfolio/metrics");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);
        assertFalse(PortfolioDebugContextHolder.isCaptureEnabled());
    }

    @Test
    public void headerOnAdminEnablesCapture() throws Exception {
        AdminChecker adminChecker = Mockito.mock(AdminChecker.class);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        JwtUtils jwtUtils = Mockito.mock(JwtUtils.class);
        PortfolioDebugGateFilter filter = new PortfolioDebugGateFilter(adminChecker, permissionChecker, jwtUtils);
        filter.setCaptureEnabled(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/app/portfolio/metrics");
        request.addHeader("X-Debug-Market", "1");
        request.addHeader("Authorization", "Bearer test-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtils.getUserIdFromToken("test-token")).thenReturn(1L);
        when(adminChecker.isAdmin(1L)).thenReturn(true);
        when(permissionChecker.hasPermission(1L, "ADMIN_MARKET_DEBUG_CAPTURE")).thenReturn(true);

        filter.doFilter(request, response, chain);
        assertTrue(PortfolioDebugContextHolder.isCaptureEnabled());
    }

    @Test
    public void headerOnNonAdminDoesNotEnableCapture() throws Exception {
        AdminChecker adminChecker = Mockito.mock(AdminChecker.class);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        JwtUtils jwtUtils = Mockito.mock(JwtUtils.class);
        PortfolioDebugGateFilter filter = new PortfolioDebugGateFilter(adminChecker, permissionChecker, jwtUtils);
        filter.setCaptureEnabled(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/app/portfolio/metrics");
        request.addHeader("X-Debug-Market", "true");
        request.addHeader("Authorization", "Bearer test-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtils.getUserIdFromToken("test-token")).thenReturn(2L);
        when(adminChecker.isAdmin(2L)).thenReturn(false);

        filter.doFilter(request, response, chain);
        assertFalse(PortfolioDebugContextHolder.isCaptureEnabled());
    }
}
