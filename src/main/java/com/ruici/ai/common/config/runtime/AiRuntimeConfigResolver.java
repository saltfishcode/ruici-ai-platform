package com.ruici.ai.common.config.runtime;

/**
 * AI 运行时配置解析器。
 */
public interface AiRuntimeConfigResolver {

    AiRuntimeConfigSnapshot resolveChatConfig(AiRuntimeResolveContext context);

    AiRuntimeConfigSnapshot refreshChatSnapshot(AiRuntimeResolveContext context);

    AiRuntimeConfigSnapshot resolveEmbeddingConfig(AiRuntimeResolveContext context);

    AiRuntimeConfigSnapshot refreshEmbeddingSnapshot(AiRuntimeResolveContext context);
}
