package com.chat.exception;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e) {
        logger.debug("客户端断开连接: {}", e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        logger.warn("业务异常: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<Map<String, Object>> handleUserBannedException(UserBannedException e) {
        logger.warn("用户被封禁: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e, WebRequest request) {
        if (e instanceof ClientAbortException) {
            logger.debug("客户端断开连接: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.OK).build();
        }

        String contentType = null;
        if (request instanceof ServletWebRequest swr) {
            contentType = swr.getResponse() != null ? swr.getResponse().getContentType() : null;
        }
        if (contentType != null && !contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            logger.debug("非 JSON 请求异常 ({}), 跳过错误响应: {}", contentType, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        logger.error("系统异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "服务器内部错误"));
    }
}
