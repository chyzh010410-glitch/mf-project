package com.mf.fertilizer.config;

import com.mf.fertilizer.annotation.RequireRole;
import com.mf.fertilizer.constant.RedisKey;
import com.mf.fertilizer.constant.RoleEnum;
import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.infra.service.CacheService;
import com.mf.fertilizer.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtInterceptorConfigTest {

    private final CacheService cacheService = mock(CacheService.class);
    private final JwtInterceptorConfig.JwtInterceptor interceptor = new JwtInterceptorConfig.JwtInterceptor(cacheService);

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void publicEndpointSkipsTokenCheck() throws Exception {
        var request = request("/client/fertilization/rules/recommend", null);
        var response = new MockHttpServletResponse();

        var allowed = interceptor.preHandle(request, response, handler("publicEndpoint"));

        assertTrue(allowed);
        assertFalse(UserContext.isAuthenticated());
    }

    @Test
    void protectedEndpointRejectsMissingToken() {
        var request = request("/client/orders", null);
        var response = new MockHttpServletResponse();

        var allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void defaultClientEndpointPopulatesUserContext() throws Exception {
        var token = JwtUtil.generateWithUserType(8L, "client", RoleEnum.CONSUMER, RoleEnum.CONSUMER);
        when(cacheService.hasKey(RedisKey.clientToken(token))).thenReturn(true);
        var request = request("/client/orders", token);
        var response = new MockHttpServletResponse();

        var allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(8L, UserContext.requireUserId());
        assertTrue(UserContext.isClientUser());

        interceptor.afterCompletion(request, response, new Object(), null);
        assertFalse(UserContext.isAuthenticated());
    }

    @Test
    void superAdminEndpointRejectsOperatorRole() throws Exception {
        var token = JwtUtil.generateWithUserType(9L, "operator", RoleEnum.OPERATOR, RoleEnum.ADMIN);
        when(cacheService.hasKey(RedisKey.loginToken(token))).thenReturn(true);
        var request = request("/admin/secure", token);
        var response = new MockHttpServletResponse();

        var allowed = interceptor.preHandle(request, response, handler("superAdminEndpoint"));

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
        assertFalse(UserContext.isAuthenticated());
    }

    private MockHttpServletRequest request(String path, String token) {
        var request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        return request;
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new TestController(), TestController.class.getDeclaredMethod(methodName));
    }

    private static class TestController {

        @RequireRole(RoleEnum.PUBLIC)
        void publicEndpoint() {
        }

        @RequireRole(RoleEnum.SUPER_ADMIN)
        void superAdminEndpoint() {
        }
    }
}
