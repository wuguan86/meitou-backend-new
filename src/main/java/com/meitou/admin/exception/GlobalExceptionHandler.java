package com.meitou.admin.exception;

import com.meitou.admin.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理异常，返回友好的错误信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理业务异常
     * 返回 HTTP 200，但在响应体中包含业务错误码
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.ok(Result.error(e.getCode(), e.getMessage()));
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<?>> handleValidationException(Exception e) {
        String message = "参数验证失败";
        
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            message = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        } else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            message = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        }
        
        log.warn("参数验证失败: {}", message);
        // 参数校验失败属于客户端错误，可以使用 400，也可以使用 200 + 错误码
        // 这里遵循业务异常处理，返回 200 + 400 code
        return ResponseEntity.ok(Result.error(400, message));
    }
    
    /**
     * 处理运行时异常
     * 区分业务逻辑中未捕获的运行时异常和真正的系统崩溃
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<?>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        // 运行时异常通常视为系统错误，返回 500
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "系统处理请求时发生错误: " + e.getMessage()));
    }
    
    /**
     * 处理其他异常
     * 真正的系统崩溃/未预料的错误 -> HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "系统内部错误，请联系管理员"));
    }
}

