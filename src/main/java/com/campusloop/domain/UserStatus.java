package com.campusloop.domain;

/**
 * 账号状态。
 */
public enum UserStatus {
    /** 正常 */
    ACTIVE,
    /** 被管理员封禁，禁止登录与发布 */
    BANNED
}
