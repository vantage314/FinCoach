package com.fincoach.core.healthv2.rebalance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincoach.core.healthv2.entity.FcRebalanceTemplateEntity;
import com.fincoach.core.healthv2.mapper.FcRebalanceTemplateMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class RebalanceTemplateRegistryTest {

    @Test
    public void testReloadChangesActiveTemplate() {
        FcRebalanceTemplateMapper mapper = Mockito.mock(FcRebalanceTemplateMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();

        FcRebalanceTemplateEntity v1 = new FcRebalanceTemplateEntity();
        v1.setId(1L);
        v1.setCode("BALANCED");
        v1.setVersion(1);
        v1.setTemplateJson("{\"CASH\":0.15,\"BOND\":0.30}");

        FcRebalanceTemplateEntity v2 = new FcRebalanceTemplateEntity();
        v2.setId(2L);
        v2.setCode("BALANCED");
        v2.setVersion(2);
        v2.setTemplateJson("{\"CASH\":0.10,\"BOND\":0.40}");

        when(mapper.selectActive()).thenReturn(v1, v2);

        RebalanceTemplateRegistry registry = new RebalanceTemplateRegistry(mapper, objectMapper);
        registry.reload();
        assertEquals(1, registry.getActive().getVersion());

        registry.reload();
        assertEquals(2, registry.getActive().getVersion());
        Map<String, Double> targets = registry.getActive().getTargets();
        assertEquals(0.40, targets.get("BOND"));
    }
}
