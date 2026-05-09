package com.ruici.ai.common.config.runtime.resolver;

import com.ruici.ai.common.config.LlmProviderProperties;
import com.ruici.ai.common.config.LlmProviderProperties.ProviderConfig;
import com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigEntity;
import com.ruici.ai.common.config.runtime.model.AiRuntimeConfigSource;
import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;
import com.ruici.ai.common.config.runtime.policy.AiRuntimePolicyService;
import com.ruici.ai.common.config.runtime.repository.AiRuntimeConfigRepository;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeResolveContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class DefaultAiRuntimeConfigResolver implements AiRuntimeConfigResolver {

    private static final String DEFAULT_CHAT_CONFIG_KEY = "THIRD_PARTY_MODEL";
    private static final String DEFAULT_EMBEDDING_CONFIG_KEY = "AI_EMBEDDING_MODEL";
    private static final String DEFAULT_ASR_CONFIG_KEY = "AI_ASR_MODEL";
    private static final String DEFAULT_TTS_CONFIG_KEY = "AI_TTS_MODEL";
    private static final String DEFAULT_PROVIDER_ID = "third-party";
    private static final String DEFAULT_MODEL_NAME = "gpt-5.2";
    private static final String DEFAULT_FALLBACK_MODEL = "qwen-plus";
    private static final String DEFAULT_EMBEDDING_PROVIDER_ID = "dashscope";
    private static final String DEFAULT_EMBEDDING_MODEL_NAME = "text-embedding-v3";
    private static final String DEFAULT_VOICE_PROVIDER_ID = "dashscope";
    private static final String DEFAULT_ASR_MODEL_NAME = "qwen3-asr-flash-realtime";
    private static final String DEFAULT_TTS_MODEL_NAME = "qwen3-tts-flash-realtime";
    private static final long STATIC_CONFIG_VERSION = 0L;

    private final AiRuntimeConfigRepository configRepository;
    private final AiRuntimePolicyService policyService;
    private final LlmProviderProperties llmProviderProperties;
    private final Map<String, AiRuntimeConfigSnapshot> snapshotCache = new ConcurrentHashMap<>();
    private final Map<String, AiRuntimeConfigSnapshot> lastKnownGoodCache = new ConcurrentHashMap<>();

    public DefaultAiRuntimeConfigResolver(
        AiRuntimeConfigRepository configRepository,
        AiRuntimePolicyService policyService,
        LlmProviderProperties llmProviderProperties
    ) {
        this.configRepository = configRepository;
        this.policyService = policyService;
        this.llmProviderProperties = llmProviderProperties;
    }

    @Override
    public AiRuntimeConfigSnapshot resolveChatConfig(AiRuntimeResolveContext context) {
        return resolveConfig(context);
    }

    @Override
    public AiRuntimeConfigSnapshot refreshChatSnapshot(AiRuntimeResolveContext context) {
        return refreshSnapshot(context);
    }

    @Override
    public AiRuntimeConfigSnapshot resolveEmbeddingConfig(AiRuntimeResolveContext context) {
        return resolveConfig(context);
    }

    @Override
    public AiRuntimeConfigSnapshot refreshEmbeddingSnapshot(AiRuntimeResolveContext context) {
        return refreshSnapshot(context);
    }

    @Override
    public AiRuntimeConfigSnapshot resolveAsrConfig(AiRuntimeResolveContext context) {
        return resolveConfig(context);
    }

    @Override
    public AiRuntimeConfigSnapshot refreshAsrSnapshot(AiRuntimeResolveContext context) {
        return refreshSnapshot(context);
    }

    @Override
    public AiRuntimeConfigSnapshot resolveTtsConfig(AiRuntimeResolveContext context) {
        return resolveConfig(context);
    }

    @Override
    public AiRuntimeConfigSnapshot refreshTtsSnapshot(AiRuntimeResolveContext context) {
        return refreshSnapshot(context);
    }

    private AiRuntimeConfigSnapshot resolveConfig(AiRuntimeResolveContext context) {
        if (isRequestOverridePresent(context)) {
            return resolveRequestOverride(context);
        }
        if (!StringUtils.hasText(context.snapshotKey())) {
            return refreshSnapshot(context);
        }
        AiRuntimeConfigSnapshot cachedSnapshot = snapshotCache.get(context.snapshotKey());
        if (cachedSnapshot != null) {
            return cachedSnapshot;
        }
        return refreshSnapshot(context);
    }

    private AiRuntimeConfigSnapshot refreshSnapshot(AiRuntimeResolveContext context) {
        try {
            AiRuntimeConfigSnapshot snapshot = loadSnapshotFromDatabase(context);
            cacheSnapshot(context.snapshotKey(), snapshot);
            return snapshot;
        } catch (Exception exception) {
            AiRuntimeConfigSnapshot lastKnownGood = getLastKnownGood(context.snapshotKey());
            if (lastKnownGood != null) {
                log.warn("AI 运行时配置查询失败，回退到 last-known-good: snapshotKey={}, reason={}",
                    context.snapshotKey(), exception.getMessage());
                return lastKnownGood;
            }
            log.warn("AI 运行时配置查询失败，回退静态配置: snapshotKey={}, reason={}",
                context.snapshotKey(), exception.getMessage());
            AiRuntimeConfigSnapshot fallbackSnapshot = buildStaticSnapshot(context, AiRuntimeConfigSource.ENV_CONFIG, false);
            policyService.validateResolvedSnapshot(fallbackSnapshot, context.clientType());
            cacheSnapshot(context.snapshotKey(), fallbackSnapshot);
            return fallbackSnapshot;
        }
    }

    private AiRuntimeConfigSnapshot resolveRequestOverride(AiRuntimeResolveContext context) {
        policyService.validateRequestOverride(context);
        String providerId = StringUtils.hasText(context.requestProviderId())
            ? context.requestProviderId().trim()
            : resolveStaticProviderId(context);
        ProviderConfig providerConfig = resolveProviderConfig(providerId);
        String modelName = StringUtils.hasText(context.requestModelName())
            ? context.requestModelName().trim()
            : providerConfig.getModel();

        // 兜底模型：请求级覆盖 → 数据库运行时配置 → 静态/环境配置
        String fallbackModelName;
        if (StringUtils.hasText(context.requestFallbackModelName())) {
            fallbackModelName = context.requestFallbackModelName().trim();
        } else {
            fallbackModelName = resolveFallbackModelFromDbOrStatic(context, providerConfig);
        }

        AiRuntimeConfigSnapshot snapshot = new AiRuntimeConfigSnapshot(
            resolveConfigKey(context),
            context.domain(),
            context.scene(),
            providerId,
            modelName,
            fallbackModelName,
            STATIC_CONFIG_VERSION,
            AiRuntimeConfigSource.REQUEST_OVERRIDE,
            false
        );
        policyService.validateResolvedSnapshot(snapshot, context.clientType());
        logResolvedSnapshot(snapshot);
        return snapshot;
    }

    /**
     * 从数据库运行时配置中解析兜底模型名，若 DB 未命中则回退到静态配置。
     */
    private String resolveFallbackModelFromDbOrStatic(AiRuntimeResolveContext context, ProviderConfig providerConfig) {
        try {
            AiRuntimeConfigEntity entity = findRuntimeConfig(context);
            if (entity != null && StringUtils.hasText(entity.getFallbackModelName())) {
                log.debug("从数据库运行时配置获取 fallback model: entityId={}, fallback={}",
                    entity.getId(), entity.getFallbackModelName());
                return entity.getFallbackModelName().trim();
            }
        } catch (Exception e) {
            log.warn("查询数据库 fallback model 异常，使用静态配置: {}", e.getMessage());
        }
        return resolveStaticFallbackModelName(context, providerConfig);
    }

    private AiRuntimeConfigSnapshot loadSnapshotFromDatabase(AiRuntimeResolveContext context) {
        AiRuntimeConfigEntity entity = findRuntimeConfig(context);
        if (entity == null) {
            AiRuntimeConfigSnapshot staticSnapshot = buildStaticSnapshot(context, AiRuntimeConfigSource.ENV_CONFIG, false);
            policyService.validateResolvedSnapshot(staticSnapshot, context.clientType());
            logResolvedSnapshot(staticSnapshot);
            return staticSnapshot;
        }
        AiRuntimeConfigSnapshot snapshot = new AiRuntimeConfigSnapshot(
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
        policyService.validateResolvedSnapshot(snapshot, context.clientType());
        logResolvedSnapshot(snapshot);
        return snapshot;
    }

    private AiRuntimeConfigEntity findRuntimeConfig(AiRuntimeResolveContext context) {
        String configKey = resolveConfigKey(context);
        String domain = context.domain().code();
        String scene = context.scene().code();
        return configRepository
            .findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(configKey, domain, scene, Boolean.TRUE)
            .or(() -> context.scene() == AiRuntimeScene.GLOBAL
                ? java.util.Optional.empty()
                : configRepository.findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
                    configKey,
                    domain,
                    AiRuntimeScene.GLOBAL.code(),
                    Boolean.TRUE
                ))
            .orElse(null);
    }

    private void cacheSnapshot(String snapshotKey, AiRuntimeConfigSnapshot snapshot) {
        if (!StringUtils.hasText(snapshotKey) || snapshot == null) {
            return;
        }
        snapshotCache.put(snapshotKey, snapshot);
        lastKnownGoodCache.put(snapshotKey, toLastKnownGoodSnapshot(snapshot));
    }

    public void evictSnapshot(String snapshotKey) {
        if (!StringUtils.hasText(snapshotKey)) {
            return;
        }
        snapshotCache.remove(snapshotKey);
    }

    public void evictAllSnapshots() {
        snapshotCache.clear();
        log.info("[DefaultAiRuntimeConfigResolver] Snapshot cache cleared");
    }

    private AiRuntimeConfigSnapshot getLastKnownGood(String snapshotKey) {
        if (!StringUtils.hasText(snapshotKey)) {
            return null;
        }
        return lastKnownGoodCache.get(snapshotKey);
    }

    private AiRuntimeConfigSnapshot toLastKnownGoodSnapshot(AiRuntimeConfigSnapshot snapshot) {
        return new AiRuntimeConfigSnapshot(
            snapshot.configKey(),
            snapshot.domain(),
            snapshot.scene(),
            snapshot.providerId(),
            snapshot.modelName(),
            snapshot.fallbackModelName(),
            snapshot.configVersion(),
            AiRuntimeConfigSource.LAST_KNOWN_GOOD,
            true
        );
    }

    private void logResolvedSnapshot(AiRuntimeConfigSnapshot snapshot) {
        log.info("AI 运行时配置已解析: domain={}, scene={}, provider={}, model={}, fallback={}, source={}, v={}{}",
            snapshot.domain(), snapshot.scene(),
            snapshot.providerId(), snapshot.modelName(),
            snapshot.fallbackModelName(),
            snapshot.source(), snapshot.configVersion(),
            snapshot.stale() ? " (stale)" : "");
    }

    private AiRuntimeConfigSnapshot buildStaticSnapshot(
        AiRuntimeResolveContext context,
        AiRuntimeConfigSource source,
        boolean stale
    ) {
        String providerId = resolveStaticProviderId(context);
        ProviderConfig providerConfig = resolveProviderConfig(providerId);
        return new AiRuntimeConfigSnapshot(
            resolveConfigKey(context),
            context.domain(),
            context.scene(),
            providerId,
            resolveStaticModelName(context, providerConfig),
            resolveStaticFallbackModelName(context, providerConfig),
            STATIC_CONFIG_VERSION,
            source,
            stale
        );
    }

    private String resolveStaticProviderId(AiRuntimeResolveContext context) {
        if (StringUtils.hasText(context.staticProviderId())) {
            return context.staticProviderId().trim();
        }
        if (context.domain() == AiRuntimeDomain.EMBEDDING) {
            if (StringUtils.hasText(llmProviderProperties.getDefaultEmbeddingProvider())) {
                return llmProviderProperties.getDefaultEmbeddingProvider().trim();
            }
            return DEFAULT_EMBEDDING_PROVIDER_ID;
        }
        if (context.domain() == AiRuntimeDomain.ASR || context.domain() == AiRuntimeDomain.TTS) {
            return DEFAULT_VOICE_PROVIDER_ID;
        }
        if (StringUtils.hasText(llmProviderProperties.getDefaultProvider())) {
            return llmProviderProperties.getDefaultProvider().trim();
        }
        return DEFAULT_PROVIDER_ID;
    }

    private String resolveStaticModelName(AiRuntimeResolveContext context, ProviderConfig providerConfig) {
        if (StringUtils.hasText(context.staticModelName())) {
            return context.staticModelName().trim();
        }
        if (context.domain() == AiRuntimeDomain.EMBEDDING) {
            if (StringUtils.hasText(providerConfig.getEmbeddingModel())) {
                return providerConfig.getEmbeddingModel().trim();
            }
            return DEFAULT_EMBEDDING_MODEL_NAME;
        }
        if (context.domain() == AiRuntimeDomain.ASR) {
            if (StringUtils.hasText(providerConfig.getModel())) {
                return providerConfig.getModel().trim();
            }
            return DEFAULT_ASR_MODEL_NAME;
        }
        if (context.domain() == AiRuntimeDomain.TTS) {
            if (StringUtils.hasText(providerConfig.getModel())) {
                return providerConfig.getModel().trim();
            }
            return DEFAULT_TTS_MODEL_NAME;
        }
        if (StringUtils.hasText(providerConfig.getModel())) {
            return providerConfig.getModel().trim();
        }
        return DEFAULT_MODEL_NAME;
    }

    private String resolveStaticFallbackModelName(AiRuntimeResolveContext context, ProviderConfig providerConfig) {
        if (StringUtils.hasText(context.staticFallbackModelName())) {
            return context.staticFallbackModelName().trim();
        }
        if (context.domain() == AiRuntimeDomain.EMBEDDING
            || context.domain() == AiRuntimeDomain.ASR
            || context.domain() == AiRuntimeDomain.TTS) {
            return null;
        }
        if (StringUtils.hasText(providerConfig.getFallbackModel())) {
            return providerConfig.getFallbackModel().trim();
        }
        return DEFAULT_FALLBACK_MODEL;
    }

    private ProviderConfig resolveProviderConfig(String providerId) {
        if (llmProviderProperties.getProviders() != null) {
            ProviderConfig providerConfig = llmProviderProperties.getProviders().get(providerId);
            if (providerConfig != null) {
                return providerConfig;
            }
        }
        if (DEFAULT_PROVIDER_ID.equals(providerId)) {
            ProviderConfig defaultConfig = new ProviderConfig();
            defaultConfig.setModel(DEFAULT_MODEL_NAME);
            defaultConfig.setFallbackModel(DEFAULT_FALLBACK_MODEL);
            defaultConfig.setEmbeddingModel(DEFAULT_EMBEDDING_MODEL_NAME);
            defaultConfig.setBaseUrl("https://api-s.zwenooo.link/v1");
            defaultConfig.setApiKey("");
            return defaultConfig;
        }
        if (DEFAULT_EMBEDDING_PROVIDER_ID.equals(providerId)) {
            ProviderConfig defaultConfig = new ProviderConfig();
            defaultConfig.setModel(DEFAULT_ASR_MODEL_NAME);
            defaultConfig.setEmbeddingModel(DEFAULT_EMBEDDING_MODEL_NAME);
            defaultConfig.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode");
            defaultConfig.setApiKey("");
            return defaultConfig;
        }
        throw new IllegalArgumentException("Unknown LLM provider: " + providerId);
    }

    private String resolveConfigKey(AiRuntimeResolveContext context) {
        return StringUtils.hasText(context.configKey())
            ? context.configKey().trim()
            : defaultConfigKey(context.domain());
    }

    private String defaultConfigKey(AiRuntimeDomain domain) {
        if (domain == AiRuntimeDomain.EMBEDDING) {
            return DEFAULT_EMBEDDING_CONFIG_KEY;
        }
        if (domain == AiRuntimeDomain.ASR) {
            return DEFAULT_ASR_CONFIG_KEY;
        }
        if (domain == AiRuntimeDomain.TTS) {
            return DEFAULT_TTS_CONFIG_KEY;
        }
        return DEFAULT_CHAT_CONFIG_KEY;
    }

    private boolean isRequestOverridePresent(AiRuntimeResolveContext context) {
        return context.requestOverrideAllowed()
            && (StringUtils.hasText(context.requestProviderId())
            || StringUtils.hasText(context.requestModelName())
            || StringUtils.hasText(context.requestFallbackModelName()));
    }
}
