package com.fincoach.core.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风险测评记录实体
 */
@Data
@TableName("risk_assessment")
public class RiskAssessment {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Integer totalScore;
    
    private String riskLevel;
    
    private String assessmentJson;
    
    private LocalDateTime createTime;
}
