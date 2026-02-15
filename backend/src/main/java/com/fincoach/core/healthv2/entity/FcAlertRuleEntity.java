package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("fc_alert_rule")
public class FcAlertRuleEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleKey;
    private String thresholdsJson;
    private String severity;
    private String messageTemplate;
    private Boolean enabled;
    private LocalDateTime updatedAt;
}
