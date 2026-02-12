package com.fincoach.core.common;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统未知错误: ", e);
        return Result.error(500, "Debug Error: " + e.getClass().getName() + " - " + e.getMessage());
    }
}
