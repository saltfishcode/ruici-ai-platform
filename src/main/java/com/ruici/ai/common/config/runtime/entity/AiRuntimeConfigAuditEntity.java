package com.ruici.ai.common.config.runtime.entity;

/**
 * AI 运行时配置审计 JPA 实体。
 * <p>对应数据库表 {@code ai_runtime_config_audit}，记录每次配置变更的操作者、变更类型和前后值摘要。
 * 由 {@link com.ruici.ai.common.config.runtime.service.AiRuntimeConfigCommandService} 在保存/启停/刷新时同步写入。</p>
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

    @Column(name = "action_type", length = 32)
    private String actionType;

    @Column(name = "before_summary", length = 1024)
    private String beforeSummary;

    @Column(name = "after_summary", length = 1024)
    private String afterSummary;

    @Column(name = "operator", length = 64)
    private String operator;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
