package com.lampify.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM encryption for sensitive order / payment fields at rest.
 * Ciphertext is stored as Base64(iv + ciphertext).
 */
@Service
public class EncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private static EncryptionService instance;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();
    private final boolean enabled;

    public EncryptionService(@Value("${app.encryption.secret:}") String secret) {
        this.enabled = secret != null && !secret.isBlank();
        this.secretKey = enabled ? deriveKey(secret.trim()) : null;
    }

    @PostConstruct
    void register() {
        instance = this;
    }

    public static EncryptionService getInstance() {
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String encrypt(String plaintext) {
        if (!enabled || plaintext == null) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception exception) {
            throw new IllegalStateException("Encryption failed", exception);
        }
    }

    public String decrypt(String stored) {
        if (!enabled || stored == null) {
            return stored;
        }
        // Pass through values that do not look like our Base64(iv+cipher) payload
        // so pre-encryption rows (if any) remain readable during rollout.
        if (!looksEncrypted(stored)) {
            return stored;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(stored);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Decryption failed", exception);
        }
    }

    private boolean looksEncrypted(String value) {
        if (value.length() < 24) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length > IV_BYTES + 16;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static SecretKey deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not derive encryption key", exception);
        }
    }
}
