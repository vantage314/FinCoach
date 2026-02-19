package com.fincoach.core.healthv2.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fincoach.core.healthv2.entity.FcAlertEntity;
import com.fincoach.core.healthv2.entity.FcNotificationEntity;
import com.fincoach.core.healthv2.mapper.FcAlertMapper;
import com.fincoach.core.healthv2.mapper.FcNotificationMapper;
import com.fincoach.core.healthv2.service.impl.AlertServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

public class AlertServiceTest {

    @Test
    public void testDedupeIncrementsHitCount() {
        initTableInfo();
        FcAlertMapper alertMapper = Mockito.mock(FcAlertMapper.class);
        FcNotificationMapper notificationMapper = Mockito.mock(FcNotificationMapper.class);
        AlertServiceImpl service = new AlertServiceImpl();
        ReflectionTestUtils.setField(service, "alertMapper", alertMapper);
        ReflectionTestUtils.setField(service, "notificationMapper", notificationMapper);
        ReflectionTestUtils.setField(service, "objectMapper", TestSupport.objectMapper());

        FcAlertEntity existing = new FcAlertEntity();
        existing.setId(1L);
        existing.setUserId(1L);
        existing.setAlertType("DTI_DANGER");
        existing.setSeverity("WARN");
        existing.setStatus("OPEN");
        existing.setHitCount(1);
        existing.setLastSeenAt(LocalDateTime.now().minusDays(1));

        Mockito.when(alertMapper.selectOne(any())).thenReturn(existing);
        ArgumentCaptor<FcAlertEntity> captor = ArgumentCaptor.forClass(FcAlertEntity.class);
        Mockito.when(alertMapper.updateById(captor.capture())).thenReturn(1);

        service.raiseAlert(1L, "DTI_DANGER", "WARN", "title", "message", "HEALTHV2", Map.of());

        assertEquals(2, captor.getValue().getHitCount());
        Mockito.verify(notificationMapper, times(0)).insert(any());
    }

    @Test
    public void testNewAlertCreatesNotification() {
        initTableInfo();
        FcAlertMapper alertMapper = Mockito.mock(FcAlertMapper.class);
        FcNotificationMapper notificationMapper = Mockito.mock(FcNotificationMapper.class);
        AlertServiceImpl service = new AlertServiceImpl();
        ReflectionTestUtils.setField(service, "alertMapper", alertMapper);
        ReflectionTestUtils.setField(service, "notificationMapper", notificationMapper);
        ReflectionTestUtils.setField(service, "objectMapper", TestSupport.objectMapper());

        Mockito.when(alertMapper.selectOne(any())).thenReturn(null);
        Mockito.doAnswer(invocation -> {
            FcAlertEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        }).when(alertMapper).insert(any());

        ArgumentCaptor<FcNotificationEntity> notificationCaptor = ArgumentCaptor.forClass(FcNotificationEntity.class);
        Mockito.when(notificationMapper.insert(notificationCaptor.capture())).thenReturn(1);

        service.raiseAlert(1L, "CASHFLOW_NEGATIVE", "WARN", "title", "message", "HEALTHV2", Map.of("k", "v"));

        assertNotNull(notificationCaptor.getValue());
        assertEquals(1L, notificationCaptor.getValue().getAlertId());
    }

    @Test
    public void testSeverityEscalationCreatesNotification() {
        initTableInfo();
        FcAlertMapper alertMapper = Mockito.mock(FcAlertMapper.class);
        FcNotificationMapper notificationMapper = Mockito.mock(FcNotificationMapper.class);
        AlertServiceImpl service = new AlertServiceImpl();
        ReflectionTestUtils.setField(service, "alertMapper", alertMapper);
        ReflectionTestUtils.setField(service, "notificationMapper", notificationMapper);
        ReflectionTestUtils.setField(service, "objectMapper", TestSupport.objectMapper());

        FcAlertEntity existing = new FcAlertEntity();
        existing.setId(2L);
        existing.setUserId(1L);
        existing.setAlertType("DTI_DANGER");
        existing.setSeverity("WARN");
        existing.setStatus("OPEN");
        existing.setHitCount(1);

        Mockito.when(alertMapper.selectOne(any())).thenReturn(existing);
        Mockito.when(alertMapper.updateById(any())).thenReturn(1);

        service.raiseAlert(1L, "DTI_DANGER", "DANGER", "title", "message", "HEALTHV2", Map.of());

        Mockito.verify(notificationMapper, times(1)).insert(any());
    }

    private void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, FcAlertEntity.class);
        TableInfoHelper.initTableInfo(assistant, FcNotificationEntity.class);
    }

    private static final class TestSupport {
        private static com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }
    }
}
