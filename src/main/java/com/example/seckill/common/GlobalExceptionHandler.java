package com.example.seckill.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        return ApiResponse.fail(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() == null
                ? "参数错误"
                : e.getBindingResult().getFieldError().getDefaultMessage();
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException e) {
        return ApiResponse.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception e) {
        return ApiResponse.fail(e.getMessage() == null ? "服务异常" : e.getMessage());
    }
}
