package com.ruici.ai.common.config.runtime.service;

/**
 * AI 运行时配置查询服务。
 * <p>提供配置列表查询（支持 domain/scene/providerId 组合过滤）、详情查询、最新版本号查询。
 * 前端模型选择页面通过 {@link com.ruici.ai.common.config.runtime.controller.AiRuntimeConfigController}
 * 间接调用此服务。</p>
 */

import com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigEntity;
import com.ruici.ai.common.config.runtime.repository.AiRuntimeConfigRepository;
import com.ruici.ai.common.config.runtime.dto.AiRuntimeConfigDetailDTO;
import com.ruici.ai.common.config.runtime.dto.AiRuntimeConfigListItemDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiRuntimeConfigQueryService {

    private final AiRuntimeConfigRepository configRepository;

    public AiRuntimeConfigQueryService(AiRuntimeConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public List<AiRuntimeConfigListItemDTO> list(String domain, String scene, String providerId) {
        List<AiRuntimeConfigEntity> entities = configRepository.findAll();
        if (StringUtils.hasText(domain)) {
            entities = entities.stream()
                .filter(e -> domain.equalsIgnoreCase(e.getDomain()))
                .toList();
        }
        if (StringUtils.hasText(scene)) {
            entities = entities.stream()
                .filter(e -> scene.equalsIgnoreCase(e.getScene()))
                .toList();
        }
        if (StringUtils.hasText(providerId)) {
            entities = entities.stream()
                .filter(e -> providerId.equals(e.getProviderId()))
                .toList();
        }
        return entities.stream()
            .map(AiRuntimeConfigListItemDTO::fromEntity)
            .toList();
    }

    public AiRuntimeConfigDetailDTO getDetail(Long id) {
        return configRepository.findById(id)
            .map(AiRuntimeConfigDetailDTO::fromEntity)
            .orElseThrow(() -> new com.ruici.ai.common.exception.BusinessException(
                com.ruici.ai.common.exception.ErrorCode.NOT_FOUND,
                "AI 运行时配置不存在: " + id));
    }

    public long getLatestVersion() {
        return configRepository.findAll().stream()
            .mapToLong(e -> e.getConfigVersion() != null ? e.getConfigVersion() : 0L)
            .max()
            .orElse(0L);
    }
}
