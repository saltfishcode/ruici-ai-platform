package com.ruici.ai.common.config.runtime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AI 运行时配置审计仓库。
 */
@Repository
public interface AiRuntimeConfigAuditRepository extends JpaRepository<AiRuntimeConfigAuditEntity, Long> {
}
