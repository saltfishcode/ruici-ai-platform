package com.ruici.ai.common.config.runtime.snapshot;

import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;

/**
 * AI 运行时配置解析上下文。
 * <p>业务模块在调用解析器前构造此对象，携带本次解析需要的全部输入条件。
 * 包括当前请求的 domain/scene、是否允许请求级覆盖、请求级 provider/model、静态配置覆盖等。</p>
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
