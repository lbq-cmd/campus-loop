package com.campusloop.domain;

/**
 * 用户角色。
 */
public enum Role {
    /** 普通学生用户 */
    STUDENT("普通用户"),
    /** 系统管理员 */
    ADMIN("管理员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
