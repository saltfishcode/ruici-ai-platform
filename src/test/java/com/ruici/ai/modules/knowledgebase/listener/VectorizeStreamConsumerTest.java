package com.ruici.ai.modules.knowledgebase.listener;

import com.ruici.ai.common.config.runtime.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.AiRuntimeConfigSource;
import com.ruici.ai.common.config.runtime.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.AiRuntimeScene;
import com.ruici.ai.common.constant.AsyncTaskStreamConstants;
import com.ruici.ai.infrastructure.redis.RedisService;
import com.ruici.ai.modules.knowledgebase.repository.KnowledgeBaseRepository;
import com.ruici.ai.modules.knowledgebase.service.KnowledgeBaseVectorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.stream.StreamMessageId;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("向量化任务消费者测试")
class VectorizeStreamConsumerTest {

    @Mock
    private RedisService redisService;

    @Mock
    private KnowledgeBaseVectorService vectorService;

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Test
    @DisplayName("消费消息时会还原 embedding 快照并原样传给向量化服务")
    void shouldParseEmbeddingSnapshotAndPassItToVectorService() {
        TestableVectorizeStreamConsumer consumer = new TestableVectorizeStreamConsumer(
            redisService,
            vectorService,
            knowledgeBaseRepository
        );
        Map<String, String> message = Map.of(
            AsyncTaskStreamConstants.FIELD_KB_ID, "10",
            AsyncTaskStreamConstants.FIELD_CONTENT, "知识库内容",
            AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_PROVIDER_ID, "dashscope",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_MODEL_NAME, "text-embedding-v3",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_CONFIG_VERSION, "7",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_CONFIG_SOURCE, "DB_RUNTIME_CONFIG",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_STALE, "false"
        );

        VectorizeStreamConsumer.VectorizePayload payload = consumer.parse(message);

        consumer.process(payload);

        verify(vectorService).vectorizeAndStore(
            10L,
            "知识库内容",
            new AiRuntimeConfigSnapshot(
                "AI_EMBEDDING_MODEL",
                AiRuntimeDomain.EMBEDDING,
                AiRuntimeScene.KNOWLEDGEBASE,
                "dashscope",
                "text-embedding-v3",
                null,
                7L,
                AiRuntimeConfigSource.DB_RUNTIME_CONFIG,
                false
            )
        );
    }

    @Test
    @DisplayName("重试入队时会保留同一份 embedding 快照字段")
    void shouldKeepSameEmbeddingSnapshotWhenRetryingMessage() {
        TestableVectorizeStreamConsumer consumer = new TestableVectorizeStreamConsumer(
            redisService,
            vectorService,
            knowledgeBaseRepository
        );
        VectorizeStreamConsumer.VectorizePayload payload = consumer.parse(Map.of(
            AsyncTaskStreamConstants.FIELD_KB_ID, "20",
            AsyncTaskStreamConstants.FIELD_CONTENT, "需要重试的内容",
            AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "1",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_PROVIDER_ID, "dashscope",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_MODEL_NAME, "text-embedding-v3",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_CONFIG_VERSION, "9",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_CONFIG_SOURCE, "LAST_KNOWN_GOOD",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_STALE, "true"
        ));
        ArgumentCaptor<Map<String, String>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        consumer.retry(payload, 2);

        verify(redisService).streamAdd(
            eq(AsyncTaskStreamConstants.KB_VECTORIZE_STREAM_KEY),
            messageCaptor.capture(),
            eq(AsyncTaskStreamConstants.STREAM_MAX_LEN)
        );
        assertThat(messageCaptor.getValue())
            .containsEntry(AsyncTaskStreamConstants.FIELD_KB_ID, "20")
            .containsEntry(AsyncTaskStreamConstants.FIELD_CONTENT, "需要重试的内容")
            .containsEntry(AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "2")
            .containsEntry(AsyncTaskStreamConstants.FIELD_EMBEDDING_PROVIDER_ID, "dashscope")
            .containsEntry(AsyncTaskStreamConstants.FIELD_EMBEDDING_MODEL_NAME, "text-embedding-v3")
            .containsEntry(AsyncTaskStreamConstants.FIELD_EMBEDDING_CONFIG_VERSION, "9")
            .containsEntry(AsyncTaskStreamConstants.FIELD_EMBEDDING_CONFIG_SOURCE, "LAST_KNOWN_GOOD")
            .containsEntry(AsyncTaskStreamConstants.FIELD_EMBEDDING_STALE, "true");
    }

    private static final class TestableVectorizeStreamConsumer extends VectorizeStreamConsumer {

        private TestableVectorizeStreamConsumer(
            RedisService redisService,
            KnowledgeBaseVectorService vectorService,
            KnowledgeBaseRepository knowledgeBaseRepository
        ) {
            super(redisService, vectorService, knowledgeBaseRepository);
        }

        private VectorizePayload parse(Map<String, String> message) {
            return super.parsePayload(new StreamMessageId(1, 0), message);
        }

        private void process(VectorizePayload payload) {
            super.processBusiness(payload);
        }

        private void retry(VectorizePayload payload, int retryCount) {
            super.retryMessage(payload, retryCount);
        }
    }
}
