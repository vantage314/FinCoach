package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fc_score_rule_set")
public class FcScoreRuleSetEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Integer version;
    private Integer enabled;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
