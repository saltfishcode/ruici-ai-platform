package com.ruici.ai.common.config.runtime;

import java.util.Arrays;

/**
 * AI 运行时能力域。
 *
 * <p>数据库中保存的是小写 code，避免 JPA Enum 默认序列化与现有 SQL seed 发生大小写偏差。</p>
 */
public enum AiRuntimeDomain {

    CHAT("chat"),
    EMBEDDING("embedding"),
    ASR("asr"),
    TTS("tts");

    private final String code;

    AiRuntimeDomain(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AiRuntimeDomain fromCode(String code) {
        return Arrays.stream(values())
            .filter(value -> value.code.equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown AI runtime domain: " + code));
    }
}
