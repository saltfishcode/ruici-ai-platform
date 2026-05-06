package com.ruici.ai.common.config.runtime;

/**
 * AI 运行时配置命中来源。
 */
public enum AiRuntimeConfigSource {
    REQUEST_OVERRIDE,
    DB_RUNTIME_CONFIG,
    ENV_CONFIG,
    LAST_KNOWN_GOOD,
    CODE_DEFAULT
}
