package com.fincoach.core.healthv2.advice;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AdviceEngineResult {
    private List<AdviceDTO> advices = new ArrayList<>();
    private Map<String, Object> meta = new LinkedHashMap<>();
}
