package com.ruici.ai.modules.knowledgebase.service;

import com.ruici.ai.common.ai.EmbeddingProviderRegistry;
import com.ruici.ai.common.ai.LlmProviderRegistry;
import com.ruici.ai.common.ai.OpenAiCompatibleGatewayClient;
import com.ruici.ai.common.config.runtime.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.AiRuntimeConfigSource;
import com.ruici.ai.common.config.runtime.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.AiRuntimeScene;
import com.ruici.ai.modules.knowledgebase.model.QueryRequest;
import com.ruici.ai.modules.knowledgebase.model.QueryResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("知识库查询运行时快照测试")
class KnowledgeBaseQueryServiceTest {

    @Mock
    private LlmProviderRegistry llmProviderRegistry;

    @Mock
    private EmbeddingProviderRegistry embeddingProviderRegistry;

    @Mock
    private OpenAiCompatibleGatewayClient gatewayClient;

    @Mock
    private KnowledgeBaseVectorService vectorService;

    @Mock
    private KnowledgeBaseListService listService;

    @Mock
    private KnowledgeBaseCountService countService;

    private KnowledgeBaseQueryService createService() throws Exception {
        KnowledgeBaseQueryProperties properties = new KnowledgeBaseQueryProperties();
        properties.setLlmProvider("third-party");
        return new KnowledgeBaseQueryService(
            llmProviderRegistry,
            embeddingProviderRegistry,
            gatewayClient,
            vectorService,
            listService,
            countService,
            properties,
            new DefaultResourceLoader()
        );
    }

    private AiRuntimeConfigSnapshot runtimeSnapshot() {
        return new AiRuntimeConfigSnapshot(
            "THIRD_PARTY_MODEL",
            AiRuntimeDomain.CHAT,
            AiRuntimeScene.KNOWLEDGEBASE,
            "third-party",
            "gpt-5.2",
            "qwen-plus",
            3L,
            AiRuntimeConfigSource.DB_RUNTIME_CONFIG,
            false
        );
    }

    private AiRuntimeConfigSnapshot embeddingSnapshot() {
        return new AiRuntimeConfigSnapshot(
            "AI_EMBEDDING_MODEL",
            AiRuntimeDomain.EMBEDDING,
            AiRuntimeScene.KNOWLEDGEBASE,
            "dashscope",
            "text-embedding-v3",
            null,
            7L,
            AiRuntimeConfigSource.DB_RUNTIME_CONFIG,
            false
        );
    }

    private void stubRuntimeSnapshot(AiRuntimeConfigSnapshot runtimeSnapshot) {
        given(llmProviderRegistry.resolveChatSnapshot(
            eq("third-party"),
            eq(null),
            eq(null),
            eq(AiRuntimeScene.KNOWLEDGEBASE),
            eq(LlmProviderRegistry.buildSnapshotKey(AiRuntimeScene.KNOWLEDGEBASE, "default", "THIRD_PARTY_MODEL")),
            eq("default"),
            eq(false)
        )).willReturn(runtimeSnapshot);
    }

    private void stubEmbeddingSnapshot(AiRuntimeConfigSnapshot runtimeSnapshot) {
        given(embeddingProviderRegistry.resolveEmbeddingSnapshot(AiRuntimeScene.KNOWLEDGEBASE))
            .willReturn(runtimeSnapshot);
    }

    @Test
    @DisplayName("未选择知识库时通用问答也按 KNOWLEDGEBASE 场景解析运行时快照")
    void shouldResolveKnowledgeBaseRuntimeSnapshotForGeneralAnswer() throws Exception {
        KnowledgeBaseQueryService service = createService();
        AiRuntimeConfigSnapshot runtimeSnapshot = runtimeSnapshot();

        stubRuntimeSnapshot(runtimeSnapshot);
        given(gatewayClient.supports("third-party")).willReturn(true);
        given(gatewayClient.generateText(eq(runtimeSnapshot), anyString(), anyString())).willReturn("这是通用回答");

        QueryResponse response = service.queryKnowledgeBase(new QueryRequest(List.of(), "请帮我总结这份材料"));

        assertThat(response.answer()).isEqualTo("这是通用回答");
        assertThat(response.knowledgeBaseId()).isNull();
        assertThat(response.knowledgeBaseName()).isEmpty();
        verify(llmProviderRegistry).resolveChatSnapshot(
            "third-party",
            null,
            null,
            AiRuntimeScene.KNOWLEDGEBASE,
            LlmProviderRegistry.buildSnapshotKey(AiRuntimeScene.KNOWLEDGEBASE, "default", "THIRD_PARTY_MODEL"),
            "default",
            false
        );
        verify(gatewayClient).generateText(eq(runtimeSnapshot), anyString(), anyString());
        verifyNoInteractions(vectorService, countService, listService);
    }

    @Nested
    @DisplayName("流式问答")
    class StreamingAnswer {

        @Test
        @DisplayName("未选择知识库时会把历史上下文拼进通用流式请求")
        void shouldIncludeFormattedHistoryInGeneralStreamGatewayInput() throws Exception {
            KnowledgeBaseQueryService service = createService();
            AiRuntimeConfigSnapshot runtimeSnapshot = runtimeSnapshot();
            List<Message> history = List.of(
                new UserMessage("第一轮用户提问"),
                new AssistantMessage("第一轮助手回答")
            );

            stubRuntimeSnapshot(runtimeSnapshot);
            given(gatewayClient.supports("third-party")).willReturn(true);
            given(gatewayClient.streamText(eq(runtimeSnapshot), anyString(), anyString()))
                .willReturn(Flux.just("第一段", "第二段"));

            List<String> chunks = service.answerQuestionStream(List.of(), "请继续总结", history)
                .collectList()
                .block();

            assertThat(chunks).containsExactly("第一段", "第二段");
            ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
            verify(gatewayClient).streamText(eq(runtimeSnapshot), anyString(), inputCaptor.capture());
            assertThat(inputCaptor.getValue())
                .contains("用户: 第一轮用户提问")
                .contains("助手: 第一轮助手回答")
                .contains("当前请求:")
                .contains("请继续总结");
            verifyNoInteractions(vectorService, countService, listService);
        }

        @Test
        @DisplayName("知识库命中但流式内容呈现无结果话术时统一归一化为固定提示")
        void shouldNormalizeNoResultLikeGatewayStreamToFixedResponse() throws Exception {
            KnowledgeBaseQueryService service = createService();
            AiRuntimeConfigSnapshot runtimeSnapshot = runtimeSnapshot();
            AiRuntimeConfigSnapshot embeddingSnapshot = embeddingSnapshot();

            stubRuntimeSnapshot(runtimeSnapshot);
            stubEmbeddingSnapshot(embeddingSnapshot);
            given(gatewayClient.supports("third-party")).willReturn(true);
            given(vectorService.similaritySearch(anyString(), eq(List.of(10L)), anyInt(), anyDouble(), any()))
                .willReturn(List.of(new Document("命中的知识片段")));
            given(gatewayClient.streamText(eq(runtimeSnapshot), anyString(), anyString()))
                .willReturn(Flux.just("没有找到相关信息", "，请换一个问题"));

            List<String> chunks = service.answerQuestionStream(List.of(10L), "岗位要求是什么？")
                .collectList()
                .block();

            assertThat(chunks).containsExactly("抱歉，在选定的知识库中未检索到相关信息。请换一个更具体的关键词或补充上下文后再试。");
            verify(countService).updateQuestionCounts(List.of(10L));
            verify(vectorService).similaritySearch("岗位要求是什么？", List.of(10L), 12, 0.28, embeddingSnapshot);
            verify(gatewayClient).streamText(eq(runtimeSnapshot), anyString(), anyString());
        }
    }
}
