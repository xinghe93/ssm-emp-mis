package com.xinghe.emp.controller;

import com.xinghe.emp.common.BusinessException;
import com.xinghe.emp.common.Result;
import com.xinghe.emp.dto.LoginRequest;
import com.xinghe.emp.interceptor.LoginInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.xinghe.emp.common.BusinessException.Code.AUTH_UNAVAILABLE;
import static com.xinghe.emp.common.BusinessException.Code.UNAUTHENTICATED;

@RestController
@RequestMapping(path = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final Environment environment;

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<Void>> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        String username = environment.getProperty("AUTH_USERNAME");
        String password = environment.getProperty("AUTH_PASSWORD");
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(AUTH_UNAVAILABLE, "实验登录账号尚未配置");
        }
        boolean passwordMatches = MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8),
                body.password().getBytes(StandardCharsets.UTF_8));
        if (!username.equals(body.username()) || !passwordMatches) {
            throw new BusinessException(UNAUTHENTICATED, "用户名或密码错误");
        }

        var session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        } else {
            request.changeSessionId();
        }
        session.setMaxInactiveInterval((int) Duration.ofMinutes(30).toSeconds());
        session.setAttribute(LoginInterceptor.AUTHENTICATED, Boolean.TRUE);
        return sessionResponse();
    }

    @PostMapping("/logout")
    public ResponseEntity<Result<Void>> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return sessionResponse();
    }

    private static ResponseEntity<Result<Void>> sessionResponse() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Result.success(null));
    }
}
