package com.fincoach.core.healthv2.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RebalanceConfirmRequestDTO {

    @NotNull(message = "reportId不能为空")
    private Long reportId;

    private String notes;

    private List<ExecutedAction> executedActions;

    @Data
    public static class ExecutedAction {
        private String action;
        private String type;
        private BigDecimal amount;
    }
}
