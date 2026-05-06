package com.ruici.ai.common.ai;

import com.ruici.ai.common.config.LlmProviderProperties;
import com.ruici.ai.common.config.LlmProviderProperties.ProviderConfig;
import com.ruici.ai.common.config.PgVectorStoreRuntimeProperties;
import com.ruici.ai.common.config.runtime.AiRuntimeConfigResolver;
import com.ruici.ai.common.config.runtime.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.AiRuntimeResolveContext;
import com.ruici.ai.common.config.runtime.AiRuntimeScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embedding provider 注册表。
 *
 * <p>Phase 6 只做批次级 / 查询级快照：向量化任务和检索请求在入口解析一次 embedding 快照，
 * 之后整个调用链都复用同一份 provider/model 配置，避免 chunk 级查库与模型漂移。</p>
 */
@Component
@Slf4j
public class EmbeddingProviderRegistry {

    private static final String EMBEDDING_CLIENT_TYPE = "embedding";
    private static final String EMBEDDING_CONFIG_KEY = "AI_EMBEDDING_MODEL";

    private final LlmProviderProperties llmProviderProperties;
    private final PgVectorStoreRuntimeProperties pgVectorStoreRuntimeProperties;
    private final AiRuntimeConfigResolver aiRuntimeConfigResolver;
    private final JdbcTemplate jdbcTemplate;
    private final Map<String, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();
    private final Map<String, VectorStore> vectorStoreCache = new ConcurrentHashMap<>();

    public EmbeddingProviderRegistry(
        LlmProviderProperties llmProviderProperties,
        PgVectorStoreRuntimeProperties pgVectorStoreRuntimeProperties,
        AiRuntimeConfigResolver aiRuntimeConfigResolver,
        JdbcTemplate jdbcTemplate
    ) {
        this.llmProviderProperties = llmProviderProperties;
        this.pgVectorStoreRuntimeProperties = pgVectorStoreRuntimeProperties;
        this.aiRuntimeConfigResolver = aiRuntimeConfigResolver;
        this.jdbcTemplate = jdbcTemplate;
    }

    public AiRuntimeConfigSnapshot resolveEmbeddingSnapshot(AiRuntimeScene scene) {
        return aiRuntimeConfigResolver.resolveEmbeddingConfig(new AiRuntimeResolveContext(
            AiRuntimeDomain.EMBEDDING,
            scene,
            EMBEDDING_CONFIG_KEY,
            null,
            null,
            null,
            llmProviderProperties.getDefaultEmbeddingProvider(),
            resolveStaticEmbeddingModel(),
            null,
            LlmProviderRegistry.buildSnapshotKey(scene, EMBEDDING_CLIENT_TYPE, EMBEDDING_CONFIG_KEY),
            EMBEDDING_CLIENT_TYPE,
            false
        ));
    }

    public VectorStore getVectorStore(AiRuntimeConfigSnapshot snapshot) {
        return vectorStoreCache.computeIfAbsent(buildCacheKey(snapshot), key -> {
            EmbeddingModel embeddingModel = getEmbeddingModel(snapshot);
            PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName(pgVectorStoreRuntimeProperties.getSchemaName())
                .vectorTableName(pgVectorStoreRuntimeProperties.getTableName())
                .vectorTableValidationsEnabled(pgVectorStoreRuntimeProperties.isSchemaValidation())
                .dimensions(pgVectorStoreRuntimeProperties.getDimensions())
                .distanceType(pgVectorStoreRuntimeProperties.getDistanceType())
                .indexType(pgVectorStoreRuntimeProperties.getIndexType())
                .removeExistingVectorStoreTable(pgVectorStoreRuntimeProperties.isRemoveExistingVectorStoreTable())
                .initializeSchema(pgVectorStoreRuntimeProperties.isInitializeSchema())
                .maxDocumentBatchSize(pgVectorStoreRuntimeProperties.getMaxDocumentBatchSize())
                .build();
            initializeVectorStore(vectorStore);
            log.info("[EmbeddingProviderRegistry] Created PgVectorStore for provider={}, model={}, version={}",
                snapshot.providerId(), snapshot.modelName(), snapshot.configVersion());
            return vectorStore;
        });
    }

    public void evictVectorStore(AiRuntimeConfigSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        String cacheKey = buildCacheKey(snapshot);
        vectorStoreCache.remove(cacheKey);
        embeddingModelCache.remove(cacheKey);
    }

    private EmbeddingModel getEmbeddingModel(AiRuntimeConfigSnapshot snapshot) {
        return embeddingModelCache.computeIfAbsent(buildCacheKey(snapshot), key -> {
            ProviderConfig providerConfig = resolveProviderConfig(snapshot.providerId());
            String normalizedBaseUrl = normalizeBaseUrlForOpenAiApi(providerConfig.getBaseUrl());
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(10000);
            requestFactory.setReadTimeout(300000);

            RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(requestFactory);

            OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizedBaseUrl)
                .apiKey(providerConfig.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();

            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(snapshot.modelName())
                .build();
            log.info("[EmbeddingProviderRegistry] Creating embedding model for provider={}, model={}",
                snapshot.providerId(), snapshot.modelName());
            return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                options,
                RetryUtils.DEFAULT_RETRY_TEMPLATE
            );
        });
    }

    private ProviderConfig resolveProviderConfig(String providerId) {
        if (llmProviderProperties.getProviders() == null) {
            throw new IllegalArgumentException("No embedding providers configured");
        }
        ProviderConfig providerConfig = llmProviderProperties.getProviders().get(providerId);
        if (providerConfig == null) {
            throw new IllegalArgumentException("Unknown embedding provider: " + providerId);
        }
        return providerConfig;
    }

    private String resolveStaticEmbeddingModel() {
        String defaultProvider = llmProviderProperties.getDefaultEmbeddingProvider();
        if (!StringUtils.hasText(defaultProvider) || llmProviderProperties.getProviders() == null) {
            return null;
        }
        ProviderConfig providerConfig = llmProviderProperties.getProviders().get(defaultProvider.trim());
        if (providerConfig == null) {
            return null;
        }
        return providerConfig.getEmbeddingModel();
    }

    private void initializeVectorStore(PgVectorStore vectorStore) {
        if (vectorStore instanceof InitializingBean initializingBean) {
            try {
                initializingBean.afterPropertiesSet();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize PgVectorStore", e);
            }
        }
    }

    private String buildCacheKey(AiRuntimeConfigSnapshot snapshot) {
        ProviderConfig providerConfig = resolveProviderConfig(snapshot.providerId());
        return String.join(":",
            EMBEDDING_CLIENT_TYPE,
            snapshot.providerId(),
            snapshot.modelName(),
            nullToEmpty(providerConfig.getBaseUrl()),
            String.valueOf(snapshot.configVersion())
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String normalizeBaseUrlForOpenAiApi(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        return baseUrl.trim().endsWith("/")
            ? baseUrl.trim().substring(0, baseUrl.trim().length() - 1)
            : baseUrl.trim();
    }
}
