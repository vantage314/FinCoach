package com.fincoach.core.healthv2.dto;

import lombok.Data;

@Data
public class AdminExecutionStatsVO {

    private Integer days;

    private Long reportsGenerated;

    private Long rebalanceConfirmCount;

    private Double rebalanceConfirmRate;

    private Double avgConfirmDelayHours;

    private Long highRiskUsersCount;

    private Long lowLiquidityUsersCount;

    private Long highDtiUsersCount;
}
