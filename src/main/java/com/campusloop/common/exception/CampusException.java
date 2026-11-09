package com.campusloop.common.exception;

/**
 * 业务异常基类：所有可预期的业务失败均继承本类，便于表现层统一捕获提示。
 */
public class CampusException extends RuntimeException {

    public CampusException(String message) {
        super(message);
    }

    public CampusException(String message, Throwable cause) {
        super(message, cause);
    }
}
