package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("fc_score_rule_version")
public class FcScoreRuleVersionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String version;
    private String status;
    private String weightsJson;
    private String thresholdsJson;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
