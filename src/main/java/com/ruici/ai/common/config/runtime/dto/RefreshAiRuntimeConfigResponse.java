package com.ruici.ai.common.config.runtime.dto;

/**
 * 刷新运行时缓存后的响应。
 * <p>latestConfigVersion 可用于前端判断是否需要重新拉取配置列表。</p>
 */

public record RefreshAiRuntimeConfigResponse(
    String message,
    Long latestConfigVersion
) {
}
