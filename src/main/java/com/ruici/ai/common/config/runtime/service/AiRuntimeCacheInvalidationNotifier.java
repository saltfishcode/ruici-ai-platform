package com.ruici.ai.common.config.runtime.service;

import com.ruici.ai.common.ai.LlmProviderRegistry;
import com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigEntity;
import com.ruici.ai.common.config.runtime.model.AiRuntimeConfigSource;
import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;
import com.ruici.ai.common.config.runtime.resolver.DefaultAiRuntimeConfigResolver;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

/**
 * AI 运行时缓存失效通知器。
 *
 * <p>单实例模式下，本地清理 resolver / ChatClient 缓存已经足够；
 * 多实例部署时，还需要把失效事件广播给其他节点，否则它们会继续命中旧快照。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AiRuntimeCacheInvalidationNotifier {

    static final String TOPIC_NAME = "ai:runtime:cache:invalidation";
    private static final String REFRESH_ALL_PAYLOAD = "REFRESH_ALL";

    private final RedissonClient redissonClient;
    private final DefaultAiRuntimeConfigResolver defaultResolver;
    private final LlmProviderRegistry llmProviderRegistry;

    @PostConstruct
    void registerListener() {
        RTopic topic = redissonClient.getTopic(TOPIC_NAME);
        topic.addListener(String.class, (channel, payload) -> {
            if (!StringUtils.hasText(payload)) {
                return;
            }
            if (REFRESH_ALL_PAYLOAD.equals(payload)) {
                evictAllLocal();
                return;
            }
            evictLocal(payload);
        });
    }

    public void evictAndBroadcast(AiRuntimeConfigEntity entity) {
        String payload = buildSnapshotKey(entity);
        evictLocal(payload);
        redissonClient.getTopic(TOPIC_NAME).publish(payload);
    }

    public void refreshAllAndBroadcast() {
        evictAllLocal();
        redissonClient.getTopic(TOPIC_NAME).publish(REFRESH_ALL_PAYLOAD);
    }

    private void evictAllLocal() {
        defaultResolver.evictAllSnapshots();
        llmProviderRegistry.evictAllChatClients();
        log.info("AI 运行时缓存已执行全量本地失效");
    }

    private void evictLocal(String snapshotKey) {
        if (!StringUtils.hasText(snapshotKey)) {
            return;
        }
        defaultResolver.evictSnapshot(snapshotKey);
        // 远程节点收到单 key 失效时，无法从 snapshotKey 反推完整的 ChatClient 缓存 key
        // （需要 providerId + model + fallback + baseUrl + configVersion），
        // 因此采用全量清理策略。ChatClient 创建成本低（仅包装 ChatModel），over-evict 可接受。
        llmProviderRegistry.evictAllChatClients();
        log.info("AI 运行时缓存已失效: snapshotKey={}, ChatClient 缓存已全量清理", snapshotKey);
    }

    static String buildSnapshotKey(AiRuntimeConfigEntity entity) {
        AiRuntimeScene scene;
        try {
            scene = AiRuntimeScene.fromCode(entity.getScene());
        } catch (IllegalArgumentException e) {
            scene = AiRuntimeScene.GLOBAL;
        }

        String clientType = switch (AiRuntimeDomain.fromCode(entity.getDomain())) {
            case CHAT -> "default";
            case EMBEDDING -> "default";
            case ASR -> "voice-asr";
            case TTS -> "voice-tts";
        };
        return LlmProviderRegistry.buildSnapshotKey(scene, clientType, entity.getConfigKey());
    }

    static AiRuntimeConfigSnapshot toSnapshot(AiRuntimeConfigEntity entity) {
        return new AiRuntimeConfigSnapshot(
            entity.getConfigKey(),
            AiRuntimeDomain.fromCode(entity.getDomain()),
            AiRuntimeScene.fromCode(entity.getScene()),
            entity.getProviderId(),
            entity.getModelName(),
            entity.getFallbackModelName(),
            entity.getConfigVersion(),
            AiRuntimeConfigSource.DB_RUNTIME_CONFIG,
            false
        );
    }
}
