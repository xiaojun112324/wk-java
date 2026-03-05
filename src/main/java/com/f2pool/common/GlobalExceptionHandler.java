package com.f2pool.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 捕获所有未处理的异常，并打印堆栈信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e) {
        log.error("系统发生未知异常: ", e); // 打印完整堆栈
        return R.fail("系统错误: " + e.getMessage());
    }
}
