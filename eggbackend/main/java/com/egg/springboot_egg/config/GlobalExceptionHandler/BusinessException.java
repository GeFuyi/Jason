package com.egg.springboot_egg.config.GlobalExceptionHandler;

/**
 * 自定义业务异常
 */
public class BusinessException extends RuntimeException {

    private int code;
    private String message;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 直接返回 Result 对象，方便 Service 层直接调用
     */
    public <T> Result<T> toResult() {
        return Result.fail(this.code, this.message);
    }
}
