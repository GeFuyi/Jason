package com.egg.springboot_egg.config.GlobalExceptionHandler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通用接口返回包装类
 * @param <T> 返回数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 状态码
     * 200 OK
     * 400 参数错误
     * 401 未认证
     * 403 无权限
     * 404 资源未找到
     * 500 服务器错误
     */
    private int code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    // ==================== 成功返回 ====================

    public static <T> Result<T> ok() {
        return new Result<>(true, 200, "您又操作成功了！~~", null, LocalDateTime.now());
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(true, 200, "您又操作成功了！~~", data, LocalDateTime.now());
    }

    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(true, 200, message, data, LocalDateTime.now());
    }

    // ==================== 常用失败状态 ====================

    // 操作失败
    public static <T> Result<T> fail() {
        return new Result<>(false, 500, "操作失败", null, LocalDateTime.now());
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(false, 500, message, null, LocalDateTime.now());
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(false, code, message, null, LocalDateTime.now());
    }

    // 参数错误
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(false, 400, message, null, LocalDateTime.now());
    }

    // 未认证（未登录）
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(false, 401, message, null, LocalDateTime.now());
    }

    // 无权限访问
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(false, 403, message, null, LocalDateTime.now());
    }

    // 资源未找到
    public static <T> Result<T> notFound(String message) {
        return new Result<>(false, 404, message, null, LocalDateTime.now());
    }

    // ==================== 自定义构建 ====================

    public static <T> Result<T> build(boolean success, int code, String message, T data) {
        return new Result<>(success, code, message, data, LocalDateTime.now());
    }

}
