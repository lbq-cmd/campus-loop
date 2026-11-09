package com.campusloop.common.exception;

/**
 * 输入校验不通过（防御性编程的第一道关口）。
 */
public class ValidationException extends CampusException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
