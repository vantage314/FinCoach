package com.fincoach.core.healthv2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "删除债务请求")
public class DebtDeleteDTO {

    @NotNull(message = "债务ID不能为空")
    @Schema(description = "债务ID", example = "1")
    private Long id;
}
