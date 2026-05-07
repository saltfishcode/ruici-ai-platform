package com.ruici.ai.common.config.runtime.entity;

/**
 * AI 运行时配置 JPA 实体。
 * <p>对应数据库表 {@code ai_runtime_config}，存储非敏感的运行时控制信息。
 * 包括 domain/scene/provider/model/fallback/priority/version 等。
 * 不保存任何 API Key 或认证凭据。</p>
 *
 * <p>前端通过 {@link com.ruici.ai.common.config.runtime.controller.AiRuntimeConfigController}
 * 管理此表数据，解析器通过 {@link com.ruici.ai.common.config.runtime.repository.AiRuntimeConfigRepository}
 * 读取生效配置。</p>
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_runtime_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRuntimeConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, length = 128)
    private String configKey;

    @Column(name = "domain", length = 32)
    private String domain;

    @Column(name = "scene", length = 32)
    private String scene;

    @Column(name = "provider_id", length = 64)
    private String providerId;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(name = "fallback_model_name", length = 128)
    private String fallbackModelName;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "config_version")
    private Long configVersion;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
