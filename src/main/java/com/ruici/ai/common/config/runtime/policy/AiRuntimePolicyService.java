package com.ruici.ai.common.config.runtime.policy;

import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeResolveContext;

/**
 * AI 运行时策略服务接口。
 * <p>校验治理层：约束允许的请求覆盖范围、校验解析结果快照的合法性。
 * 避免业务模块通过请求覆盖或错误配置导致运行时问题。</p>
 */
public interface AiRuntimePolicyService {
    void validateRequestOverride(AiRuntimeResolveContext context);
    void validateResolvedSnapshot(AiRuntimeConfigSnapshot snapshot, String clientType);
}
