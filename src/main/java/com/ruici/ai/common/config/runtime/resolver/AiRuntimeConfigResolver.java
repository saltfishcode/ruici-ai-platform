package com.ruici.ai.common.config.runtime.resolver;

import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeResolveContext;

/**
 * AI 运行时配置解析器接口。
 * <p>统一入口：业务模块通过此接口获取 AI 模型配置的快照，而不是自己拼接解析优先级。
 * 调用方构造 {@link AiRuntimeResolveContext} 后传入，解析器返回最终命中的快照。</p>
 *
 * <p>实现类 {@link DefaultAiRuntimeConfigResolver} 按照
 * REQUEST_OVERRIDE → DB_RUNTIME_CONFIG → ENV_CONFIG → LAST_KNOWN_GOOD 的顺序解析。</p>
 */
public interface AiRuntimeConfigResolver {
    AiRuntimeConfigSnapshot resolveChatConfig(AiRuntimeResolveContext context);
    AiRuntimeConfigSnapshot refreshChatSnapshot(AiRuntimeResolveContext context);
    AiRuntimeConfigSnapshot resolveEmbeddingConfig(AiRuntimeResolveContext context);
    AiRuntimeConfigSnapshot refreshEmbeddingSnapshot(AiRuntimeResolveContext context);
    AiRuntimeConfigSnapshot resolveAsrConfig(AiRuntimeResolveContext context);
    AiRuntimeConfigSnapshot refreshAsrSnapshot(AiRuntimeResolveContext context);
    AiRuntimeConfigSnapshot resolveTtsConfig(AiRuntimeResolveContext context);
    AiRuntimeConfigSnapshot refreshTtsSnapshot(AiRuntimeResolveContext context);
}
