package com.fincoach.core.healthv2.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recent report summary")
public class RecentReportVO {

    private Long reportId;

    private Long userId;

    private LocalDateTime reportDate;

    private Integer healthScore;

    private Integer riskScore;

    private Integer behaviorScore;
}
