package com.ruici.ai.common.config.runtime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI 运行时配置查询仓库。
 */
@Repository
public interface AiRuntimeConfigRepository extends JpaRepository<AiRuntimeConfigEntity, Long> {

    Optional<AiRuntimeConfigEntity> findFirstByDomainAndSceneAndEnabledOrderByPriorityAsc(
        String domain,
        String scene,
        Boolean enabled
    );

    Optional<AiRuntimeConfigEntity> findFirstByConfigKeyAndDomainAndSceneAndEnabledOrderByPriorityAsc(
        String configKey,
        String domain,
        String scene,
        Boolean enabled
    );

    List<AiRuntimeConfigEntity> findByDomainAndSceneAndEnabledOrderByPriorityAsc(
        String domain,
        String scene,
        Boolean enabled
    );
}
