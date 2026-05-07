package com.ruici.ai.common.config.runtime.policy;

import com.ruici.ai.common.config.runtime.dto.SaveAiRuntimeConfigRequest;
import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;
import com.ruici.ai.common.exception.BusinessException;
import com.ruici.ai.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * AI 运行时配置保存校验服务。
 * <p>在前端或 API 保存配置时，校验 domain/scene 合法性以及 domain × provider 的约束关系。
 * 例如 embedding/asr/tts 域只能使用 dashscope 提供商。</p>
 */
@Service
public class AiRuntimeConfigValidationService {

    private static final Set<String> EMBEDDING_PROVIDERS = Set.of("dashscope");
    private static final Set<String> VOICE_PROVIDERS = Set.of("dashscope");

    public void validateForSave(SaveAiRuntimeConfigRequest request) {
        validateDomain(request.domain());
        validateScene(request.scene());
        validateProvider(request.domain(), request.providerId());
    }

    private void validateDomain(String domain) {
        try {
            AiRuntimeDomain.fromCode(domain);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                "不支持的 AI 能力域: " + domain + "，可选值: chat/embedding/asr/tts");
        }
    }

    private void validateScene(String scene) {
        try {
            AiRuntimeScene.fromCode(scene);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                "不支持的业务场景: " + scene + "，可选值: global/simulation/knowledgebase/voice/document");
        }
    }

    private void validateProvider(String domain, String providerId) {
        if (!StringUtils.hasText(providerId)) {
            return;
        }
        AiRuntimeDomain domainEnum = AiRuntimeDomain.fromCode(domain);
        switch (domainEnum) {
            case CHAT -> { }
            case EMBEDDING -> {
                if (!EMBEDDING_PROVIDERS.contains(providerId)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "向量模型 (embedding) 仅支持 dashscope 提供商，不支持的 provider: " + providerId);
                }
            }
            case ASR, TTS -> {
                if (!VOICE_PROVIDERS.contains(providerId)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "语音模型 (asr/tts) 仅支持 dashscope 提供商，不支持的 provider: " + providerId);
                }
            }
        }
    }
}
