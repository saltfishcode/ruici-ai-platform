package com.ruici.ai.common.config.runtime.policy;

import com.ruici.ai.common.config.runtime.dto.SaveAiRuntimeConfigRequest;
import com.ruici.ai.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AI 运行时配置保存校验测试")
class AiRuntimeConfigValidationServiceTest {

    private final AiRuntimeConfigValidationService validationService = new AiRuntimeConfigValidationService();

    @Test
    @DisplayName("chat 域允许 third-party 提供商")
    void shouldAcceptChatProvider() {
        SaveAiRuntimeConfigRequest request = new SaveAiRuntimeConfigRequest(
            null,
            "THIRD_PARTY_MODEL",
            "chat",
            "global",
            "third-party",
            "gpt-5.2",
            "qwen-plus",
            true,
            1,
            null
        );

        assertThatCode(() -> validationService.validateForSave(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("embedding 域拒绝非 dashscope 提供商")
    void shouldRejectUnsupportedEmbeddingProvider() {
        SaveAiRuntimeConfigRequest request = new SaveAiRuntimeConfigRequest(
            null,
            "AI_EMBEDDING_MODEL",
            "embedding",
            "knowledgebase",
            "third-party",
            "text-embedding-v3",
            null,
            true,
            1,
            null
        );

        assertThatThrownBy(() -> validationService.validateForSave(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("embedding")
            .hasMessageContaining("dashscope");
    }

    @Test
    @DisplayName("asr 域仅允许 dashscope 提供商")
    void shouldAcceptDashscopeForAsr() {
        SaveAiRuntimeConfigRequest request = new SaveAiRuntimeConfigRequest(
            null,
            "AI_ASR_MODEL",
            "asr",
            "voice",
            "dashscope",
            "qwen3-asr-flash-realtime",
            null,
            true,
            1,
            null
        );

        assertThatCode(() -> validationService.validateForSave(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("tts 域拒绝非 dashscope 提供商")
    void shouldRejectUnsupportedTtsProvider() {
        SaveAiRuntimeConfigRequest request = new SaveAiRuntimeConfigRequest(
            null,
            "AI_TTS_MODEL",
            "tts",
            "voice",
            "third-party",
            "qwen3-tts-flash-realtime",
            null,
            true,
            1,
            null
        );

        assertThatThrownBy(() -> validationService.validateForSave(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("asr/tts")
            .hasMessageContaining("dashscope");
    }
}
