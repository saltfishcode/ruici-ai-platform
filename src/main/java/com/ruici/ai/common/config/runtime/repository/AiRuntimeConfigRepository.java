package com.ruici.ai.common.config.runtime.repository;

import com.ruici.ai.common.config.runtime.entity.AiRuntimeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * AI 运行时配置数据访问仓库。
 * <p>提供按 configKey + domain + scene + enabled 的有序查询能力，
 * 支持场景精确匹配后退回到 GLOBAL 场景的兜底逻辑（在 Service 层组合）。</p>
 */
public interface AiRuntimeConfigRepository extends JpaRepository<AiRuntimeConfigEntity, Long> {
    Optional<AiRuntimeConfigEntity> findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
        String configKey, String domain, String scene, Boolean enabled);
}
