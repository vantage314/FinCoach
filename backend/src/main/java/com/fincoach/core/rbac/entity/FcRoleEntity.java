package com.fincoach.core.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fc_role")
public class FcRoleEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
