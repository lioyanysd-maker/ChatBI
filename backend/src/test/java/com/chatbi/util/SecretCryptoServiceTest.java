package com.chatbi.util;

import com.chatbi.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretCryptoServiceTest {

    private SecretCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        config.setEncryptionKey("test-key-for-unit-tests");
        cryptoService = new SecretCryptoService(config);
        cryptoService.init();
    }

    @Test
    void encryptsAndDecryptsPlainText() {
        String plain = "sk-test-api-key-123456";
        String encrypted = cryptoService.encrypt(plain);
        assertTrue(cryptoService.isEncrypted(encrypted));
        assertEquals(plain, cryptoService.decrypt(encrypted));
    }

    @Test
    void keepsLegacyPlainTextReadable() {
        assertEquals("legacy-password", cryptoService.decrypt("legacy-password"));
    }
}
