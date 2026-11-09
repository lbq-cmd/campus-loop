package com.campusloop.common.exception;

/**
 * 越权操作：例如修改他人发布的物品、非管理员调用后台接口。
 */
public class PermissionDeniedException extends CampusException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}
