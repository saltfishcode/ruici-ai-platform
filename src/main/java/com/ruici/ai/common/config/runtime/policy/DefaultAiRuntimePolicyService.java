package com.ruici.ai.common.config.runtime.policy;

import com.ruici.ai.common.config.LlmProviderProperties;
import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeResolveContext;
import com.ruici.ai.common.exception.BusinessException;
import com.ruici.ai.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DefaultAiRuntimePolicyService implements AiRuntimePolicyService {

    private final LlmProviderProperties llmProviderProperties;

    public DefaultAiRuntimePolicyService(LlmProviderProperties llmProviderProperties) {
        this.llmProviderProperties = llmProviderProperties;
    }

    @Override
    public void validateRequestOverride(AiRuntimeResolveContext context) {
        if (!context.requestOverrideAllowed()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前链路不允许请求级覆盖 AI Provider");
        }
        if (context.domain() != AiRuntimeDomain.CHAT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前阶段仅支持 chat 域请求级覆盖");
        }
        if (!StringUtils.hasText(context.requestProviderId()) && !StringUtils.hasText(context.requestModelName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求级覆盖未提供有效 provider/model");
        }
        if (StringUtils.hasText(context.requestProviderId())
            && !llmProviderProperties.getProviders().containsKey(context.requestProviderId().trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求级覆盖的 Provider 不存在: " + context.requestProviderId());
        }
    }

    @Override
    public void validateResolvedSnapshot(AiRuntimeConfigSnapshot snapshot, String clientType) {
        if (snapshot.domain() != AiRuntimeDomain.CHAT
            && snapshot.domain() != AiRuntimeDomain.EMBEDDING
            && snapshot.domain() != AiRuntimeDomain.ASR
            && snapshot.domain() != AiRuntimeDomain.TTS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的 AI 能力域: " + snapshot.domain());
        }
        if (!StringUtils.hasText(snapshot.providerId())) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 运行时快照缺少 providerId");
        }
        if (!StringUtils.hasText(snapshot.modelName())) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 运行时快照缺少 modelName");
        }
        if (llmProviderProperties.getProviders() == null
            || !llmProviderProperties.getProviders().containsKey(snapshot.providerId())) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 运行时快照引用了未知 Provider: " + snapshot.providerId());
        }
    }
}
