package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("fc_strategy_flag")
public class FcStrategyFlagEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String flagKey;
    private Boolean enabled;
    private String description;
    private LocalDateTime updatedAt;
}
