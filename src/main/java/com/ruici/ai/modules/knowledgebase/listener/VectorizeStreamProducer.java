package com.ruici.ai.modules.knowledgebase.listener;

import com.ruici.ai.common.ai.EmbeddingProviderRegistry;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.async.AbstractStreamProducer;
import com.ruici.ai.common.constant.AsyncTaskStreamConstants;
import com.ruici.ai.infrastructure.redis.RedisService;
import com.ruici.ai.modules.knowledgebase.model.VectorStatus;
import com.ruici.ai.modules.knowledgebase.repository.KnowledgeBaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 向量化任务生产者
 * 负责发送向量化任务到 Redis Stream
 */
@Slf4j
@Component
public class VectorizeStreamProducer extends AbstractStreamProducer<VectorizeStreamProducer.VectorizeTaskPayload> {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;

    record VectorizeTaskPayload(Long kbId, String content, AiRuntimeConfigSnapshot embeddingSnapshot) {}

    public VectorizeStreamProducer(
        RedisService redisService,
        KnowledgeBaseRepository knowledgeBaseRepository,
        EmbeddingProviderRegistry embeddingProviderRegistry
    ) {
        super(redisService);
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.embeddingProviderRegistry = embeddingProviderRegistry;
    }

    /**
     * 发送向量化任务到 Redis Stream
     *
     * @param kbId    知识库ID
     * @param content 文档内容
     */
    public void sendVectorizeTask(Long kbId, String content) {
        AiRuntimeConfigSnapshot embeddingSnapshot = embeddingProviderRegistry.resolveEmbeddingSnapshot(
            AiRuntimeScene.KNOWLEDGEBASE
        );
        sendTask(new VectorizeTaskPayload(kbId, content, embeddingSnapshot));
    }

    @Override
    protected String taskDisplayName() {
        return "向量化";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.KB_VECTORIZE_STREAM_KEY;
    }

    @Override
    protected Map<String, String> buildMessage(VectorizeTaskPayload payload) {
        AiRuntimeConfigSnapshot embeddingSnapshot = payload.embeddingSnapshot();
        return Map.of(
            AsyncTaskStreamConstants.FIELD_KB_ID, payload.kbId().toString(),
            AsyncTaskStreamConstants.FIELD_CONTENT, payload.content(),
            AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0",
            AsyncTaskStreamConstants.FIELD_EMBEDDING_PROVIDER_ID, embeddingSnapshot.providerId(),
            AsyncTaskStreamConstants.FIELD_EMBEDDING_MODEL_NAME, embeddingSnapshot.modelName(),
            AsyncTaskStreamConstants.FIELD_EMBEDDING_CONFIG_VERSION, String.valueOf(embeddingSnapshot.configVersion()),
            AsyncTaskStreamConstants.FIELD_EMBEDDING_CONFIG_SOURCE, embeddingSnapshot.source().name(),
            AsyncTaskStreamConstants.FIELD_EMBEDDING_STALE, String.valueOf(embeddingSnapshot.stale())
        );
    }

    @Override
    protected String payloadIdentifier(VectorizeTaskPayload payload) {
        return "kbId=" + payload.kbId();
    }

    @Override
    protected void onSendFailed(VectorizeTaskPayload payload, String error) {
        updateVectorStatus(payload.kbId(), VectorStatus.FAILED, truncateError(error));
    }

    /**
     * 更新向量化状态
     */
    private void updateVectorStatus(Long kbId, VectorStatus status, String error) {
        knowledgeBaseRepository.findById(kbId).ifPresent(kb -> {
            kb.setVectorStatus(status);
            if (error != null) {
                kb.setVectorError(error.length() > 500 ? error.substring(0, 500) : error);
            }
            knowledgeBaseRepository.save(kb);
        });
    }
}
