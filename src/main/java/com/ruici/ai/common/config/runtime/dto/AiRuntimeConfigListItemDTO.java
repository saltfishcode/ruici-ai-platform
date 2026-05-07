package com.ruici.ai.common.config.runtime.dto;

/**
 * AI 运行时配置列表项 DTO。
 * <p>对应 {@link com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigEntity} 的列表视图，
 * 不包含 created_at 等细节字段。</p>
 */

import com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigEntity;
import java.time.LocalDateTime;

public record AiRuntimeConfigListItemDTO(
    Long id,
    String configKey,
    String domain,
    String scene,
    String providerId,
    String modelName,
    String fallbackModelName,
    Boolean enabled,
    Integer priority,
    Long configVersion,
    String remark,
    String updatedBy,
    LocalDateTime updatedAt
) {

    public static AiRuntimeConfigListItemDTO fromEntity(AiRuntimeConfigEntity entity) {
        return new AiRuntimeConfigListItemDTO(
            entity.getId(),
            entity.getConfigKey(),
            entity.getDomain(),
            entity.getScene(),
            entity.getProviderId(),
            entity.getModelName(),
            entity.getFallbackModelName(),
            entity.getEnabled(),
            entity.getPriority(),
            entity.getConfigVersion(),
            entity.getRemark(),
            entity.getUpdatedBy(),
            entity.getUpdatedAt()
        );
    }
}
