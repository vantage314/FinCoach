package com.fincoach.core.healthv2.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Score bucket")
public class BucketVO {

    @Schema(description = "Bucket label")
    private String label;

    @Schema(description = "Count in bucket")
    private Long count;
}
