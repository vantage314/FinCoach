package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 体检v2-通知实体
 */
@Data
@TableName("fc_notification")
public class FcNotificationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 通知类型: ALERT / SYSTEM / ADVICE */
    private String type;

    private String title;

    private String content;

    /** 扩展信息(JSON) */
    private String payloadJson;

    /** 是否已读: 0=未读, 1=已读 */
    private Integer isRead;

    private LocalDateTime createdAt;
}
