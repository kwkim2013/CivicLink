package com.civiclink.auth;

import com.civiclink.config.DatabaseConfig;
import com.civiclink.complaint.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuthAndDatabaseTest {
    @Test void complaintPostAcceptsValidCsrfAndRejectsInvalidCsrf() {
        ApiAccessInterceptor gate = new ApiAccessInterceptor();
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/complaints");
        MockHttpServletResponse response = new MockHttpServletResponse();
        req.getSession().setAttribute("user", new SessionUser(1, "user", "USER"));
        String token = ApiAccessInterceptor.csrf(req.getSession());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> gate.preHandle(req, response, new Object())).getStatus().value());
        req.addHeader("X-CSRF-Token", "invalid");
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> gate.preHandle(req, response, new Object())).getStatus().value());
        req.removeHeader("X-CSRF-Token");
        req.addHeader("X-CSRF-Token", token);
        assertTrue(gate.preHandle(req, response, new Object()));
    }

    @Test void usersAreIsolatedAndRegistrationCannotChooseAdmin() {
        DatabaseConfig config=new DatabaseConfig();
        JdbcTemplate jdbc=config.jdbcTemplate(config.dataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
        AuthService auth=new AuthService(jdbc,new PasswordHasher(),"admin-test","AdminPassword123!");
        SessionUser a=auth.register("alice","AlicePassword123!");
        SessionUser b=auth.register("bob","BobPassword123!");
        assertEquals("USER",a.role);
        assertEquals(a.id,auth.login("alice","AlicePassword123!").id);
        assertThrows(ResponseStatusException.class,()->auth.login("alice","WrongPassword123!"));
        assertThrows(ResponseStatusException.class,()->auth.register("alice","AlicePassword123!"));
        ComplaintRepository repo=new ComplaintRepository(jdbc);
        Complaint created=repo.create(new Complaint(null,"테스트","내용","분류","FAKE"),a);
        assertEquals("RECEIVED",created.getStatus());
        assertNull(repo.find(created.getId(),b));
        assertEquals(0,repo.update(created.getId(),created,b));
        assertEquals(0,repo.delete(created.getId(),b));
        assertEquals(1,repo.list(auth.login("admin-test","AdminPassword123!")).size());
    }

    @Test void requiresAuthenticationCsrfAndAdminForCollection() {
        ApiAccessInterceptor gate=new ApiAccessInterceptor();
        MockHttpServletResponse response=new MockHttpServletResponse();
        MockHttpServletRequest req=new MockHttpServletRequest("GET","/api/complaints");
        assertThrows(ResponseStatusException.class,()->gate.preHandle(req,response,new Object()));
        req.setMethod("POST");req.setRequestURI("/api/decisions/index");
        req.getSession().setAttribute("user",new SessionUser(1,"user","USER"));
        assertThrows(ResponseStatusException.class,()->gate.preHandle(req,response,new Object()));
        req.addHeader("X-CSRF-Token",ApiAccessInterceptor.csrf(req.getSession()));
        assertThrows(ResponseStatusException.class,()->gate.preHandle(req,response,new Object()));
        req.setRequestURI("/api/decisions/index/");
        assertThrows(ResponseStatusException.class,()->gate.preHandle(req,response,new Object()));
        req.setRequestURI("/api/decisions/index;ignored=value");
        assertThrows(ResponseStatusException.class,()->gate.preHandle(req,response,new Object()));
        req.setRequestURI("/api/decisions/index");
        req.getSession().setAttribute("user",new SessionUser(2,"admin","ADMIN"));
        assertTrue(gate.preHandle(req,response,new Object()));
    }
}
