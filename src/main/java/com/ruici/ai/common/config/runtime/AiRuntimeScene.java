package com.ruici.ai.common.config.runtime;

import java.util.Arrays;

/**
 * AI 运行时业务场景。
 */
public enum AiRuntimeScene {

    GLOBAL("global"),
    SIMULATION("simulation"),
    KNOWLEDGEBASE("knowledgebase"),
    VOICE("voice"),
    DOCUMENT("document");

    private final String code;

    AiRuntimeScene(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AiRuntimeScene fromCode(String code) {
        return Arrays.stream(values())
            .filter(value -> value.code.equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown AI runtime scene: " + code));
    }
}
