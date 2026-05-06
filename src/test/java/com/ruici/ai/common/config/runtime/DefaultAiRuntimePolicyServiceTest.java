package com.ruici.ai.common.config.runtime;

import com.ruici.ai.common.config.LlmProviderProperties;
import com.ruici.ai.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AI 运行时策略服务测试")
class DefaultAiRuntimePolicyServiceTest {

    private DefaultAiRuntimePolicyService createPolicyService() {
        LlmProviderProperties properties = new LlmProviderProperties();
        LlmProviderProperties.ProviderConfig providerConfig = new LlmProviderProperties.ProviderConfig();
        providerConfig.setModel("gpt-5.2");
        providerConfig.setFallbackModel("qwen-plus");
        providerConfig.setBaseUrl("https://api-s.zwenooo.link/v1");
        properties.setProviders(Map.of("third-party", providerConfig));
        return new DefaultAiRuntimePolicyService(properties);
    }

    @Test
    @DisplayName("合法 chat 快照通过校验")
    void shouldAcceptValidChatSnapshot() {
        DefaultAiRuntimePolicyService policyService = createPolicyService();
        AiRuntimeConfigSnapshot snapshot = new AiRuntimeConfigSnapshot(
            "THIRD_PARTY_MODEL",
            AiRuntimeDomain.CHAT,
            AiRuntimeScene.DOCUMENT,
            "third-party",
            "gpt-5.2",
            "qwen-plus",
            0L,
            AiRuntimeConfigSource.ENV_CONFIG,
            false
        );

        assertThatCode(() -> policyService.validateResolvedSnapshot(snapshot, "default"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("未知 Provider 的快照拒绝通过")
    void shouldRejectSnapshotWithUnknownProvider() {
        DefaultAiRuntimePolicyService policyService = createPolicyService();
        AiRuntimeConfigSnapshot snapshot = new AiRuntimeConfigSnapshot(
            "THIRD_PARTY_MODEL",
            AiRuntimeDomain.CHAT,
            AiRuntimeScene.DOCUMENT,
            "missing-provider",
            "gpt-5.2",
            "qwen-plus",
            0L,
            AiRuntimeConfigSource.ENV_CONFIG,
            false
        );

        assertThatThrownBy(() -> policyService.validateResolvedSnapshot(snapshot, "default"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("未知 Provider");
    }

    @Test
    @DisplayName("合法 embedding 快照通过校验")
    void shouldAcceptValidEmbeddingSnapshot() {
        LlmProviderProperties properties = new LlmProviderProperties();
        LlmProviderProperties.ProviderConfig providerConfig = new LlmProviderProperties.ProviderConfig();
        providerConfig.setEmbeddingModel("text-embedding-v3");
        providerConfig.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode");
        properties.setProviders(Map.of("dashscope", providerConfig));
        DefaultAiRuntimePolicyService policyService = new DefaultAiRuntimePolicyService(properties);

        AiRuntimeConfigSnapshot snapshot = new AiRuntimeConfigSnapshot(
            "AI_EMBEDDING_MODEL",
            AiRuntimeDomain.EMBEDDING,
            AiRuntimeScene.KNOWLEDGEBASE,
            "dashscope",
            "text-embedding-v3",
            null,
            3L,
            AiRuntimeConfigSource.DB_RUNTIME_CONFIG,
            false
        );

        assertThatCode(() -> policyService.validateResolvedSnapshot(snapshot, "embedding"))
            .doesNotThrowAnyException();
    }
}
