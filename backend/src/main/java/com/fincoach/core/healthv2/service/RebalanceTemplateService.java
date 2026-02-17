package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.entity.FcRebalanceTemplateEntity;
import com.fincoach.core.healthv2.rebalance.RebalanceTemplateSnapshot;

import java.util.List;

public interface RebalanceTemplateService {
    List<FcRebalanceTemplateEntity> list();
    Long save(FcRebalanceTemplateEntity dto);
    void publish(Long templateId);
    void reload();
    RebalanceTemplateSnapshot getActiveSnapshot();
}
