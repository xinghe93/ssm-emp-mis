package com.xinghe.emp.interceptor;

import com.xinghe.emp.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.xinghe.emp.common.BusinessException.Code.UNAUTHENTICATED;

public class LoginInterceptor implements HandlerInterceptor {

    public static final String AUTHENTICATED = "com.xinghe.emp.authenticated";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var session = request.getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute(AUTHENTICATED))) {
            throw new BusinessException(UNAUTHENTICATED, "请先登录");
        }
        return true;
    }
}
