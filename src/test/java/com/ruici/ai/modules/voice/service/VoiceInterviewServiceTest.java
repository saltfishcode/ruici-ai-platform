package com.ruici.ai.modules.voice.service;

import com.ruici.ai.common.ai.LlmProviderRegistry;
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

    @Mock
    private VoiceInterviewProperties properties;

    @Mock
    private VoiceEvaluateStreamProducer voiceEvaluateStreamProducer;

    @Mock
    private LlmProviderRegistry llmProviderRegistry;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("创建会话时会固化 voice LLM 快照到会话实体")
    void shouldPersistVoiceRuntimeSnapshotWhenCreatingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AiRuntimeConfigSnapshot snapshot = new AiRuntimeConfigSnapshot(
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
        CreateSessionRequest createRequest = CreateSessionRequest.builder()
            .skillId("java-backend")
            .introEnabled(true)
            .build();

        given(llmProviderRegistry.resolveChatSnapshot(
            eq("dashscope"),
            eq(null),
            eq(null),
            eq(AiRuntimeScene.VOICE),
            eq(LlmProviderRegistry.buildSnapshotKey(AiRuntimeScene.VOICE, "voice", "THIRD_PARTY_MODEL")),
            eq("voice"),
            eq(true)
        )).willReturn(snapshot);
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
            llmProviderRegistry
        );

        service.createSession(createRequest);
        org.mockito.Mockito.verify(sessionRepository).save(org.mockito.ArgumentMatchers.argThat(session ->
            "dashscope".equals(session.getLlmProvider())
                && "qwen-plus-realtime".equals(session.getLlmModelName())
                && "qwen-flash".equals(session.getLlmFallbackModelName())
                && Long.valueOf(12L).equals(session.getLlmConfigVersion())
                && AiRuntimeConfigSource.DB_RUNTIME_CONFIG.name().equals(session.getLlmConfigSource())
                && Boolean.FALSE.equals(session.getLlmConfigStale())
        ));
    }
}
