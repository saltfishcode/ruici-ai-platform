package com.ruici.ai.common.config.runtime.snapshot;

import com.ruici.ai.common.config.runtime.model.AiRuntimeConfigSource;
import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;

/**
 * AI 运行时配置快照。
 * <p>一次解析请求的最终结果。包含调用真正使用的 provider、model、fallback model、版本号和来源标记。
 * 业务模块从 {@link com.ruici.ai.common.config.runtime.resolver.AiRuntimeConfigResolver} 拿到此快照后，
 传递给 {@link com.ruici.ai.common.ai.LlmProviderRegistry} 创建或获取 {@code ChatClient}。</p>
 *
 * <p>快照一旦生成即不可变，保证单次请求/任务/会话内使用同一份配置。</p>
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
