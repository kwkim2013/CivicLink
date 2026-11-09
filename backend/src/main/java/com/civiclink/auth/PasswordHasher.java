package com.civiclink.auth;

import org.springframework.stereotype.Component;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class PasswordHasher {
    public String hash(String password) {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        return "600000:" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(derive(password, salt, 600000));
    }
    public boolean matches(String password, String encoded) {
        try {
            String[] parts = encoded.split(":");
            return MessageDigest.isEqual(Base64.getDecoder().decode(parts[2]),
                    derive(password, Base64.getDecoder().decode(parts[1]), Integer.parseInt(parts[0])));
        } catch (RuntimeException e) { return false; }
    }
    private byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (Exception e) { throw new IllegalStateException("Password hashing failed", e); }
        finally { spec.clearPassword(); }
    }
}
