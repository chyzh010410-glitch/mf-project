package com.mf.fertilizer.config;

import com.mf.fertilizer.constant.ResultCode;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResultVO<Void> handleBusiness(BusinessException e) {
        return ResultVO.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public ResultVO<Void> handleBind(BindException e) {
        var msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ResultVO.fail(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResultVO<Void> handleNoResource(NoResourceFoundException e) {
        return ResultVO.fail(ResultCode.NOT_FOUND, "接口不存在: " + e.getResourcePath());
    }

    @ExceptionHandler(Exception.class)
    public ResultVO<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ResultVO.fail("系统繁忙，请稍后重试");
    }
}
