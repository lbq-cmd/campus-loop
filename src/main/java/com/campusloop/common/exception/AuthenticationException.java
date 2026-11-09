package com.campusloop.common.exception;

/**
 * 认证失败：用户名/密码错误、账号被封禁等。
 */
public class AuthenticationException extends CampusException {

    public AuthenticationException(String message) {
        super(message);
    }
}
