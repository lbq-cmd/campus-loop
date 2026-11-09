package com.campusloop.domain;

/**
 * 物品状态机：ONLINE（在架）→ OFF_SHELF（下架）/ LOCKED（交易锁定）→ SOLD（已流转）；
 * 管理员可将违规物品置为 REMOVED（强制移除）。
 */
public enum ItemStatus {
    ONLINE,
    OFF_SHELF,
    LOCKED,
    SOLD,
    REMOVED;

    public boolean isVisibleInMarket() {
        return this == ONLINE;
    }
}
