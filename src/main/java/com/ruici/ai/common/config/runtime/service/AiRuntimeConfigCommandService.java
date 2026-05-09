package com.ruici.ai.common.config.runtime.service;

/**
 * AI 运行时配置命令服务。
 * <p>提供配置的增改、启停、缓存刷新能力。每次保存自动递增 configVersion 以触发下游缓存失效，
 * 同步写入审计记录。修改配置后调用 {@code evictCacheForEntity} 局部失效受影响 key。</p>
 */

import com.ruici.ai.common.ai.LlmProviderRegistry;
import com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigAuditEntity;
import com.ruici.ai.common.config.runtime.repository.AiRuntimeConfigAuditRepository;
import com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigEntity;
import com.ruici.ai.common.config.runtime.repository.AiRuntimeConfigRepository;
import com.ruici.ai.common.config.runtime.resolver.AiRuntimeConfigResolver;
import com.ruici.ai.common.config.runtime.snapshot.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.policy.AiRuntimeConfigValidationService;
import com.ruici.ai.common.config.runtime.model.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.model.AiRuntimeScene;
import com.ruici.ai.common.config.runtime.resolver.DefaultAiRuntimeConfigResolver;
import com.ruici.ai.common.config.runtime.dto.RefreshAiRuntimeConfigResponse;
import com.ruici.ai.common.config.runtime.dto.SaveAiRuntimeConfigRequest;
import com.ruici.ai.common.exception.BusinessException;
import com.ruici.ai.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiRuntimeConfigCommandService {

    private static final Logger log = LoggerFactory.getLogger(AiRuntimeConfigCommandService.class);

    private final AiRuntimeConfigRepository configRepository;
    private final AiRuntimeConfigAuditRepository auditRepository;
    private final AiRuntimeConfigValidationService validationService;
    private final AiRuntimeConfigResolver configResolver;
    private final LlmProviderRegistry llmProviderRegistry;
    private final DefaultAiRuntimeConfigResolver defaultResolver;
    private final AiRuntimeCacheInvalidationNotifier cacheInvalidationNotifier;

    public AiRuntimeConfigCommandService(
        AiRuntimeConfigRepository configRepository,
        AiRuntimeConfigAuditRepository auditRepository,
        AiRuntimeConfigValidationService validationService,
        AiRuntimeConfigResolver configResolver,
        LlmProviderRegistry llmProviderRegistry,
        DefaultAiRuntimeConfigResolver defaultResolver,
        AiRuntimeCacheInvalidationNotifier cacheInvalidationNotifier
    ) {
        this.configRepository = configRepository;
        this.auditRepository = auditRepository;
        this.validationService = validationService;
        this.configResolver = configResolver;
        this.llmProviderRegistry = llmProviderRegistry;
        this.defaultResolver = defaultResolver;
        this.cacheInvalidationNotifier = cacheInvalidationNotifier;
    }

    @Transactional
    public Long save(SaveAiRuntimeConfigRequest request, String operator) {
        validationService.validateForSave(request);

        if (request.id() != null) {
            AiRuntimeConfigEntity existing = configRepository.findById(request.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                    "AI 运行时配置不存在: " + request.id()));

            String beforeSummary = summarize(existing);
            existing.setConfigKey(request.configKey().trim());
            existing.setDomain(request.domain().trim());
            existing.setScene(request.scene().trim());
            existing.setProviderId(StringUtils.hasText(request.providerId()) ? request.providerId().trim() : null);
            existing.setModelName(request.modelName().trim());
            existing.setFallbackModelName(
                StringUtils.hasText(request.fallbackModelName()) ? request.fallbackModelName().trim() : null);
            existing.setEnabled(request.enabled());
            existing.setPriority(request.normalizedPriority());
            existing.setConfigVersion(existing.getConfigVersion() + 1);
            existing.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : null);
            existing.setUpdatedBy(StringUtils.hasText(operator) ? operator : "api");
            AiRuntimeConfigEntity saved = configRepository.save(existing);

            writeAudit(saved.getId(), "UPDATE", beforeSummary, summarize(saved), operator);
            evictCacheForEntity(saved);
            log.info("AI 运行时配置已更新: id={}, configKey={}, domain={}, scene={}, newVersion={}",
                saved.getId(), saved.getConfigKey(), saved.getDomain(), saved.getScene(), saved.getConfigVersion());
            return saved.getId();
        }

        AiRuntimeConfigEntity entity = AiRuntimeConfigEntity.builder()
            .configKey(request.configKey().trim())
            .domain(request.domain().trim())
            .scene(request.scene().trim())
            .providerId(StringUtils.hasText(request.providerId()) ? request.providerId().trim() : null)
            .modelName(request.modelName().trim())
            .fallbackModelName(
                StringUtils.hasText(request.fallbackModelName()) ? request.fallbackModelName().trim() : null)
            .enabled(request.enabled())
            .priority(request.normalizedPriority())
            .configVersion(1L)
            .remark(StringUtils.hasText(request.remark()) ? request.remark().trim() : null)
            .updatedBy(StringUtils.hasText(operator) ? operator : "api")
            .build();
        AiRuntimeConfigEntity saved = configRepository.save(entity);

        writeAudit(saved.getId(), "CREATE", null, summarize(saved), operator);
        log.info("AI 运行时配置已新增: id={}, configKey={}, domain={}, scene={}",
            saved.getId(), saved.getConfigKey(), saved.getDomain(), saved.getScene());
        return saved.getId();
    }

    @Transactional
    public void changeEnabled(Long id, boolean enabled, String operator) {
        AiRuntimeConfigEntity entity = configRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                "AI 运行时配置不存在: " + id));
        String beforeSummary = summarize(entity);
        entity.setEnabled(enabled);
        entity.setConfigVersion(entity.getConfigVersion() + 1);
        entity.setUpdatedBy(StringUtils.hasText(operator) ? operator : "api");
        configRepository.save(entity);

        String actionType = enabled ? "ENABLE" : "DISABLE";
        writeAudit(entity.getId(), actionType, beforeSummary, summarize(entity), operator);
        evictCacheForEntity(entity);
        log.info("AI 运行时配置已{}: id={}, configKey={}", enabled ? "启用" : "禁用", id, entity.getConfigKey());
    }

    @Transactional
    public RefreshAiRuntimeConfigResponse refreshCache(String operator) {
        cacheInvalidationNotifier.refreshAllAndBroadcast();
        long latestVersion = configRepository.findAll().stream()
            .mapToLong(e -> e.getConfigVersion() != null ? e.getConfigVersion() : 0L)
            .max()
            .orElse(0L);
        writeAudit(null, "REFRESH", null, "cache evicted, latestVersion=" + latestVersion, operator);
        log.info("AI 运行时缓存已刷新，最新版本号: {}", latestVersion);
        return new RefreshAiRuntimeConfigResponse("缓存已刷新", latestVersion);
    }

    private void evictCacheForEntity(AiRuntimeConfigEntity entity) {
        AiRuntimeDomain domain;
        try {
            domain = AiRuntimeDomain.fromCode(entity.getDomain());
        } catch (IllegalArgumentException e) {
            return;
        }
        AiRuntimeScene scene;
        try {
            scene = AiRuntimeScene.fromCode(entity.getScene());
        } catch (IllegalArgumentException e) {
            scene = AiRuntimeScene.GLOBAL;
        }

        cacheInvalidationNotifier.evictAndBroadcast(entity);

        AiRuntimeConfigSnapshot snapshot = AiRuntimeCacheInvalidationNotifier.toSnapshot(entity);

        if (domain == AiRuntimeDomain.CHAT || domain == AiRuntimeDomain.EMBEDDING) {
            llmProviderRegistry.evictChatClient(snapshot, "default");
            if (domain == AiRuntimeDomain.CHAT) {
                llmProviderRegistry.evictChatClient(snapshot, "plain");
            }
        }
    }

    private void writeAudit(Long configId, String actionType, String beforeSummary, String afterSummary, String operator) {
        AiRuntimeConfigAuditEntity audit = AiRuntimeConfigAuditEntity.builder()
            .configId(configId)
            .actionType(actionType)
            .beforeSummary(beforeSummary)
            .afterSummary(afterSummary)
            .operator(StringUtils.hasText(operator) ? operator : "api")
            .build();
        auditRepository.save(audit);
    }

    private static String summarize(AiRuntimeConfigEntity entity) {
        return String.format("%s/%s/%s provider=%s model=%s fallback=%s enabled=%s priority=%d v=%d",
            entity.getConfigKey(), entity.getDomain(), entity.getScene(),
            entity.getProviderId(), entity.getModelName(), entity.getFallbackModelName(),
            entity.getEnabled(), entity.getPriority(),
            entity.getConfigVersion() != null ? entity.getConfigVersion() : 0L);
    }
}
