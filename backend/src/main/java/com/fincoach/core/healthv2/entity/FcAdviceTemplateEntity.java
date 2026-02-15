package com.fincoach.core.healthv2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("fc_advice_template")
public class FcAdviceTemplateEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sceneKey;
    private String templateText;
    private String variablesJson;
    private Boolean enabled;
    private LocalDateTime updatedAt;
}
