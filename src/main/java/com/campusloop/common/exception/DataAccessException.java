package com.campusloop.common.exception;

/**
 * 数据访问层异常：屏蔽 JDBC 受检异常，避免 SQL 细节泄漏到业务层。
 */
public class DataAccessException extends CampusException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
