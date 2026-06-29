package com.bitpub.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String existingHash = "$2a$10$t3Xy7lV2wO3T6v9b2eLzZOMvB5HkH8H5XG8Z.0Y/R6YyW.JcM1q/C";
        boolean matches = encoder.matches("admin", existingHash);
        System.out.println("Existing hash matches 'admin': " + matches);
        System.out.println("New hash for 'admin': " + encoder.encode("admin"));
    }
}
