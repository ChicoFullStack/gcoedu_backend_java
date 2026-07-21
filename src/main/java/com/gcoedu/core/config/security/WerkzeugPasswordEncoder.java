package com.gcoedu.core.config.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;
import java.nio.charset.StandardCharsets;

public class WerkzeugPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        try {
            int iterations = 600000;
            String salt = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            KeySpec spec = new PBEKeySpec(rawPassword.toString().toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, 32 * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] derived = factory.generateSecret(spec).getEncoded();
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : derived) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "pbkdf2:sha256:" + iterations + "$" + salt + "$" + hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error encoding password", e);
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.startsWith("pbkdf2:sha256:")) {
            return false;
        }

        String[] parts = encodedPassword.split("\\$");
        if (parts.length != 3) return false;

        String methodAndIterations = parts[0];
        String salt = parts[1];
        String hash = parts[2];

        int iterations = Integer.parseInt(methodAndIterations.split(":")[2]);

        try {
            KeySpec spec = new PBEKeySpec(rawPassword.toString().toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, 32 * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] derived = factory.generateSecret(spec).getEncoded();
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : derived) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hash.equals(hexString.toString());
        } catch (Exception e) {
            return false;
        }
    }
}
