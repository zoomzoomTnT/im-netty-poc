package com.example.im.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Minimal HMAC demo for IM token verification.
 * Production: use proper key management, timestamp, nonce, short-lived tokens.
 */
public class HmacUtil {

    private static final String SECRET = "poc-secret-key-change-me"; // never hardcode in real code

    public static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyDemo(String token, String userId) {
        // For POC: token is simply the HMAC of userId
        String expected = sign(userId);
        return expected.equals(token);
    }

    public static void main(String[] args) {
        // Generate a demo token
        System.out.println(sign("alice"));
    }
}
