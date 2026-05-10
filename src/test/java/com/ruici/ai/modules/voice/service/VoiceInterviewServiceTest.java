package com.ruici.ai.modules.voice.service;

import com.ruici.ai.common.ai.LlmProviderRegistry;
import com.ruici.ai.common.config.runtime.resolver.AiRuntimeConfigResolver;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.model.AiRuntimeConfigSource;
import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;
import com.ruici.ai.modules.voice.config.VoiceInterviewProperties;
import com.ruici.ai.modules.voice.dto.CreateSessionRequest;
import com.ruici.ai.modules.voice.listener.VoiceEvaluateStreamProducer;
import com.ruici.ai.modules.voice.model.VoiceInterviewSessionEntity;
import com.ruici.ai.modules.voice.repository.VoiceInterviewEvaluationRepository;
import com.ruici.ai.modules.voice.repository.VoiceInterviewMessageRepository;
import com.ruici.ai.modules.voice.repository.VoiceInterviewSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("语音会话创建运行时快照测试")
class VoiceInterviewServiceTest {

    @Mock
    private VoiceInterviewSessionRepository sessionRepository;

    @Mock
    private VoiceInterviewMessageRepository messageRepository;

    @Mock
    private VoiceInterviewEvaluationRepository evaluationRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<VoiceInterviewSessionEntity> sessionBucket;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private VoiceInterviewProperties properties;

    @Mock
    private VoiceEvaluateStreamProducer voiceEvaluateStreamProducer;

    @Mock
    private LlmProviderRegistry llmProviderRegistry;

    @Mock
    private AiRuntimeConfigResolver aiRuntimeConfigResolver;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("创建会话时会固化 voice LLM/ASR/TTS 快照到会话实体")
    void shouldPersistVoiceRuntimeSnapshotWhenCreatingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AiRuntimeConfigSnapshot llmSnapshot = new AiRuntimeConfigSnapshot(
            "THIRD_PARTY_MODEL",
            AiRuntimeDomain.CHAT,
            AiRuntimeScene.VOICE,
            "dashscope",
            "qwen-plus-realtime",
            "qwen-flash",
            12L,
            AiRuntimeConfigSource.DB_RUNTIME_CONFIG,
            false
        );
        AiRuntimeConfigSnapshot asrSnapshot = new AiRuntimeConfigSnapshot(
            "AI_ASR_MODEL",
            AiRuntimeDomain.ASR,
            AiRuntimeScene.VOICE,
            "dashscope",
            "qwen3-asr-flash-realtime",
            null,
            5L,
            AiRuntimeConfigSource.DB_RUNTIME_CONFIG,
            false
        );
        AiRuntimeConfigSnapshot ttsSnapshot = new AiRuntimeConfigSnapshot(
            "AI_TTS_MODEL",
            AiRuntimeDomain.TTS,
            AiRuntimeScene.VOICE,
            "dashscope",
            "qwen3-tts-flash-realtime",
            null,
            6L,
            AiRuntimeConfigSource.DB_RUNTIME_CONFIG,
            false
        );
        CreateSessionRequest createRequest = CreateSessionRequest.builder()
            .skillId("java-backend")
            .llmProvider("dashscope")
            .introEnabled(true)
            .build();

        given(properties.getQwen().getAsr().getModel()).willReturn("qwen3-asr-flash-realtime");
        given(properties.getQwen().getTts().getModel()).willReturn("qwen3-tts-flash-realtime");

        given(llmProviderRegistry.resolveChatSnapshot(
            eq("dashscope"),
            eq(null),
            eq(null),
            eq(AiRuntimeScene.VOICE),
            eq(LlmProviderRegistry.buildSnapshotKey(AiRuntimeScene.VOICE, "voice", "THIRD_PARTY_MODEL")),
            eq("voice"),
            eq(true)
        )).willReturn(llmSnapshot);
        given(aiRuntimeConfigResolver.resolveAsrConfig(any())).willReturn(asrSnapshot);
        given(aiRuntimeConfigResolver.resolveTtsConfig(any())).willReturn(ttsSnapshot);
        given(sessionRepository.save(any(VoiceInterviewSessionEntity.class))).willAnswer(invocation -> {
            VoiceInterviewSessionEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        });
        doReturn(sessionBucket).when(redissonClient).getBucket("voice:session:100");

        VoiceInterviewService service = new VoiceInterviewService(
            sessionRepository,
            messageRepository,
            evaluationRepository,
            redissonClient,
            properties,
            voiceEvaluateStreamProducer,
            llmProviderRegistry,
            aiRuntimeConfigResolver
        );

        service.createSession(createRequest);
        verify(sessionRepository).save(org.mockito.ArgumentMatchers.argThat(session ->
            "dashscope".equals(session.getLlmProvider())
                && "qwen-plus-realtime".equals(session.getLlmModelName())
                && "qwen-flash".equals(session.getLlmFallbackModelName())
                && Long.valueOf(12L).equals(session.getLlmConfigVersion())
                && AiRuntimeConfigSource.DB_RUNTIME_CONFIG.name().equals(session.getLlmConfigSource())
                && Boolean.FALSE.equals(session.getLlmConfigStale())
                && "dashscope".equals(session.getAsrProvider())
                && "qwen3-asr-flash-realtime".equals(session.getAsrModelName())
                && Long.valueOf(5L).equals(session.getAsrConfigVersion())
                && AiRuntimeConfigSource.DB_RUNTIME_CONFIG.name().equals(session.getAsrConfigSource())
                && Boolean.FALSE.equals(session.getAsrConfigStale())
                && "dashscope".equals(session.getTtsProvider())
                && "qwen3-tts-flash-realtime".equals(session.getTtsModelName())
                && Long.valueOf(6L).equals(session.getTtsConfigVersion())
                && AiRuntimeConfigSource.DB_RUNTIME_CONFIG.name().equals(session.getTtsConfigSource())
                && Boolean.FALSE.equals(session.getTtsConfigStale())
        ));
    }
}
