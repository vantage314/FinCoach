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
@Schema(description = "Recent alert summary")
public class RecentAlertVO {

    private Long id;

    private Long userId;

    private String ruleKey;

    private LocalDateTime triggerAt;

    private String status;

    private String payload;
}
