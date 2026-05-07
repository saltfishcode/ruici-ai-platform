package com.ruici.ai.common.config.runtime.dto;

/**
 * AI 运行时配置详情 DTO。
 * <p>对应 {@link com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigEntity} 的完整视图，
 * 包含 created_at/updated_at 等时间字段。</p>
 */

import com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigEntity;
import java.time.LocalDateTime;

public record AiRuntimeConfigDetailDTO(
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
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static AiRuntimeConfigDetailDTO fromEntity(AiRuntimeConfigEntity entity) {
        return new AiRuntimeConfigDetailDTO(
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
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
