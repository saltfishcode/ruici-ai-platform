package com.ruici.ai.common.config.runtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 运行时配置实体。
 *
 * <p>该实体只映射 provider/model/作用域等非敏感控制信息，严禁存储真实密钥或凭据。</p>
 */
@Entity
@Table(name = "ai_runtime_config", indexes = {
    @Index(name = "idx_ai_runtime_config_domain_scene_enabled", columnList = "domain,scene,enabled"),
    @Index(name = "idx_ai_runtime_config_key_enabled_priority", columnList = "config_key,enabled,priority"),
    @Index(name = "idx_ai_runtime_config_provider_domain_scene", columnList = "provider_id,domain,scene")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRuntimeConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, length = 64)
    private String configKey;

    @Column(name = "domain", nullable = false, length = 32)
    private String domain;

    @Column(name = "scene", nullable = false, length = 32)
    private String scene;

    @Column(name = "provider_id", length = 64)
    private String providerId;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(name = "fallback_model_name", length = 128)
    private String fallbackModelName;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "config_version", nullable = false)
    @Builder.Default
    private Long configVersion = 1L;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
