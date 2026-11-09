package com.civiclink.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import javax.servlet.http.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    public static class Credentials { public String username; public String password; }
    public static class PasswordChange { public String currentPassword; public String newPassword; }
    public static class ProfileChange { public String displayName; public String organization; public String department; }
    @GetMapping("/csrf") public Map<String,String> csrf(HttpServletRequest req) {
        return Map.of("csrfToken", ApiAccessInterceptor.csrf(req.getSession()));
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public SessionUser register(@RequestBody Credentials input) { return service.register(input.username,input.password); }
    @PostMapping("/login") public Map<String,Object> login(@RequestBody Credentials input, HttpServletRequest req) {
        SessionUser user = service.login(input.username,input.password);
        HttpSession old = req.getSession(false); if (old != null) old.invalidate();
        HttpSession session = req.getSession(true); session.setMaxInactiveInterval(1800); session.setAttribute("user",user);
        return Map.of("user",user,"csrfToken",ApiAccessInterceptor.csrf(session));
    }
    @GetMapping("/me") public SessionUser me(HttpServletRequest req) { return ApiAccessInterceptor.user(req); }
    @PostMapping("/password") public Map<String,String> changePassword(@RequestBody PasswordChange input, HttpServletRequest req) {
        service.changePassword(ApiAccessInterceptor.user(req), input.currentPassword, input.newPassword);
        return Map.of("message", "비밀번호가 변경되었습니다.");
    }
    @PutMapping("/profile") public SessionUser updateProfile(@RequestBody ProfileChange input, HttpServletRequest req) {
        SessionUser updated = service.updateProfile(ApiAccessInterceptor.user(req), input.displayName, input.organization, input.department);
        req.getSession().setAttribute("user", updated);
        return updated;
    }
    @PostMapping("/logout") public Map<String,String> logout(HttpServletRequest req) {
        req.getSession().invalidate(); return Map.of("message","로그아웃되었습니다.");
    }
}
