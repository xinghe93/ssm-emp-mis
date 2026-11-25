package com.xinghe.emp.common;

import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        var status = switch (exception.getCode()) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case EMPLOYEE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case AUTH_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return failure(status, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataConflict() {
        return failure(HttpStatus.CONFLICT, "请求数据与数据库约束冲突");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        LOGGER.error("请求处理失败，异常类型 {}", exception.getClass().getName());
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        String message = switch (status.value()) {
            case 400 -> validationMessage(exception);
            case 404 -> "接口不存在";
            case 405 -> "请求方法不受支持";
            case 406 -> "无法提供请求的响应格式";
            case 415 -> "请求内容类型不受支持";
            default -> status.is5xxServerError() ? "服务器内部错误" : "请求处理失败";
        };
        return super.handleExceptionInternal(exception, Result.error(status.value(), message), headers, status, request);
    }

    private static String validationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException invalid) {
            String fields = invalid.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + " " + (error.isBindingFailure() ? "格式不正确" : error.getDefaultMessage()))
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining("，"));
            if (!fields.isEmpty()) {
                return fields;
            }
        }
        if (exception instanceof HandlerMethodValidationException invalid) {
            String messages = invalid.getParameterValidationResults().stream()
                    .flatMap(result -> result.getResolvableErrors().stream())
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining("，"));
            if (!messages.isEmpty()) {
                return messages;
            }
        }
        return "请求参数缺失或格式不正确";
    }

    private static ResponseEntity<Result<Void>> failure(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Result.error(status.value(), message));
    }
}
