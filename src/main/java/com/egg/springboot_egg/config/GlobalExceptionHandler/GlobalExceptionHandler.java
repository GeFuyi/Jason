package com.egg.springboot_egg.config.GlobalExceptionHandler;


import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------- 处理自定义业务异常 --------------------
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException ex) {
        Result<?> result = Result.fail(ex.getCode(), ex.getMessage());
        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    // -------------------- 处理参数校验异常 --------------------
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<?>> handleValidationException(Exception ex) {
        String message = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException manv = (MethodArgumentNotValidException) ex;
            message = manv.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (ex instanceof BindException) {
            BindException be = (BindException) ex;
            message = be.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        Result<?> result = Result.fail(HttpStatus.BAD_REQUEST.value(), message);
        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    // -------------------- 处理认证异常 --------------------
    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Result<?>> handleUnauthenticatedException(UnauthenticatedException ex) {
        Result<?> result = Result.fail(HttpStatus.UNAUTHORIZED.value(), "未认证或登录超时");
        return new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);
    }

    // -------------------- 处理权限异常 --------------------
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<Result<?>> handleAuthorizationException(AuthorizationException ex) {
        Result<?> result = Result.fail(HttpStatus.FORBIDDEN.value(), "无权限访问");
        return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
    }

    // -------------------- 处理所有其他异常 --------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception ex, HttpServletRequest request) {
        ex.printStackTrace(); // 打印到控制台方便调试
        Result<?> result = Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "服务器内部错误: " + ex.getMessage());
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
