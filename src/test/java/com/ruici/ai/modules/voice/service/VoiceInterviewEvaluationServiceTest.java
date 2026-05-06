package com.ruici.ai.modules.voice.service;

import com.ruici.ai.common.ai.LlmProviderRegistry;
import com.ruici.ai.common.config.runtime.AiRuntimeConfigSource;
import com.ruici.ai.common.evaluation.EvaluationReport;
import com.ruici.ai.common.evaluation.UnifiedEvaluationService;
import com.ruici.ai.modules.simulation.skill.InterviewSkillService;
import com.ruici.ai.modules.voice.model.VoiceInterviewMessageEntity;
import com.ruici.ai.modules.voice.model.VoiceInterviewSessionEntity;
import com.ruici.ai.modules.voice.repository.VoiceInterviewEvaluationRepository;
import com.ruici.ai.modules.voice.repository.VoiceInterviewMessageRepository;
import com.ruici.ai.modules.voice.repository.VoiceInterviewSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("语音评估运行时快照测试")
class VoiceInterviewEvaluationServiceTest {

    @Mock
    private UnifiedEvaluationService unifiedEvaluationService;

    @Mock
    private LlmProviderRegistry llmProviderRegistry;

    @Mock
    private VoiceInterviewEvaluationRepository evaluationRepository;

    @Mock
    private VoiceInterviewMessageRepository messageRepository;

    @Mock
    private VoiceInterviewSessionRepository sessionRepository;

    @Mock
    private InterviewSkillService skillService;

    @Mock
    private ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("生成评估时会使用会话固化的 LLM 快照")
    void shouldUsePersistedVoiceSnapshotForEvaluation() {
        VoiceInterviewSessionEntity session = VoiceInterviewSessionEntity.builder()
            .id(10L)
            .roleType("java-backend")
            .skillId("java-backend")
            .llmProvider("dashscope")
            .llmModelName("qwen-plus-realtime")
            .llmFallbackModelName("qwen-flash")
            .llmConfigVersion(6L)
            .llmConfigSource(AiRuntimeConfigSource.DB_RUNTIME_CONFIG.name())
            .llmConfigStale(false)
            .startTime(LocalDateTime.now())
            .build();
        VoiceInterviewMessageEntity message = VoiceInterviewMessageEntity.builder()
            .sessionId(10L)
            .aiGeneratedText("请介绍一下项目")
            .userRecognizedText("我负责订单模块")
            .sequenceNum(1)
            .build();
        EvaluationReport report = new EvaluationReport(
            "10",
            1,
            85,
            List.of(),
            List.of(),
            "整体不错",
            List.of(),
            List.of(),
            List.of()
        );

        given(sessionRepository.findById(10L)).willReturn(Optional.of(session));
        given(messageRepository.findBySessionIdOrderBySequenceNumAsc(10L)).willReturn(List.of(message));
        given(llmProviderRegistry.getChatClient(session.toLlmRuntimeSnapshot())).willReturn(chatClient);
        given(skillService.buildEvaluationReferenceSectionSafe("java-backend")).willReturn("参考基线");
        given(unifiedEvaluationService.evaluate(eq(chatClient), eq("10"), any(), eq(null), eq("参考基线")))
            .willReturn(report);

        VoiceInterviewEvaluationService service = new VoiceInterviewEvaluationService(
            unifiedEvaluationService,
            llmProviderRegistry,
            evaluationRepository,
            messageRepository,
            sessionRepository,
            objectMapper,
            skillService
        );

        service.generateEvaluation(10L);

        verify(llmProviderRegistry).getChatClient(session.toLlmRuntimeSnapshot());
    }
}
