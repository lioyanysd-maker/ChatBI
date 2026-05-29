package com.chatbi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void stripsMarkdownBoldMarkers() {
        String raw = "围绕**\"人、货、场、交易\"**四大核心构建";
        String formatted = promptBuilder.formatDescriptiveAnswer(raw);
        assertFalse(formatted.contains("**"));
        assertTrue(formatted.contains("人、货、场、交易"));
    }
}
