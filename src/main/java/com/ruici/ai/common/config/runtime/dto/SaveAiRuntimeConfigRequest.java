package com.ruici.ai.common.config.runtime.dto;

/**
 * 保存或更新 AI 运行时配置的请求体。
 * <p>domain/scene 使用字符串传入，由 {@link com.ruici.ai.common.config.runtime.policy.AiRuntimeConfigValidationService} 校验合法性。
 * id=null 表示新增，id!=null 表示更新。</p>
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveAiRuntimeConfigRequest(
    Long id,

    @NotBlank(message = "配置键不能为空")
    String configKey,

    @NotBlank(message = "能力域不能为空")
    String domain,

    @NotBlank(message = "业务场景不能为空")
    String scene,

    String providerId,

    @NotBlank(message = "模型名不能为空")
    String modelName,

    String fallbackModelName,

    @NotNull(message = "启用状态不能为空")
    Boolean enabled,

    Integer priority,

    String remark
) {

    public static final int DEFAULT_PRIORITY = 100;

    public int normalizedPriority() {
        return priority != null ? priority : DEFAULT_PRIORITY;
    }
}
