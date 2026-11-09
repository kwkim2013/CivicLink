package com.civiclink.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final PasswordHasher hasher;
    private final String dummyHash;
    public AuthService(JdbcTemplate jdbc, PasswordHasher hasher,
                       @Value("${CIVICLINK_ADMIN_USERNAME:}") String admin,
                       @Value("${CIVICLINK_ADMIN_PASSWORD:}") String password) {
        this.jdbc = jdbc; this.hasher = hasher;
        this.dummyHash = hasher.hash("dummy-password-for-timing");
        if (!admin.isBlank() && !password.isBlank()) {
            validate(admin, password);
            if (jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE username=?", Integer.class, admin) == 0)
                jdbc.update("INSERT INTO app_users(username,password_hash,role) VALUES(?,?, 'ADMIN')", admin, hasher.hash(password));
        }
    }
    public SessionUser register(String username, String password) {
        validate(username, password);
        try { jdbc.update("INSERT INTO app_users(username,password_hash,role) VALUES(?,?, 'USER')", username, hasher.hash(password)); }
        catch (DuplicateKeyException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."); }
        return jdbc.queryForObject("SELECT id,username,role,organization,department,display_name FROM app_users WHERE username=?",
                (rs,n) -> new SessionUser(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)), username);
    }
    public SessionUser login(String username, String password) {
        validate(username, password);
        List<String> hashes = jdbc.query("SELECT password_hash FROM app_users WHERE username=?", (rs,n)->rs.getString(1), username);
        boolean matches = hasher.matches(password, hashes.isEmpty() ? dummyHash : hashes.get(0));
        if (hashes.isEmpty() || !matches) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        return jdbc.queryForObject("SELECT id,username,role,organization,department,display_name FROM app_users WHERE username=?",
                (rs,n) -> new SessionUser(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)), username);
    }
    public void changePassword(SessionUser user, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호를 입력해 주세요.");
        validate(user.username, newPassword);
        String stored = jdbc.queryForObject("SELECT password_hash FROM app_users WHERE id=?", String.class, user.id);
        if (!hasher.matches(currentPassword, stored))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다.");
        jdbc.update("UPDATE app_users SET password_hash=? WHERE id=?", hasher.hash(newPassword), user.id);
    }
    public SessionUser updateProfile(SessionUser user, String displayName, String organization, String department) {
        if (displayName == null || displayName.length() > 100 || organization == null || organization.length() > 200 || department == null || department.length() > 200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름은 100자, 소속기관과 부서는 200자 이내로 입력해 주세요.");
        jdbc.update("UPDATE app_users SET display_name=?, organization=?, department=? WHERE id=?", displayName.trim(), organization.trim(), department.trim(), user.id);
        return jdbc.queryForObject("SELECT id,username,role,organization,department,display_name FROM app_users WHERE id=?",
                (rs,n) -> new SessionUser(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)), user.id);
    }
    private void validate(String user, String password) {
        if (user == null || !user.matches("[a-zA-Z0-9_.-]{3,50}") || password == null || password.length()<12 || password.length()>128)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디는 영문·숫자·_.- 3~50자, 비밀번호는 12~128자여야 합니다.");
    }
}
