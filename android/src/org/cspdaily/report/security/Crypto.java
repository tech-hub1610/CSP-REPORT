package org.cspdaily.report.security;

import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Enterprise-grade cryptographic password utility using PBKDF2WithHmacSHA256.
 *
 * Parameters:
 * - Algorithm: PBKDF2WithHmacSHA256
 * - Iterations: 65,536
 * - Key Length: 256 bits (32 bytes)
 * - Salt: 16 bytes (128 bits) cryptographically secure random salt
 * - Encoding: Base64 (NO_WRAP)
 */
public final class Crypto {
    public static final int PBKDF2_ITERATIONS = 65536;
    public static final int KEY_LENGTH_BITS = 256;
    public static final int SALT_LENGTH_BYTES = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static final class HashResult {
        public final String hash;
        public final String salt;

        public HashResult(String hash, String salt) {
            this.hash = hash != null ? hash : "";
            this.salt = salt != null ? salt : "";
        }
    }

    private Crypto() {}

    private static String encodeBase64(byte[] data) {
        if (data == null) return "";
        try {
            return java.util.Base64.getEncoder().encodeToString(data);
        } catch (Throwable t) {
            try {
                return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
            } catch (Throwable t2) {
                return "";
            }
        }
    }

    private static byte[] decodeBase64(String str) {
        if (str == null || str.isEmpty()) return new byte[0];
        try {
            return java.util.Base64.getDecoder().decode(str);
        } catch (Throwable t) {
            try {
                return android.util.Base64.decode(str, android.util.Base64.NO_WRAP);
            } catch (Throwable t2) {
                return new byte[0];
            }
        }
    }

    /**
     * Generate a new 16-byte random salt and compute the PBKDF2 hash.
     */
    public static HashResult hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return new HashResult("", "");
        }
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        String saltBase64 = encodeBase64(salt);
        String hashBase64 = hashWithSalt(plainPassword.trim(), salt);
        return new HashResult(hashBase64, saltBase64);
    }

    /**
     * Compute PBKDF2 hash using an existing Base64 encoded salt.
     */
    public static String hashWithSalt(String plainPassword, String saltBase64) {
        if (plainPassword == null || saltBase64 == null || saltBase64.isEmpty()) {
            return "";
        }
        try {
            byte[] saltBytes = decodeBase64(saltBase64);
            return hashWithSalt(plainPassword.trim(), saltBytes);
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * Compute PBKDF2 hash using raw salt bytes.
     */
    public static String hashWithSalt(String plainPassword, byte[] saltBytes) {
        if (plainPassword == null || saltBytes == null || saltBytes.length == 0) {
            return "";
        }
        try {
            KeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return encodeBase64(hash);
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2WithHmacSHA256 computation failed", e);
        }
    }

    /**
     * Constant-time password verification against stored PBKDF2 hash and salt.
     */
    public static boolean verify(String plainPassword, String storedHash, String storedSalt) {
        if (plainPassword == null || storedHash == null || storedSalt == null) {
            return false;
        }
        String trimmedHash = storedHash.trim();
        String trimmedSalt = storedSalt.trim();
        if (trimmedHash.isEmpty() || trimmedSalt.isEmpty()) {
            return false;
        }
        try {
            String computedHash = hashWithSalt(plainPassword.trim(), trimmedSalt);
            if (computedHash.isEmpty()) {
                return false;
            }
            return MessageDigest.isEqual(trimmedHash.getBytes("UTF-8"), computedHash.getBytes("UTF-8"));
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Checks if a password string is already in PBKDF2 format or needs migration.
     */
    public static boolean isHashed(String hash, String salt) {
        return hash != null && !hash.trim().isEmpty() && salt != null && !salt.trim().isEmpty();
    }
}
