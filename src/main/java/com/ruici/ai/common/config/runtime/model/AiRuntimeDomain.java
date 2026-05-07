package com.ruici.ai.common.config.runtime.model;

/**
 * AI 能力域枚举。
 * <p>标记一条运行时配置归属于哪类 AI 能力：聊天、向量化、语音识别或语音合成。
 * 业务模块在构造 {@link com.ruici.ai.common.config.runtime.snapshot.AiRuntimeResolveContext} 时通过此枚举
 * 告知解析器需要解析哪类配置。</p>
 */
public enum AiRuntimeDomain {
    CHAT, EMBEDDING, ASR, TTS;

    public String code() {
        return name().toLowerCase();
    }

    public static AiRuntimeDomain fromCode(String code) {
        for (AiRuntimeDomain domain : values()) {
            if (domain.code().equalsIgnoreCase(code)) {
                return domain;
            }
        }
        throw new IllegalArgumentException("Unknown AiRuntimeDomain: " + code);
    }
}
