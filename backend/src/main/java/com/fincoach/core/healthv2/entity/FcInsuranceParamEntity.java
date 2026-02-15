package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("fc_insurance_param")
public class FcInsuranceParamEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paramKey;
    private String valueJson;
    private String description;
    private LocalDateTime updatedAt;
}
