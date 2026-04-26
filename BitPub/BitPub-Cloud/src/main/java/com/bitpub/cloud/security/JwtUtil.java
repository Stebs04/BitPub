package com.bitpub.cloud.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility per la gestione dei token JWT in modo stateless, senza dipendenze esterne.
 * Utilizza HMAC-SHA256 per la firma.
 *
 * @author BitPub Team
 * @version 1.0
 */
@Component
public class JwtUtil {

    private static final String SECRET = "UnaChiaveSegretaMoltoLungaPerGarantireSicurezzaBitPub2024!";
    private static final long EXPIRATION_TIME = 86400000; // 1 giorno in millisecondi

    public String generateToken(String username, String role) {
        try {
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            long exp = System.currentTimeMillis() + EXPIRATION_TIME;
            String payload = "{\"sub\":\"" + username + "\",\"role\":\"" + role + "\",\"exp\":" + exp + "}";

            String base64UrlHeader = encodeBase64Url(header.getBytes(StandardCharsets.UTF_8));
            String base64UrlPayload = encodeBase64Url(payload.getBytes(StandardCharsets.UTF_8));

            String signature = hmacSha256(base64UrlHeader + "." + base64UrlPayload, SECRET);

            return base64UrlHeader + "." + base64UrlPayload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Errore durante la generazione del token JWT", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String signature = hmacSha256(parts[0] + "." + parts[1], SECRET);
            if (!signature.equals(parts[2])) return false;

            String payload = new String(decodeBase64Url(parts[1]), StandardCharsets.UTF_8);
            Pattern expPattern = Pattern.compile("\"exp\":(\\d+)");
            Matcher matcher = expPattern.matcher(payload);
            if (matcher.find()) {
                long exp = Long.parseLong(matcher.group(1));
                if (System.currentTimeMillis() > exp) return false;
            } else {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        String payload = extractPayload(token);
        return extractClaim(payload, "\"sub\":\"([^\"]+)\"");
    }

    public String extractRole(String token) {
        String payload = extractPayload(token);
        return extractClaim(payload, "\"role\":\"([^\"]+)\"");
    }

    private String extractPayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("Token non valido");
        return new String(decodeBase64Url(parts[1]), StandardCharsets.UTF_8);
    }

    private String extractClaim(String payload, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(payload);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String encodeBase64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] decodeBase64Url(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url);
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return encodeBase64Url(hash);
    }
}
