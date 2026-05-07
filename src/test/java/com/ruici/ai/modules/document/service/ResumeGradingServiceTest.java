package com.ruici.ai.modules.document.service;

import com.ruici.ai.common.ai.LlmProviderRegistry;
import com.ruici.ai.common.ai.StructuredOutputInvoker;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.model.AiRuntimeConfigSource;
import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;
import com.ruici.ai.modules.document.model.AnalysisDifficulty;
import com.ruici.ai.modules.document.model.DocumentAnalysisResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("文档分析运行时快照测试")
class ResumeGradingServiceTest {

    @Mock
    private LlmProviderRegistry llmProviderRegistry;

    @Mock
    private StructuredOutputInvoker structuredOutputInvoker;

    @Mock
    private ChatClient chatClient;

    @Test
    @DisplayName("分析时按 DOCUMENT 场景解析运行时快照")
    void shouldResolveDocumentRuntimeSnapshotBeforeInvokingStructuredOutput() throws Exception {
        ResumeAnalysisProperties properties = new ResumeAnalysisProperties();
        ResumeGradingService service = new ResumeGradingService(
            llmProviderRegistry,
            structuredOutputInvoker,
            properties,
            new DefaultResourceLoader()
        );
        AiRuntimeConfigSnapshot runtimeSnapshot = new AiRuntimeConfigSnapshot(
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
        String resumeText = "张三\n五年 Java 后端开发经验，负责高并发系统重构与稳定性治理。";

        given(llmProviderRegistry.resolveChatSnapshot(
            eq(null),
            eq(null),
            eq(null),
            eq(AiRuntimeScene.DOCUMENT),
            eq(LlmProviderRegistry.buildSnapshotKey(AiRuntimeScene.DOCUMENT, "default", "THIRD_PARTY_MODEL")),
            eq("default"),
            eq(false)
        )).willReturn(runtimeSnapshot);
        given(llmProviderRegistry.getChatClient(runtimeSnapshot)).willReturn(chatClient);
        given(structuredOutputInvoker.invoke(
            eq(chatClient),
            any(String.class),
            any(String.class),
            any(),
            eq(com.ruici.ai.common.exception.ErrorCode.RESUME_ANALYSIS_FAILED),
            eq("简历分析失败："),
            eq("简历分析"),
            any()
        )).willThrow(new RuntimeException("LLM temporary unavailable"));

        DocumentAnalysisResponse response = service.analyzeResume(
            resumeText,
            "java-backend",
            AnalysisDifficulty.NORMAL
        );

        assertThat(response.overallScore()).isZero();
        assertThat(response.summary()).contains("LLM temporary unavailable");
        verify(llmProviderRegistry).resolveChatSnapshot(
            null,
            null,
            null,
            AiRuntimeScene.DOCUMENT,
            LlmProviderRegistry.buildSnapshotKey(AiRuntimeScene.DOCUMENT, "default", "THIRD_PARTY_MODEL"),
            "default",
            false
        );
        verify(llmProviderRegistry).getChatClient(runtimeSnapshot);
        verify(llmProviderRegistry, never()).getPlainChatClient(anyString());
        verify(llmProviderRegistry, never()).getVoiceChatClient(any(AiRuntimeConfigSnapshot.class));
    }
}
