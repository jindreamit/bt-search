package com.btsearch.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Token验证拦截器
 * 验证请求中的访问令牌
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TokenInterceptor.class);

    @Value("${app.access.token:shijin}")
    private String validToken;

    @Value("${app.access.header:X-Access-Token}")
    private String tokenHeader;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 从请求头或参数中获取 token
        String token = request.getHeader(tokenHeader);
        if (token == null) {
            token = request.getParameter("token");
        }

        // 验证 token
        if (validToken.equals(token)) {
            return true;
        }

        // 验证失败，记录日志并返回 401
        log.warn("Invalid token attempt from {}: {}", request.getRemoteAddr(), path);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\"}");
        return false;
    }
}
