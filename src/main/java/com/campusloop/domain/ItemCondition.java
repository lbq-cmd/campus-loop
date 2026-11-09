package com.campusloop.domain;

/**
 * 物品成色。
 */
public enum ItemCondition {
    NEW("全新"),
    LIKE_NEW("九九新"),
    GOOD("成色良好"),
    FAIR("有使用痕迹");

    private final String label;

    ItemCondition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
