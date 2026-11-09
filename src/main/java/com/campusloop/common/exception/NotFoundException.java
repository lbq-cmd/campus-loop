package com.campusloop.common.exception;

/**
 * 目标数据不存在。
 */
public class NotFoundException extends CampusException {

    public NotFoundException(String message) {
        super(message);
    }
}
