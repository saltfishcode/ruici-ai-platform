package com.ruici.ai.common.config.runtime;

/**
 * AI 运行时策略服务。
 */
public interface AiRuntimePolicyService {

    void validateRequestOverride(AiRuntimeResolveContext context);

    void validateResolvedSnapshot(AiRuntimeConfigSnapshot snapshot, String clientType);
}
