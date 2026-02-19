package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fc_alert")
public class FcAlertEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String alertType;
    private String severity;
    private String title;
    private String message;
    private String status;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private Integer hitCount;
    private String dedupeKey;
    private String source;
    private String metaJson;
}
