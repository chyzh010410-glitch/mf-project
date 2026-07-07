package com.mf.fertilizer.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.platform.entity.SystemLog;
import com.mf.fertilizer.platform.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SystemLogService systemLogService;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog opLog) throws Throwable {
        var systemLog = new SystemLog();
        systemLog.setModule(opLog.module());
        systemLog.setAction(opLog.action());
        systemLog.setTarget(opLog.target());
        systemLog.setCreateTime(LocalDateTime.now());

        var currentUser = UserContext.getCurrentUser();
        if (currentUser != null) {
            systemLog.setOperatorId(currentUser.userId());
            systemLog.setOperatorName(currentUser.username());
        }

        try {
            var filteredArgs = new ArrayList<>();
            var args = joinPoint.getArgs();
            if (args != null) {
                for (Object arg : args) {
                    if (arg == null) continue;
                    if (arg instanceof HttpServletRequest) continue;
                    if (arg instanceof HttpServletResponse) continue;
                    filteredArgs.add(arg);
                }
            }
            if (!filteredArgs.isEmpty()) {
                systemLog.setRequestParams(MAPPER.writeValueAsString(filteredArgs));
            }
        } catch (Exception e) {
            systemLog.setRequestParams("[request params serialization failed]");
        }

        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest request = servletAttrs.getRequest();
                systemLog.setIp(getClientIp(request));
                systemLog.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception ignored) {
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            systemLog.setCostTime(System.currentTimeMillis() - start);
            systemLog.setResult("success");
            return result;
        } catch (Throwable e) {
            systemLog.setCostTime(System.currentTimeMillis() - start);
            systemLog.setResult("error");
            systemLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            try {
                systemLogService.save(systemLog);
            } catch (Exception e) {
                log.warn("Failed to save operation log: {}", e.getMessage());
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
