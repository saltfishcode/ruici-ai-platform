package com.ruici.ai.common.config.runtime;

/**
 * AI 运行时配置解析上下文。
 *
 * <p>context 由业务模块显式构造，避免在 common 层反向依赖各模块配置类。</p>
 */
public record AiRuntimeResolveContext(
    AiRuntimeDomain domain,
    AiRuntimeScene scene,
    String configKey,
    String requestProviderId,
    String requestModelName,
    String requestFallbackModelName,
    String staticProviderId,
    String staticModelName,
    String staticFallbackModelName,
    String snapshotKey,
    String clientType,
    boolean requestOverrideAllowed
) {
}
