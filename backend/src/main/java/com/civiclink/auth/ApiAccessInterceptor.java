package com.civiclink.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ApiAccessInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ApiAccessInterceptor.class);
    public static String csrf(HttpSession session) {
        if (session.getAttribute("csrf") == null) session.setAttribute("csrf", UUID.randomUUID().toString());
        return session.getAttribute("csrf").toString();
    }
    public static SessionUser user(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute("user");
        if (user == null) {
            log.warn("API 401: 로그인 세션 없음");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return user;
    }
    @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String path = new org.springframework.web.util.UrlPathHelper().getLookupPathForRequest(req).replaceAll("/+$", "");
        boolean safe = "GET".equals(req.getMethod()) || "HEAD".equals(req.getMethod()) || "OPTIONS".equals(req.getMethod());
        // 브라우저 CORS 사전 요청은 로그인·CSRF 검사 전에 통과시켜야 합니다.
        if ("OPTIONS".equals(req.getMethod())) return true;
        if (!safe) {
            HttpSession session = req.getSession(false);
            String token = req.getHeader("X-CSRF-Token");
            if (session == null || token == null || session.getAttribute("csrf") == null ||
                    !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), session.getAttribute("csrf").toString().getBytes(StandardCharsets.UTF_8))) {
                log.warn("API 403 CSRF: {} {}", req.getMethod(), path);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CSRF 토큰을 먼저 발급받아 X-CSRF-Token 헤더로 전달하세요.");
            }
        }
        if (path.equals("/api/auth/csrf") || path.equals("/api/auth/login") || path.equals("/api/auth/register")) return true;
        SessionUser user = user(req);
        boolean adminOnly = path.startsWith("/api/public-data/") || (!safe && (path.startsWith("/api/decisions/collect") ||
                path.matches("/api/decisions/[0-9]+/collect") || path.equals("/api/decisions/index")));
        if (adminOnly && !"ADMIN".equals(user.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        return true;
    }
}
