package com.ruici.ai.common.config.runtime;

import com.ruici.ai.common.config.LlmProviderProperties;
import com.ruici.ai.common.config.LlmProviderProperties.ProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * chat-first 阶段的运行时解析器。
 *
 * <p>实现顺序固定为：请求覆盖 -> 本地快照 -> DB -> 静态配置 -> 代码默认，并在 DB 失败时回退到
 * last-known-good，避免高热聊天链路在控制面短时故障时直接失败。</p>
 */
@Service
@Slf4j
public class DefaultAiRuntimeConfigResolver implements AiRuntimeConfigResolver {

    private static final String DEFAULT_CHAT_CONFIG_KEY = "THIRD_PARTY_MODEL";
    private static final String DEFAULT_EMBEDDING_CONFIG_KEY = "AI_EMBEDDING_MODEL";
    private static final String DEFAULT_PROVIDER_ID = "third-party";
    private static final String DEFAULT_MODEL_NAME = "gpt-5.2";
    private static final String DEFAULT_FALLBACK_MODEL = "qwen-plus";
    private static final String DEFAULT_EMBEDDING_PROVIDER_ID = "dashscope";
    private static final String DEFAULT_EMBEDDING_MODEL_NAME = "text-embedding-v3";
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
        String fallbackModelName = StringUtils.hasText(context.requestFallbackModelName())
            ? context.requestFallbackModelName().trim()
            : resolveStaticFallbackModelName(context, providerConfig);
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
        return snapshot;
    }

    private AiRuntimeConfigSnapshot loadSnapshotFromDatabase(AiRuntimeResolveContext context) {
        AiRuntimeConfigEntity entity = findRuntimeConfig(context);
        if (entity == null) {
            AiRuntimeConfigSnapshot staticSnapshot = buildStaticSnapshot(context, AiRuntimeConfigSource.ENV_CONFIG, false);
            policyService.validateResolvedSnapshot(staticSnapshot, context.clientType());
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
        if (StringUtils.hasText(providerConfig.getModel())) {
            return providerConfig.getModel().trim();
        }
        return DEFAULT_MODEL_NAME;
    }

    private String resolveStaticFallbackModelName(AiRuntimeResolveContext context, ProviderConfig providerConfig) {
        if (StringUtils.hasText(context.staticFallbackModelName())) {
            return context.staticFallbackModelName().trim();
        }
        if (context.domain() == AiRuntimeDomain.EMBEDDING) {
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
        return DEFAULT_CHAT_CONFIG_KEY;
    }

    private boolean isRequestOverridePresent(AiRuntimeResolveContext context) {
        return context.requestOverrideAllowed()
            && (StringUtils.hasText(context.requestProviderId())
            || StringUtils.hasText(context.requestModelName())
            || StringUtils.hasText(context.requestFallbackModelName()));
    }
}
