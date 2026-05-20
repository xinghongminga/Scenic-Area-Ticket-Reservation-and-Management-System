package com.example.scencispotback.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
// 全局异常处理
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    // 说明: 执行handleBiz方法（handle biz）。
    public ApiResponse<Void> handleBiz(BizException e) {
        return ApiResponse.fail(e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    // 说明: 执行handleValidation方法（handle validation）。
    public ApiResponse<Void> handleValidation(Exception e) {
        return ApiResponse.fail("参数校验失败: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    // 说明: 执行handleUnknown方法（handle unknown）。
    public ApiResponse<Void> handleUnknown(Exception e) {
        return ApiResponse.fail("系统异常: " + e.getMessage());
    }
}
