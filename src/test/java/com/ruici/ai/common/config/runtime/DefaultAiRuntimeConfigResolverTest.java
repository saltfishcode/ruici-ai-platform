package com.ruici.ai.common.config.runtime;

import com.ruici.ai.common.config.LlmProviderProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI 运行时配置解析器测试")
class DefaultAiRuntimeConfigResolverTest {

    @Mock
    private AiRuntimeConfigRepository configRepository;

    private DefaultAiRuntimeConfigResolver createResolver() {
        LlmProviderProperties properties = new LlmProviderProperties();
        properties.setDefaultProvider("third-party");

        LlmProviderProperties.ProviderConfig providerConfig = new LlmProviderProperties.ProviderConfig();
        providerConfig.setBaseUrl("https://api-s.zwenooo.link/v1");
        providerConfig.setModel("gpt-5.2");
        providerConfig.setFallbackModel("qwen-plus");
        properties.setProviders(Map.of("third-party", providerConfig));

        DefaultAiRuntimePolicyService policyService = new DefaultAiRuntimePolicyService(properties);
        return new DefaultAiRuntimeConfigResolver(configRepository, policyService, properties);
    }

    private AiRuntimeResolveContext defaultContext() {
        return new AiRuntimeResolveContext(
            AiRuntimeDomain.CHAT,
            AiRuntimeScene.KNOWLEDGEBASE,
            "THIRD_PARTY_MODEL",
            null,
            null,
            null,
            "third-party",
            null,
            null,
            "knowledgebase:default:THIRD_PARTY_MODEL",
            "default",
            false
        );
    }

    @Nested
    @DisplayName("静态与数据库回退")
    class StaticAndDatabaseFallback {

        @Test
        @DisplayName("数据库未命中时回退到静态配置")
        void shouldFallbackToStaticConfigWhenDatabaseConfigMissing() {
            DefaultAiRuntimeConfigResolver resolver = createResolver();
            given(configRepository.findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
                "THIRD_PARTY_MODEL",
                "chat",
                "knowledgebase",
                Boolean.TRUE
            )).willReturn(Optional.empty());
            given(configRepository.findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
                "THIRD_PARTY_MODEL",
                "chat",
                "global",
                Boolean.TRUE
            )).willReturn(Optional.empty());

            AiRuntimeConfigSnapshot snapshot = resolver.resolveChatConfig(defaultContext());

            assertThat(snapshot.providerId()).isEqualTo("third-party");
            assertThat(snapshot.modelName()).isEqualTo("gpt-5.2");
            assertThat(snapshot.fallbackModelName()).isEqualTo("qwen-plus");
            assertThat(snapshot.source()).isEqualTo(AiRuntimeConfigSource.ENV_CONFIG);
            assertThat(snapshot.stale()).isFalse();
        }

        @Test
        @DisplayName("数据库命中时优先使用运行时配置")
        void shouldUseDatabaseRuntimeConfigWhenPresent() {
            DefaultAiRuntimeConfigResolver resolver = createResolver();
            AiRuntimeConfigEntity entity = AiRuntimeConfigEntity.builder()
                .id(1L)
                .configKey("THIRD_PARTY_MODEL")
                .domain("chat")
                .scene("knowledgebase")
                .providerId("third-party")
                .modelName("gpt-5.4")
                .fallbackModelName("qwen-max")
                .enabled(Boolean.TRUE)
                .priority(1)
                .configVersion(9L)
                .build();
            given(configRepository.findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
                "THIRD_PARTY_MODEL",
                "chat",
                "knowledgebase",
                Boolean.TRUE
            )).willReturn(Optional.of(entity));

            AiRuntimeConfigSnapshot snapshot = resolver.resolveChatConfig(defaultContext());

            assertThat(snapshot.providerId()).isEqualTo("third-party");
            assertThat(snapshot.modelName()).isEqualTo("gpt-5.4");
            assertThat(snapshot.fallbackModelName()).isEqualTo("qwen-max");
            assertThat(snapshot.configVersion()).isEqualTo(9L);
            assertThat(snapshot.source()).isEqualTo(AiRuntimeConfigSource.DB_RUNTIME_CONFIG);
        }

        @Test
        @DisplayName("数据库短时异常时回退到 last-known-good 快照")
        void shouldFallbackToLastKnownGoodWhenDatabaseFailsAfterSuccessfulResolution() {
            DefaultAiRuntimeConfigResolver resolver = createResolver();
            AiRuntimeConfigEntity entity = AiRuntimeConfigEntity.builder()
                .id(1L)
                .configKey("THIRD_PARTY_MODEL")
                .domain("chat")
                .scene("knowledgebase")
                .providerId("third-party")
                .modelName("gpt-5.4")
                .fallbackModelName("qwen-max")
                .enabled(Boolean.TRUE)
                .priority(1)
                .configVersion(9L)
                .build();
            given(configRepository.findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
                "THIRD_PARTY_MODEL",
                "chat",
                "knowledgebase",
                Boolean.TRUE
            )).willReturn(Optional.of(entity))
                .willThrow(new RuntimeException("db down"));

            AiRuntimeConfigSnapshot seededSnapshot = resolver.resolveChatConfig(defaultContext());
            AiRuntimeConfigSnapshot fallbackSnapshot = resolver.refreshChatSnapshot(defaultContext());

            assertThat(seededSnapshot.source()).isEqualTo(AiRuntimeConfigSource.DB_RUNTIME_CONFIG);
            assertThat(fallbackSnapshot.providerId()).isEqualTo("third-party");
            assertThat(fallbackSnapshot.modelName()).isEqualTo("gpt-5.4");
            assertThat(fallbackSnapshot.fallbackModelName()).isEqualTo("qwen-max");
            assertThat(fallbackSnapshot.configVersion()).isEqualTo(9L);
            assertThat(fallbackSnapshot.source()).isEqualTo(AiRuntimeConfigSource.LAST_KNOWN_GOOD);
            assertThat(fallbackSnapshot.stale()).isTrue();
            verify(configRepository, times(2))
                .findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
                    "THIRD_PARTY_MODEL",
                    "chat",
                    "knowledgebase",
                    Boolean.TRUE
                );
        }

        @Test
        @DisplayName("数据库异常且没有缓存时回退到静态配置并写入缓存")
        void shouldFallbackToStaticConfigWhenDatabaseThrowsWithoutLastKnownGood() {
            DefaultAiRuntimeConfigResolver resolver = createResolver();
            given(configRepository.findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
                "THIRD_PARTY_MODEL",
                "chat",
                "knowledgebase",
                Boolean.TRUE
            )).willThrow(new RuntimeException("db down"));

            AiRuntimeConfigSnapshot firstSnapshot = resolver.refreshChatSnapshot(defaultContext());
            AiRuntimeConfigSnapshot secondSnapshot = resolver.resolveChatConfig(defaultContext());

            assertThat(firstSnapshot.providerId()).isEqualTo("third-party");
            assertThat(firstSnapshot.modelName()).isEqualTo("gpt-5.2");
            assertThat(firstSnapshot.fallbackModelName()).isEqualTo("qwen-plus");
            assertThat(firstSnapshot.source()).isEqualTo(AiRuntimeConfigSource.ENV_CONFIG);
            assertThat(firstSnapshot.stale()).isFalse();
            assertThat(secondSnapshot).isEqualTo(firstSnapshot);
            verify(configRepository, times(1))
                .findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
                    "THIRD_PARTY_MODEL",
                    "chat",
                    "knowledgebase",
                    Boolean.TRUE
                );
        }
    }

    @Nested
    @DisplayName("请求级覆盖")
    class RequestOverride {

        @Test
        @DisplayName("允许覆盖时优先使用请求级 provider/model")
        void shouldPreferRequestOverrideWhenAllowed() {
            DefaultAiRuntimeConfigResolver resolver = createResolver();
            AiRuntimeResolveContext context = new AiRuntimeResolveContext(
                AiRuntimeDomain.CHAT,
                AiRuntimeScene.SIMULATION,
                "THIRD_PARTY_MODEL",
                "third-party",
                "gpt-5.4",
                "qwen-max",
                "third-party",
                null,
                null,
                "simulation:default:THIRD_PARTY_MODEL",
                "default",
                true
            );

            AiRuntimeConfigSnapshot snapshot = resolver.resolveChatConfig(context);

            assertThat(snapshot.providerId()).isEqualTo("third-party");
            assertThat(snapshot.modelName()).isEqualTo("gpt-5.4");
            assertThat(snapshot.fallbackModelName()).isEqualTo("qwen-max");
            assertThat(snapshot.source()).isEqualTo(AiRuntimeConfigSource.REQUEST_OVERRIDE);
        }
    }

    @Test
    @DisplayName("embedding 域可回退到静态 embedding model")
    void shouldResolveEmbeddingStaticFallback() {
        LlmProviderProperties properties = new LlmProviderProperties();
        properties.setDefaultEmbeddingProvider("dashscope");

        LlmProviderProperties.ProviderConfig providerConfig = new LlmProviderProperties.ProviderConfig();
        providerConfig.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode");
        providerConfig.setEmbeddingModel("text-embedding-v3");
        properties.setProviders(Map.of("dashscope", providerConfig));

        DefaultAiRuntimePolicyService policyService = new DefaultAiRuntimePolicyService(properties);
        DefaultAiRuntimeConfigResolver resolver = new DefaultAiRuntimeConfigResolver(configRepository, policyService, properties);

        AiRuntimeResolveContext context = new AiRuntimeResolveContext(
            AiRuntimeDomain.EMBEDDING,
            AiRuntimeScene.KNOWLEDGEBASE,
            "AI_EMBEDDING_MODEL",
            null,
            null,
            null,
            "dashscope",
            "text-embedding-v3",
            null,
            "knowledgebase:embedding:AI_EMBEDDING_MODEL",
            "embedding",
            false
        );

        given(configRepository.findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
            "AI_EMBEDDING_MODEL",
            "embedding",
            "knowledgebase",
            Boolean.TRUE
        )).willReturn(Optional.empty());
        given(configRepository.findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
            "AI_EMBEDDING_MODEL",
            "embedding",
            "global",
            Boolean.TRUE
        )).willReturn(Optional.empty());

        AiRuntimeConfigSnapshot snapshot = resolver.resolveEmbeddingConfig(context);

        assertThat(snapshot.domain()).isEqualTo(AiRuntimeDomain.EMBEDDING);
        assertThat(snapshot.providerId()).isEqualTo("dashscope");
        assertThat(snapshot.modelName()).isEqualTo("text-embedding-v3");
        assertThat(snapshot.fallbackModelName()).isNull();
        assertThat(snapshot.source()).isEqualTo(AiRuntimeConfigSource.ENV_CONFIG);
    }
}
