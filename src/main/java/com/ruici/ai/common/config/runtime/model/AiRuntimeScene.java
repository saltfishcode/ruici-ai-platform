package com.ruici.ai.common.config.runtime.model;

/**
 * AI 运行时业务场景枚举。
 * <p>标记一条配置作用于哪个业务场景。解析器在查库时会先查精确场景匹配，
 * 未命中时回退到 {@link #GLOBAL} 场景（兜底配置）。</p>
 */
public enum AiRuntimeScene {
    GLOBAL, SIMULATION, KNOWLEDGEBASE, VOICE, DOCUMENT;

    public String code() {
        return name().toLowerCase();
    }

    public static AiRuntimeScene fromCode(String code) {
        for (AiRuntimeScene scene : values()) {
            if (scene.code().equalsIgnoreCase(code)) {
                return scene;
            }
        }
        throw new IllegalArgumentException("Unknown AiRuntimeScene: " + code);
    }
}
