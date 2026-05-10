package com.ruici.ai.common.config.runtime;

import com.ruici.ai.common.ai.LlmProviderRegistry;
import com.ruici.ai.common.config.runtime.dto.RefreshAiRuntimeConfigResponse;
import com.ruici.ai.common.config.runtime.dto.SaveAiRuntimeConfigRequest;
import com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigEntity;
import com.ruici.ai.common.config.runtime.policy.AiRuntimeConfigValidationService;
import com.ruici.ai.common.config.runtime.repository.AiRuntimeConfigAuditRepository;
import com.ruici.ai.common.config.runtime.repository.AiRuntimeConfigRepository;
import com.ruici.ai.common.config.runtime.resolver.AiRuntimeConfigResolver;
import com.ruici.ai.common.config.runtime.resolver.DefaultAiRuntimeConfigResolver;
import com.ruici.ai.common.config.runtime.service.AiRuntimeCacheInvalidationNotifier;
import com.ruici.ai.common.config.runtime.service.AiRuntimeConfigCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI 运行时配置命令服务测试")
class AiRuntimeConfigCommandServiceTest {

    @Mock
    private AiRuntimeConfigRepository configRepository;
    @Mock
    private AiRuntimeConfigAuditRepository auditRepository;
    @Mock
    private AiRuntimeConfigValidationService validationService;
    @Mock
    private AiRuntimeConfigResolver configResolver;
    @Mock
    private LlmProviderRegistry llmProviderRegistry;
    @Mock
    private DefaultAiRuntimeConfigResolver defaultResolver;
    @Mock
    private AiRuntimeCacheInvalidationNotifier cacheInvalidationNotifier;

    private AiRuntimeConfigCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new AiRuntimeConfigCommandService(
            configRepository, auditRepository, validationService,
            configResolver, llmProviderRegistry, defaultResolver, cacheInvalidationNotifier);
    }

    @Nested
    @DisplayName("保存配置")
    class Save {

        @Test
        @DisplayName("新增配置走创建逻辑")
        void shouldCreateNewConfig() {
            SaveAiRuntimeConfigRequest request = new SaveAiRuntimeConfigRequest(
                null, "TEST_KEY", "chat", "global",
                "third-party", "gpt-5.2", "qwen-plus",
                true, 10, "测试配置");

            AiRuntimeConfigEntity entity = AiRuntimeConfigEntity.builder()
                .id(1L).configKey("TEST_KEY").domain("chat").scene("global")
                .providerId("third-party").modelName("gpt-5.2").fallbackModelName("qwen-plus")
                .enabled(true).priority(10).configVersion(1L)
                .remark("测试配置").updatedBy("tester")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
            given(configRepository.save(any(AiRuntimeConfigEntity.class))).willReturn(entity);

            Long id = commandService.save(request, "tester");

            assertThat(id).isEqualTo(1L);
            ArgumentCaptor<AiRuntimeConfigEntity> captor = ArgumentCaptor.forClass(AiRuntimeConfigEntity.class);
            verify(configRepository).save(captor.capture());
            AiRuntimeConfigEntity saved = captor.getValue();
            assertThat(saved.getConfigKey()).isEqualTo("TEST_KEY");
            assertThat(saved.getConfigVersion()).isEqualTo(1L);
            verify(auditRepository).save(any());
        }

        @Test
        @DisplayName("更新配置递增版本号并失效缓存")
        void shouldUpdateConfigAndIncrementVersion() {
            SaveAiRuntimeConfigRequest request = new SaveAiRuntimeConfigRequest(
                1L, "THIRD_PARTY_MODEL", "chat", "global",
                "third-party", "gpt-5.2", "qwen-plus",
                true, 10, null);

            AiRuntimeConfigEntity existing = AiRuntimeConfigEntity.builder()
                .id(1L).configKey("THIRD_PARTY_MODEL").domain("chat").scene("global")
                .providerId("third-party").modelName("gpt-4").fallbackModelName("qwen-plus")
                .enabled(true).priority(10).configVersion(3L)
                .updatedBy("system").build();
            given(configRepository.findById(1L)).willReturn(Optional.of(existing));
            given(configRepository.save(any())).willReturn(existing);

            commandService.save(request, "tester");

            assertThat(existing.getConfigVersion()).isEqualTo(4L);
            assertThat(existing.getModelName()).isEqualTo("gpt-5.2");
            verify(cacheInvalidationNotifier).evictAndBroadcast(any());
            verify(llmProviderRegistry).evictChatClient(any(), eq("default"));
            verify(llmProviderRegistry).evictChatClient(any(), eq("plain"));
        }
    }

    @Nested
    @DisplayName("启用/禁用")
    class ToggleEnabled {

        @Test
        @DisplayName("禁用配置后递增版本号并失效缓存")
        void shouldDisableConfig() {
            AiRuntimeConfigEntity entity = AiRuntimeConfigEntity.builder()
                .id(1L).configKey("THIRD_PARTY_MODEL").domain("chat").scene("global")
                .providerId("third-party").modelName("gpt-5.2").fallbackModelName("qwen-plus")
                .enabled(true).priority(10).configVersion(2L).build();
            given(configRepository.findById(1L)).willReturn(Optional.of(entity));
            given(configRepository.save(any())).willReturn(entity);

            commandService.changeEnabled(1L, false, "tester");

            assertThat(entity.getEnabled()).isFalse();
            assertThat(entity.getConfigVersion()).isEqualTo(3L);
            verify(cacheInvalidationNotifier).evictAndBroadcast(any());
        }

        @Test
        @DisplayName("配置不存在时抛异常")
        void shouldThrowWhenNotFound() {
            given(configRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.changeEnabled(999L, false, "tester"))
                .hasMessageContaining("不存在");
        }
    }

    @Nested
    @DisplayName("刷新缓存")
    class RefreshCache {

        @Test
        @DisplayName("清空所有缓存并返回最新版本号")
        void shouldRefreshAllCaches() {
            given(configRepository.findAll()).willReturn(List.of(
                AiRuntimeConfigEntity.builder().id(1L).configVersion(5L).build()
            ));

            RefreshAiRuntimeConfigResponse response = commandService.refreshCache("tester");

            assertThat(response.latestConfigVersion()).isEqualTo(5L);
            verify(cacheInvalidationNotifier).refreshAllAndBroadcast();
            verify(auditRepository).save(any());
        }
    }
}
