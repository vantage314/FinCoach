package com.fincoach.core.common;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.fincoach.core.security.ForbiddenException;
import com.fincoach.core.security.UnauthorizedException;

import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null) {
            log.debug("参数校验失败: field={}, rejectedValue={}", fieldError.getField(), fieldError.getRejectedValue());
        }
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数校验失败";
        return ResponseEntity.badRequest().body(Result.error(400, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<String>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Result.error(400, e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.debug("请求体解析失败: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Result.error(400, "请求体不能为空或格式错误"));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result<String>> handleUnauthorizedException(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(401, e.getMessage()));
    }

    @ExceptionHandler({
            CannotGetJdbcConnectionException.class,
            DataAccessException.class,
            SQLException.class,
            MyBatisSystemException.class
    })
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleDatabaseException(Exception e) {
        log.error("数据库连接异常: ", e);
        return Result.error(500, "Database connection failed. Check DB_URL/DB_USERNAME/DB_PASSWORD.");
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        log.error("系统业务异常: ", e);
        return Result.error(500, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Result<String>> handleForbiddenException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(403, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统未知错误: ", e);
        return Result.error(500, "Debug Error: " + e.getClass().getName() + " - " + e.getMessage());
    }
}
