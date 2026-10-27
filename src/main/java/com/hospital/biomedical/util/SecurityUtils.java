package com.hospital.biomedical.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Security utilities for password hashing and verification using salted SHA-256.
 * Zero external dependencies.
 */
public final class SecurityUtils {
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurityUtils() {}

    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public static String hashPassword(String password, String salt) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable.", e);
        }
    }

    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        if (password == null || salt == null || expectedHash == null) {
            return false;
        }
        String calculated = hashPassword(password, salt);
        return MessageDigest.isEqual(
                calculated.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}
