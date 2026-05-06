package com.ruici.ai.common.config.runtime;

/**
 * 单次 AI 解析得到的不可变快照。
 *
 * <p>chat-first 阶段先用于聊天链路，保证一次业务调用中的 provider/model/fallback 来源一致。</p>
 */
public record AiRuntimeConfigSnapshot(
    String configKey,
    AiRuntimeDomain domain,
    AiRuntimeScene scene,
    String providerId,
    String modelName,
    String fallbackModelName,
    Long configVersion,
    AiRuntimeConfigSource source,
    boolean stale
) {
}
