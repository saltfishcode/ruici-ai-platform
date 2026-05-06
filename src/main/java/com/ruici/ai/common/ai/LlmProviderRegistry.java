package com.ruici.ai.common.ai;

import com.ruici.ai.common.config.runtime.AiRuntimeConfigResolver;
import com.ruici.ai.common.config.runtime.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.AiRuntimeResolveContext;
import com.ruici.ai.common.config.runtime.AiRuntimeScene;
import com.ruici.ai.common.config.LlmProviderProperties;
import com.ruici.ai.common.config.LlmProviderProperties.AdvisorConfig;
import com.ruici.ai.common.config.LlmProviderProperties.ProviderConfig;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing and caching LLM providers.
 * Supports dynamic creation of ChatClient based on provider configurations.
 */
@Component
@Slf4j
public class LlmProviderRegistry {

    private static final String DEFAULT_CLIENT_TYPE = "default";
    private static final String CHAT_CONFIG_KEY = "THIRD_PARTY_MODEL";

    private final LlmProviderProperties properties;
    private final Map<String, ChatClient> clientCache = new ConcurrentHashMap<>();
    private final AiRuntimeConfigResolver aiRuntimeConfigResolver;

    private final ToolCallingManager toolCallingManager;
    private final ObservationRegistry observationRegistry;
    private final ToolCallback interviewSkillsToolCallback;

    public LlmProviderRegistry(
            LlmProviderProperties properties,
            AiRuntimeConfigResolver aiRuntimeConfigResolver,
            @Autowired(required = false) ToolCallingManager toolCallingManager,
            @Autowired(required = false) ObservationRegistry observationRegistry,
            @Autowired(required = false) @Qualifier("interviewSkillsToolCallback") ToolCallback interviewSkillsToolCallback) {
        this.properties = properties;
        this.aiRuntimeConfigResolver = aiRuntimeConfigResolver;
        this.toolCallingManager = toolCallingManager;
        this.observationRegistry = observationRegistry;
        this.interviewSkillsToolCallback = interviewSkillsToolCallback;
    }

    /**
     * Get a ChatClient for the specified provider ID.
     * If the client is not in the cache, it will be created based on the provider's configuration.
     *
     * @param providerId The ID of the provider (e.g., "dashscope", "lmstudio")
     * @return A ChatClient instance
     * @throws IllegalArgumentException if the providerId is unknown
     */
    public ChatClient getChatClient(String providerId) {
        log.info("[LlmProviderRegistry] Requesting client for provider: {}", providerId);
        return getChatClient(resolveChatSnapshot(
            providerId,
            null,
            null,
            AiRuntimeScene.GLOBAL,
            buildSnapshotKey(AiRuntimeScene.GLOBAL, DEFAULT_CLIENT_TYPE, CHAT_CONFIG_KEY),
            DEFAULT_CLIENT_TYPE,
            false
        ));
    }

    /**
     * Get the default ChatClient based on app.ai.default-provider.
     *
     * @return The default ChatClient instance
     */
    public ChatClient getDefaultChatClient() {
        return getChatClient((String) null);
    }

    /**
     * Get a ChatClient for the specified provider, falling back to the default if null or blank.
     */
    public ChatClient getChatClientOrDefault(String providerId) {
        return getChatClient(providerId);
    }

    /**
     * 获取不带 SkillsTool 的 ChatClient，用于简历题生成等不需要 Agent 工具调用的场景。
     */
    public ChatClient getPlainChatClient(String providerId) {
        AiRuntimeConfigSnapshot snapshot = resolveChatSnapshot(
            providerId,
            null,
            null,
            AiRuntimeScene.GLOBAL,
            buildSnapshotKey(AiRuntimeScene.GLOBAL, "plain", CHAT_CONFIG_KEY),
            "plain",
            false
        );
        return getPlainChatClientBySnapshot(snapshot);
    }

    /**
     * 获取语音交互专用 ChatClient。
     *
     * <p>语音链路对首包时延和稳定性更敏感，这里显式禁用 SkillsTool / ToolCallAdvisor，
     * 避免 OpenAI-compatible provider 在流式回复中触发工具调用导致链路中断。</p>
     */
    public ChatClient getVoiceChatClient(String providerId) {
        AiRuntimeConfigSnapshot snapshot = resolveChatSnapshot(
            providerId,
            null,
            null,
            AiRuntimeScene.VOICE,
            buildSnapshotKey(AiRuntimeScene.VOICE, "voice", CHAT_CONFIG_KEY),
            "voice",
            true
        );
        return getVoiceChatClient(snapshot);
    }

    public ChatClient getVoiceChatClient(AiRuntimeConfigSnapshot snapshot) {
        return clientCache.computeIfAbsent(buildCacheKey(snapshot, "voice"), key -> {
            log.info("[LlmProviderRegistry] Cache miss. Creating voice chat client for key: {}", key);
            return createVoiceChatClient(snapshot);
        });
    }

    public ChatClient getChatClient(AiRuntimeConfigSnapshot snapshot) {
        return clientCache.computeIfAbsent(buildCacheKey(snapshot, DEFAULT_CLIENT_TYPE), key -> {
            log.info("[LlmProviderRegistry] Cache miss. Creating chat client for key: {}", key);
            return createChatClient(snapshot);
        });
    }

    private ChatClient getPlainChatClientBySnapshot(AiRuntimeConfigSnapshot snapshot) {
        return clientCache.computeIfAbsent(buildCacheKey(snapshot, "plain"), key -> {
            log.info("[LlmProviderRegistry] Cache miss. Creating plain chat client for key: {}", key);
            return createPlainChatClient(snapshot);
        });
    }

    public AiRuntimeConfigSnapshot resolveChatSnapshot(
        String requestProviderId,
        String requestModelName,
        String requestFallbackModelName,
        AiRuntimeScene scene,
        String snapshotKey,
        String clientType,
        boolean requestOverrideAllowed
    ) {
        return aiRuntimeConfigResolver.resolveChatConfig(new AiRuntimeResolveContext(
            AiRuntimeDomain.CHAT,
            scene,
            CHAT_CONFIG_KEY,
            requestProviderId,
            requestModelName,
            requestFallbackModelName,
            properties.getDefaultProvider(),
            null,
            null,
            snapshotKey,
            clientType,
            requestOverrideAllowed
        ));
    }

    public void evictChatClient(AiRuntimeConfigSnapshot snapshot, String clientType) {
        if (snapshot == null) {
            return;
        }
        clientCache.remove(buildCacheKey(snapshot, clientType));
    }

    public static String buildSnapshotKey(AiRuntimeScene scene, String clientType, String configKey) {
        return scene.code() + ":" + clientType + ":" + configKey;
    }

    private ChatClient createChatClient(AiRuntimeConfigSnapshot snapshot) {
        OpenAiChatModel chatModel = buildChatModel(snapshot);

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (interviewSkillsToolCallback != null) {
            builder.defaultToolCallbacks(interviewSkillsToolCallback);
        }
        List<Advisor> advisors = buildDefaultAdvisors(snapshot.providerId());
        if (!advisors.isEmpty()) {
            builder.defaultAdvisors(advisors.toArray(new Advisor[0]));
            log.info("[LlmProviderRegistry] Applied {} advisors for provider {}", advisors.size(), snapshot.providerId());
        }

        return builder.build();
    }

    private ChatClient createPlainChatClient(AiRuntimeConfigSnapshot snapshot) {
        OpenAiChatModel chatModel = buildChatModel(snapshot);
        log.info("[LlmProviderRegistry] Created plain ChatClient (no tools) for {}", snapshot.providerId());
        return ChatClient.builder(chatModel).build();
    }

    private ChatClient createVoiceChatClient(AiRuntimeConfigSnapshot snapshot) {
        OpenAiChatModel chatModel = buildChatModel(snapshot);
        log.info("[LlmProviderRegistry] Created voice ChatClient (plain/no tools) for {}", snapshot.providerId());
        return ChatClient.builder(chatModel).build();
    }

    private OpenAiChatModel buildChatModel(AiRuntimeConfigSnapshot snapshot) {
        ProviderConfig config = properties.getProviders().get(snapshot.providerId());
        if (config == null) {
            log.error("[LlmProviderRegistry] Provider config not found: {}", snapshot.providerId());
            throw new IllegalArgumentException("Unknown LLM provider: " + snapshot.providerId());
        }

        String normalizedBaseUrl = normalizeBaseUrlForOpenAiApi(config.getBaseUrl());

        log.info("[LlmProviderRegistry] Building ChatModel - Provider: {}, BaseUrl: {}, Model: {}",
                 snapshot.providerId(), normalizedBaseUrl, snapshot.modelName());

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(300000);

        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(requestFactory);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizedBaseUrl)
                .apiKey(config.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(snapshot.modelName())
                .temperature(0.2)
                .build();

        return new OpenAiChatModel(
                openAiApi,
                options,
                toolCallingManager,
                RetryUtils.DEFAULT_RETRY_TEMPLATE,
                observationRegistry != null ? observationRegistry : ObservationRegistry.NOOP
        );
    }

    private List<Advisor> buildDefaultAdvisors(String providerId) {
        AdvisorConfig config = properties.getAdvisors();
        if (config == null || !config.isEnabled()) {
            return List.of();
        }

        List<Advisor> advisors = new ArrayList<>();

        if (config.isToolCallEnabled()) {
            if (toolCallingManager != null) {
                advisors.add(buildToolCallAdvisor(
                    config.isToolCallConversationHistoryEnabled(),
                    config.isStreamToolCallResponses()));
            } else {
                log.warn("[LlmProviderRegistry] ToolCallAdvisor skipped: ToolCallingManager unavailable, provider={}", providerId);
            }
        }

        if (config.isMessageChatMemoryEnabled()) {
            int maxMessages = Math.max(20, config.getMessageChatMemoryMaxMessages());
            MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(
                MessageWindowChatMemory.builder()
                    .maxMessages(maxMessages)
                    .build()
            ).build();
            advisors.add(memoryAdvisor);
        }

        if (config.isSimpleLoggerEnabled()) {
            advisors.add(new SimpleLoggerAdvisor());
        }

        return advisors;
    }

    private ToolCallAdvisor buildToolCallAdvisor(boolean conversationHistoryEnabled,
                                                  boolean streamToolCallResponses) {
        return ToolCallAdvisor.builder()
            .toolCallingManager(toolCallingManager)
            .conversationHistoryEnabled(conversationHistoryEnabled)
            .streamToolCallResponses(streamToolCallResponses)
            .build();
    }

    private String resolveProviderId(String providerId) {
        return (providerId != null && !providerId.isBlank())
            ? providerId : properties.getDefaultProvider();
    }

    private String buildCacheKey(AiRuntimeConfigSnapshot snapshot, String clientType) {
        String baseUrl = properties.getProviders().get(snapshot.providerId()).getBaseUrl();
        String fallback = StringUtils.hasText(snapshot.fallbackModelName()) ? snapshot.fallbackModelName() : "-";
        return snapshot.providerId()
            + ":" + clientType
            + ":" + snapshot.modelName()
            + ":" + fallback
            + ":" + normalizeBaseUrlForOpenAiApi(baseUrl)
            + ":" + snapshot.configVersion();
    }

    static String normalizeBaseUrlForOpenAiApi(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Provider baseUrl must not be blank");
        }

        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.endsWith("/v1")) {
            return normalized.substring(0, normalized.length() - "/v1".length());
        }
        return normalized;
    }
}
