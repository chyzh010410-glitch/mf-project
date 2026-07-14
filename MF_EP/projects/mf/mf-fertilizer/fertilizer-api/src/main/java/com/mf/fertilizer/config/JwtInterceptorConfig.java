package com.mf.fertilizer.config;

import com.mf.fertilizer.annotation.RequireRole;
import com.mf.fertilizer.constant.RedisKey;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.infra.service.CacheService;
import com.mf.fertilizer.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class JwtInterceptorConfig implements WebMvcConfigurer {

    private final CacheService cacheService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor(cacheService))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/logout",
                        "/merchant/auth/register", "/merchant/auth/login",
                        "/client/auth/login", "/client/auth/register", "/client/auth/captcha", "/client/auth/reset-password",
                        "/client/products/**", "/client/categories/**",
                        "/client/encyclopedia/**", "/client/articles/**",
                        "/client/home", "/client/configs/public", "/client/faq/**", "/client/activities/**",
                        "/internal/**",
                        "/uploads/**",
                        "/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-resources/**", "/error"
                );
    }

    record JwtInterceptor(CacheService cacheService) implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            var requiredRoles = resolveRequiredRoles(request, handler);
            if (hasPublicRole(requiredRoles)) {
                return true;
            }
            var token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                response.setStatus(401);
                return false;
            }
            token = token.substring(7);
            String path = request.getRequestURI();
            String tokenKey = resolveTokenKey(path, token);
            if (!cacheService.hasKey(tokenKey)) {
                response.setStatus(401);
                return false;
            }
            // Parse JWT and populate ThreadLocal for downstream use
            var claims = JwtUtil.parse(token);
            var role = JwtUtil.getRole(claims);
            var userType = JwtUtil.getUserType(claims);
            if (!hasRequiredRole(requiredRoles, role, userType)) {
                response.setStatus(403);
                return false;
            }
            UserContext.set(
                    JwtUtil.getUserId(claims),
                    JwtUtil.getUsername(claims),
                    role,
                    userType
            );
            return true;
        }

        private boolean hasPublicRole(String[] requiredRoles) {
            for (var requiredRole : requiredRoles) {
                if (RoleEnum.PUBLIC.equals(requiredRole)) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasRequiredRole(String[] requiredRoles, String role, String userType) {
            for (var requiredRole : requiredRoles) {
                if (RoleEnum.hasRole(requiredRole, role, userType)) {
                    return true;
                }
            }
            return false;
        }

        private String[] resolveRequiredRoles(HttpServletRequest request, Object handler) {
            if (handler instanceof HandlerMethod handlerMethod) {
                var methodRole = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RequireRole.class);
                if (methodRole != null) {
                    return methodRole.value();
                }
                var classRole = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireRole.class);
                if (classRole != null) {
                    return classRole.value();
                }
            }
            return request.getRequestURI().startsWith("/client/")
                    ? new String[]{RoleEnum.CLIENT}
                    : request.getRequestURI().startsWith("/merchant/")
                    ? new String[]{RoleEnum.MERCHANT}
                    : new String[]{RoleEnum.ADMIN};
        }

        private String resolveTokenKey(String path, String token) {
            if (path.startsWith("/client/")) {
                return RedisKey.clientToken(token);
            }
            if (path.startsWith("/merchant/")) {
                return RedisKey.merchantToken(token);
            }
            return RedisKey.loginToken(token);
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            UserContext.clear();
        }
    }
}
