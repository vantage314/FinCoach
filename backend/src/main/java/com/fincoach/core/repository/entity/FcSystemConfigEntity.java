package com.fincoach.core.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fc_system_config")
public class FcSystemConfigEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String cfgKey;
    private String cfgValue;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
