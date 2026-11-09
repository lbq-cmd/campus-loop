package com.campusloop.domain;

/**
 * 物品流转方式。
 */
public enum ItemType {
    /** 售卖 */
    SELL("出售"),
    /** 以物易物 */
    EXCHANGE("交换"),
    /** 免费赠送 */
    FREE("赠送");

    private final String label;

    ItemType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
