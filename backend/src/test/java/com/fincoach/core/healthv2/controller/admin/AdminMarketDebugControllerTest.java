package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.GlobalExceptionHandler;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.debug.AdminMarketDebugMapper;
import com.fincoach.core.healthv2.debug.PortfolioDebugContextHolder;
import com.fincoach.core.healthv2.debug.PortfolioMarketDebugSnapshot;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

        AdminMarketDebugController controller = new AdminMarketDebugController(new AdminMarketDebugMapper());
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect aspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(1L, RbacPermissionCodes.ADMIN_MARKET_DEBUG_VIEW)).thenReturn(true);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(aspect);
        factory.addAspect(permissionAspect);
        AdminMarketDebugController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        PortfolioMarketDebugSnapshot snapshot = new PortfolioMarketDebugSnapshot();
        snapshot.setHistorySource("MARKET_DATA_DAILY_CLOSE");
        snapshot.setGeneratedAt(Instant.now());
        snapshot.setWarnings(new ArrayList<>());
        PortfolioMarketDebugSnapshot.CorrelationMatrixSummary summary = new PortfolioMarketDebugSnapshot.CorrelationMatrixSummary();
        summary.setAssetsCount(2);
        summary.setSampleSize(12);
        summary.setWarnings(List.of("CORR_INSUFFICIENT_POINTS:XYZ"));
        snapshot.setCorrelationMatrixSummary(summary);
        PortfolioMarketDebugSnapshot.CorrelationMatrixData matrix = new PortfolioMarketDebugSnapshot.CorrelationMatrixData();
        matrix.setAssets(List.of("A", "B"));
        matrix.setMatrix(List.of(List.of(1.0, 0.9), List.of(0.9, 1.0)));
        matrix.setMethod("pearson");
        matrix.setSampleSize(12);
        snapshot.setCorrelationMatrix(matrix);
        PortfolioMarketDebugSnapshot.DebtCashflowSummary debtCashflowSummary = new PortfolioMarketDebugSnapshot.DebtCashflowSummary();
        debtCashflowSummary.setDti(0.25);
        debtCashflowSummary.setSurplusRate(0.08);
        debtCashflowSummary.setEmergencyFundMonths(2.5);
        debtCashflowSummary.setStressLevel("MED");
        debtCashflowSummary.setWarningsCount(2);
        snapshot.setDebtCashflowSummary(debtCashflowSummary);
        PortfolioMarketDebugSnapshot.DebtOptimizerSummary debtOptimizerSummary = new PortfolioMarketDebugSnapshot.DebtOptimizerSummary();
        debtOptimizerSummary.setStrategy("AVALANCHE");
        debtOptimizerSummary.setTopDebtName("CREDIT_CARD");
        debtOptimizerSummary.setBudgetForExtraPayment(500.0);
        debtOptimizerSummary.setWarningsCount(1);
        snapshot.setDebtOptimizerSummary(debtOptimizerSummary);
        PortfolioMarketDebugSnapshot.InsuranceGapSummary insuranceGapSummary = new PortfolioMarketDebugSnapshot.InsuranceGapSummary();
        insuranceGapSummary.setPremiumRatio(0.08);
        insuranceGapSummary.setTopGapType("LIFE");
        insuranceGapSummary.setTopGapValue(120000.0);
        insuranceGapSummary.setWarningsCount(1);
        snapshot.setInsuranceGapSummary(insuranceGapSummary);
        PortfolioDebugContextHolder.enableCapture();
        PortfolioDebugContextHolder.set(snapshot);

        UserContext.setUserId(1L);
        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest").param("includeMatrix", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.history.source").value("MARKET_DATA_DAILY_CLOSE"))
                .andExpect(jsonPath("$.data.warnings").isArray())
                .andExpect(jsonPath("$.data.correlationMatrixSummary.assetsCount").value(2))
                .andExpect(jsonPath("$.data.correlationMatrixSummary.sampleSize").value(12))
                .andExpect(jsonPath("$.data.correlationMatrix.assets[0]").value("A"))
                .andExpect(jsonPath("$.data.correlationMatrix.matrix[0][0]").value(1.0))
                .andExpect(jsonPath("$.data.debtCashflowSummary.dti").value(0.25))
                .andExpect(jsonPath("$.data.debtCashflowSummary.surplusRate").value(0.08))
                .andExpect(jsonPath("$.data.debtCashflowSummary.emergencyFundMonths").value(2.5))
                .andExpect(jsonPath("$.data.debtCashflowSummary.stressLevel").value("MED"))
                .andExpect(jsonPath("$.data.debtCashflowSummary.warningsCount").value(2))
                .andExpect(jsonPath("$.data.debtOptimizerSummary.strategy").value("AVALANCHE"))
                .andExpect(jsonPath("$.data.debtOptimizerSummary.topDebtName").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.data.debtOptimizerSummary.budgetForExtraPayment").value(500.0))
                .andExpect(jsonPath("$.data.debtOptimizerSummary.warningsCount").value(1))
                .andExpect(jsonPath("$.data.insuranceGapSummary.premiumRatio").value(0.08))
                .andExpect(jsonPath("$.data.insuranceGapSummary.topGapType").value("LIFE"))
                .andExpect(jsonPath("$.data.insuranceGapSummary.topGapValue").value(120000.0))
                .andExpect(jsonPath("$.data.insuranceGapSummary.warningsCount").value(1))
                .andExpect(jsonPath("$.data.requestId").exists())
                .andExpect(jsonPath("$.data.timestamp").exists());
        UserContext.clear();
        PortfolioDebugContextHolder.clear();
    }

    @Test
    public void testNonAdminForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User user = new User();
        user.setId(2L);
        user.setRole("USER");
        when(userMapper.selectById(2L)).thenReturn(user);

        AdminMarketDebugController controller = new AdminMarketDebugController(new AdminMarketDebugMapper());
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect aspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(aspect);
        factory.addAspect(permissionAspect);
        AdminMarketDebugController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(2L);
        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
        PortfolioDebugContextHolder.clear();
    }

    @Test
    public void testNoUserContextForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);

        AdminMarketDebugController controller = new AdminMarketDebugController(new AdminMarketDebugMapper());
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect aspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(aspect);
        factory.addAspect(permissionAspect);
        AdminMarketDebugController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        PortfolioDebugContextHolder.clear();
    }

    @Test
    public void testEmptySnapshotReturnsStableSchema() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        AdminMarketDebugController controller = new AdminMarketDebugController(new AdminMarketDebugMapper());
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect aspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(1L, RbacPermissionCodes.ADMIN_MARKET_DEBUG_VIEW)).thenReturn(true);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(aspect);
        factory.addAspect(permissionAspect);
        AdminMarketDebugController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(1L);
        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warnings").isArray())
                .andExpect(jsonPath("$.data.warnings[0]").value("DEBUG_SNAPSHOT_EMPTY"))
                .andExpect(jsonPath("$.data.resolvedTickers").isArray());
        UserContext.clear();
    }

    @Test
    public void testAdminWithoutPermissionForbidden() throws Exception {
        UserMapper userMapper = Mockito.mock(UserMapper.class);
        User admin = new User();
        admin.setId(3L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(3L)).thenReturn(admin);

        AdminMarketDebugController controller = new AdminMarketDebugController(new AdminMarketDebugMapper());
        AdminChecker adminChecker = new AdminChecker(userMapper);
        AdminOnlyAspect adminAspect = new AdminOnlyAspect(adminChecker);
        PermissionChecker permissionChecker = Mockito.mock(PermissionChecker.class);
        when(permissionChecker.hasPermission(3L, RbacPermissionCodes.ADMIN_MARKET_DEBUG_VIEW)).thenReturn(false);
        PermissionAspect permissionAspect = new PermissionAspect(permissionChecker);
        AspectJProxyFactory factory = new AspectJProxyFactory(controller);
        factory.addAspect(adminAspect);
        factory.addAspect(permissionAspect);
        AdminMarketDebugController proxy = factory.getProxy();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        UserContext.setUserId(3L);
        mockMvc.perform(get("/api/admin/portfolio/market-debug/latest"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        UserContext.clear();
    }
}
