package com.ruici.ai.modules.voice.service;

import com.ruici.ai.common.ai.LlmProviderRegistry;
import com.ruici.ai.common.config.runtime.model.AiRuntimeConfigSource;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.modules.document.repository.ResumeRepository;
import com.ruici.ai.modules.voice.config.VoiceInterviewProperties;
import com.ruici.ai.modules.voice.model.VoiceInterviewSessionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("语音实时对话运行时快照测试")
class DashscopeLlmServiceTest {

    @Mock
    private LlmProviderRegistry llmProviderRegistry;

    @Mock
    private VoiceInterviewPromptService promptService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private VoiceInterviewProperties properties;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Test
    @DisplayName("实时对话时会优先使用会话固化的 LLM 快照")
    void shouldUsePersistedVoiceSnapshotForRealtimeChat() {
        VoiceInterviewSessionEntity session = VoiceInterviewSessionEntity.builder()
            .id(11L)
            .skillId("java-backend")
            .llmProvider("dashscope")
            .llmModelName("qwen-plus-realtime")
            .llmFallbackModelName("qwen-flash")
            .llmConfigVersion(9L)
            .llmConfigSource(AiRuntimeConfigSource.DB_RUNTIME_CONFIG.name())
            .llmConfigStale(false)
            .build();

        given(promptService.generateSystemPromptWithContext("java-backend", null)).willReturn("system");
        given(llmProviderRegistry.getVoiceChatClient(session.toLlmRuntimeSnapshot())).willReturn(chatClient);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getResult().getOutput().getText())
            .willReturn("这是实时回答。");

        DashscopeLlmService service = new DashscopeLlmService(
            llmProviderRegistry,
            promptService,
            resumeRepository,
            properties
        );

        String result = service.chat("你好", session, List.of());

        assertThat(result).isEqualTo("这是实时回答。");
        verify(llmProviderRegistry).getVoiceChatClient(session.toLlmRuntimeSnapshot());
        verify(llmProviderRegistry, never()).getChatClient(any(com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot.class));
    }
}
