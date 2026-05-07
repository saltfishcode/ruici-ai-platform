package com.ruici.ai.modules.knowledgebase.listener;

import com.ruici.ai.common.ai.EmbeddingProviderRegistry;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.model.AiRuntimeConfigSource;
import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;
import com.ruici.ai.common.constant.AsyncTaskStreamConstants;
import com.ruici.ai.infrastructure.redis.RedisService;
import com.ruici.ai.modules.knowledgebase.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("向量化任务生产者测试")
class VectorizeStreamProducerTest {

    @Mock
    private RedisService redisService;

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private EmbeddingProviderRegistry embeddingProviderRegistry;

    @Test
    @DisplayName("发送向量化任务时只解析一次 embedding 快照并写入消息体")
    void shouldResolveEmbeddingSnapshotOnceAndWriteItToStreamMessage() {
        VectorizeStreamProducer producer = new VectorizeStreamProducer(
            redisService,
            knowledgeBaseRepository,
            embeddingProviderRegistry
        );
        AiRuntimeConfigSnapshot embeddingSnapshot = new AiRuntimeConfigSnapshot(
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
        ArgumentCaptor<Map<String, String>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        given(embeddingProviderRegistry.resolveEmbeddingSnapshot(AiRuntimeScene.KNOWLEDGEBASE))
            .willReturn(embeddingSnapshot);
        given(redisService.streamAdd(
            eq(AsyncTaskStreamConstants.KB_VECTORIZE_STREAM_KEY),
            messageCaptor.capture(),
            eq(AsyncTaskStreamConstants.STREAM_MAX_LEN)
        )).willReturn("1-0");

        producer.sendVectorizeTask(10L, "知识库内容");

        verify(embeddingProviderRegistry).resolveEmbeddingSnapshot(AiRuntimeScene.KNOWLEDGEBASE);
        assertThat(messageCaptor.getValue())
            .containsEntry(AsyncTaskStreamConstants.FIELD_KB_ID, "10")
            .containsEntry(AsyncTaskStreamConstants.FIELD_CONTENT, "知识库内容")
            .containsEntry(AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0")
            .containsEntry(AsyncTaskStreamConstants.FIELD_EMBEDDING_PROVIDER_ID, "dashscope")
            .containsEntry(AsyncTaskStreamConstants.FIELD_EMBEDDING_MODEL_NAME, "text-embedding-v3")
            .containsEntry(AsyncTaskStreamConstants.FIELD_EMBEDDING_CONFIG_VERSION, "7")
            .containsEntry(AsyncTaskStreamConstants.FIELD_EMBEDDING_CONFIG_SOURCE, "DB_RUNTIME_CONFIG")
            .containsEntry(AsyncTaskStreamConstants.FIELD_EMBEDDING_STALE, "false");
    }
}
