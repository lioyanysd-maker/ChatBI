package com.chatbi.util;

import cn.hutool.core.util.StrUtil;
import com.chatbi.config.SecurityConfig;
import com.chatbi.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecretCryptoService {

    private static final String PREFIX = "ENC:v1:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecurityConfig securityConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    private byte[] secretKey;

    @PostConstruct
    void init() {
        secretKey = deriveKey(resolveKeyMaterial());
        if (StrUtil.isBlank(securityConfig.getEncryptionKey())) {
            log.warn("未配置 chatbi.security.encryption-key，使用内置开发密钥；生产环境请务必设置 CHATBI_ENCRYPTION_KEY");
        }
    }

    public String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return plainText;
        }
        if (plainText.startsWith(PREFIX)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new BusinessException(500, "敏感信息加密失败");
        }
    }

    public String decrypt(String storedText) {
        if (StrUtil.isBlank(storedText)) {
            return storedText;
        }
        if (!storedText.startsWith(PREFIX)) {
            return storedText;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(storedText.substring(PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException(500, "敏感信息解密失败，请检查 encryption-key 是否正确");
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private String resolveKeyMaterial() {
        if (StrUtil.isNotBlank(securityConfig.getEncryptionKey())) {
            return securityConfig.getEncryptionKey().trim();
        }
        return "chatbi-dev-encryption-key";
    }

    private byte[] deriveKey(String material) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new BusinessException(500, "加密密钥初始化失败");
        }
    }
}
