package com.ruici.ai.common.config.runtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 运行时配置审计实体。
 */
@Entity
@Table(name = "ai_runtime_config_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRuntimeConfigAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_id")
    private Long configId;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "config_key", length = 64)
    private String configKey;

    @Column(name = "domain", length = 32)
    private String domain;

    @Column(name = "scene", length = 32)
    private String scene;

    @Column(name = "before_summary", columnDefinition = "TEXT")
    private String beforeSummary;

    @Column(name = "after_summary", columnDefinition = "TEXT")
    private String afterSummary;

    @Column(name = "operator", length = 64)
    private String operator;

    @Column(name = "operated_at", nullable = false)
    private LocalDateTime operatedAt;

    @Column(name = "remark", length = 500)
    private String remark;

    @PrePersist
    protected void onCreate() {
        this.operatedAt = LocalDateTime.now();
    }
}
