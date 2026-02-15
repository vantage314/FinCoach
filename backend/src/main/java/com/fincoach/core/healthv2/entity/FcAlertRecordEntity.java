package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("fc_alert_record")
public class FcAlertRecordEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String ruleKey;
    private String severity;
    private String payloadJson;
    private String message;
    private String status;
    private LocalDateTime createdAt;
}
