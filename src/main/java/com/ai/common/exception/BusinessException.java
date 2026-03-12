package com.ai.common.exception;

public class BusinessException extends RuntimeException {
    private final Integer code;
    private final String message;

    public BusinessException(String message, Integer code) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }
}
