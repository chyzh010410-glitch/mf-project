package com.mf.datacenter.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.mf.datacenter.ai.HistoryAccessService;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HistoryAccessService.HistoryUnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<String> handleUnauthorized() { return new ApiResponse<>(401, "未认证", null); }

    @ExceptionHandler(HistoryAccessService.HistoryForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<String> handleForbidden(HistoryAccessService.HistoryForbiddenException ex) { return new ApiResponse<>(403, ex.getMessage(), null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handleValidation(MethodArgumentNotValidException ex) {
        var error = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("invalid request");
        return new ApiResponse<>(400, error, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handleIllegalArgument(IllegalArgumentException ex) {
        return new ApiResponse<>(400, ex.getMessage(), null);
    }
}
