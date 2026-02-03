package com.fincoach.core.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资产分类枚举
 * formType: SIMPLE-仅名称金额, INVEST-需选子类型, PROPERTY-可填备注
 */
@Getter
@AllArgsConstructor
public enum AssetCategoryEnum {
    CASH(1, "现金", "SIMPLE", "wallet"),
    INVESTMENT(2, "金融投资", "INVEST", "chart-line"),
    PROPERTY(3, "固定资产", "PROPERTY", "house");

    private final Integer id;
    private final String name;
    private final String formType;
    private final String iconSlug;

    public static AssetCategoryEnum getById(Integer id) {
        for (AssetCategoryEnum e : values()) {
            if (e.getId().equals(id)) {
                return e;
            }
        }
        return null;
    }
}
