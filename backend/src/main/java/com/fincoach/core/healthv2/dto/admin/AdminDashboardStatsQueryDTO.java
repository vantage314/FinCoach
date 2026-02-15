package com.fincoach.core.healthv2.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin dashboard stats query")
public class AdminDashboardStatsQueryDTO {

    @Min(7)
    @Max(365)
    @Schema(description = "Window days, default 30")
    private Integer windowDays;

    @Min(1)
    @Max(50)
    @Schema(description = "Recent size, default 10")
    private Integer recentSize;
}
