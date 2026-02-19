package com.fincoach.core.healthv2.service;

import com.fincoach.core.healthv2.dto.AdviceRebalanceResponseDTO;

public interface AdviceRebalanceService {
    AdviceRebalanceResponseDTO buildRebalanceAdvice(Long userId);
}
