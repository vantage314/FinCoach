package com.fincoach.core.common;

import lombok.Getter;
import java.math.BigDecimal;

/**
 * 风险等级枚举
 * 基于"核心维度加权法"的科学评估结果
 */
@Getter
public enum RiskLevelEnum {
    CONSERVATIVE("conservative", "稳健守护者", "安逸是福，本金安全第一", new BigDecimal("0.10")),
    STEADY("steady", "稳健理财师", "稳中求进，追求跑赢通胀", new BigDecimal("0.25")),
    BALANCED("balanced", "平衡探索者", "攻守兼备，股债各半", new BigDecimal("0.40")),
    GROWTH("growth", "进取开拓者", "博取高收益，愿承担波动", new BigDecimal("0.60")),
    AGGRESSIVE("aggressive", "激进先锋", "富贵险中求，All in 核心资产", new BigDecimal("0.80"));

    private final String code;
    private final String label;
    private final String description;
    private final BigDecimal equityLimit;

    RiskLevelEnum(String code, String label, String description, BigDecimal equityLimit) {
        this.code = code;
        this.label = label;
        this.description = description;
        this.equityLimit = equityLimit;
    }

    /**
     * 根据分数获取风险等级
     * 一票否决：分数为0时强制保守型
     */
    public static RiskLevelEnum getByScore(int score) {
        if (score <= 20) return CONSERVATIVE;
        if (score <= 40) return STEADY;
        if (score <= 60) return BALANCED;
        if (score <= 80) return GROWTH;
        return AGGRESSIVE;
    }

    public static RiskLevelEnum getByCode(String code) {
        for (RiskLevelEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return BALANCED;
    }
}
