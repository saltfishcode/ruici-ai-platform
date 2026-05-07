package com.ruici.ai.common.config.runtime.model;

/**
 * AI 运行时快照来源标记枚举。
 * <p>标记一次解析结果最终来自哪一层优先级，用于日志追溯和问题排查。
 * 解析优先级：REQUEST_OVERRIDE &gt; DB_RUNTIME_CONFIG &gt; ENV_CONFIG &gt; LAST_KNOWN_GOOD。</p>
 */
public enum AiRuntimeConfigSource {
    REQUEST_OVERRIDE,
    DB_RUNTIME_CONFIG,
    ENV_CONFIG,
    LAST_KNOWN_GOOD;

    public String code() {
        return name().toLowerCase();
    }
}
