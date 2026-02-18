package com.fincoach.core.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fc_job_status")
public class FcJobStatusEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobName;
    private String status;
    private LocalDateTime lastStartAt;
    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime lastEndAt;
    private String lastError;
    private String lastLog;
    private LocalDateTime updatedAt;
}
