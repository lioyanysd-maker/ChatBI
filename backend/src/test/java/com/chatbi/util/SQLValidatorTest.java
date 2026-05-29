package com.chatbi.util;

import com.chatbi.config.SqlConfig;
import com.chatbi.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLValidatorTest {

    private SQLValidator validator;

    @BeforeEach
    void setUp() {
        SqlConfig config = new SqlConfig();
        config.setMaxLimit(1000);
        validator = new SQLValidator(config);
    }

    @Test
    void allowsSelectOnWhitelistedTable() {
        String sql = "SELECT id, name FROM products WHERE price > 100";
        String rewritten = validator.validateAndRewrite(sql, Set.of("products"));
        assertTrue(rewritten.toLowerCase().contains("limit"));
    }

    @Test
    void blocksDeleteStatement() {
        assertThrows(BusinessException.class, () ->
                validator.validateAndRewrite("DELETE FROM products", Set.of("products")));
    }

    @Test
    void blocksUnauthorizedTable() {
        assertThrows(BusinessException.class, () ->
                validator.validateAndRewrite("SELECT * FROM secret_table", Set.of("products")));
    }

    @Test
    void preservesExistingLimit() {
        String sql = "SELECT * FROM products LIMIT 20";
        assertDoesNotThrow(() -> validator.validateAndRewrite(sql, Set.of("products")));
    }
}
