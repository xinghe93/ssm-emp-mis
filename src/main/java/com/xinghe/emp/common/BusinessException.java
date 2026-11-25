package com.xinghe.emp.common;

public class BusinessException extends RuntimeException {

    public enum Code {
        INVALID_ARGUMENT,
        EMPLOYEE_NOT_FOUND
    }

    private final Code code;

    public BusinessException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
