package com.campusloop.common.exception;

/**
 * 数据重复（如用户名、分类名已存在）。
 */
public class DuplicateException extends CampusException {

    public DuplicateException(String message) {
        super(message);
    }
}
