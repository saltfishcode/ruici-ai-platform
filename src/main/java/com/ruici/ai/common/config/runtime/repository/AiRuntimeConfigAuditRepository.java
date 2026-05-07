package com.ruici.ai.common.config.runtime.repository;

import com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AI 运行时配置审计仓库。
 * <p>用于保存配置变更审计记录，由 {@link com.ruici.ai.common.config.runtime.service.AiRuntimeConfigCommandService} 调用。</p>
 */
public interface AiRuntimeConfigAuditRepository extends JpaRepository<AiRuntimeConfigAuditEntity, Long> {
}
