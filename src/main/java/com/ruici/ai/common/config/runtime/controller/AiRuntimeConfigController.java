package com.ruici.ai.common.config.runtime.controller;

/**
 * AI 运行时配置管理 REST 控制器。
 * <p>提供配置的增删改查、启停、缓存刷新接口，前端模型选择页面直接对接此控制器。
 * 不要求认证（开源学习项目，无权限体系）。
 * domain 可选值: chat / embedding / asr / tts
 * scene 可选值: global / simulation / knowledgebase / voice / document</p>
 */

import com.ruici.ai.common.config.runtime.dto.AiRuntimeConfigDetailDTO;
import com.ruici.ai.common.config.runtime.dto.AiRuntimeConfigListItemDTO;
import com.ruici.ai.common.config.runtime.dto.RefreshAiRuntimeConfigResponse;
import com.ruici.ai.common.config.runtime.dto.SaveAiRuntimeConfigRequest;
import com.ruici.ai.common.config.runtime.service.AiRuntimeConfigCommandService;
import com.ruici.ai.common.config.runtime.service.AiRuntimeConfigQueryService;
import com.ruici.ai.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-runtime-config")
public class AiRuntimeConfigController {

    private final AiRuntimeConfigQueryService queryService;
    private final AiRuntimeConfigCommandService commandService;

    public AiRuntimeConfigController(
        AiRuntimeConfigQueryService queryService,
        AiRuntimeConfigCommandService commandService
    ) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @GetMapping
    public Result<List<AiRuntimeConfigListItemDTO>> list(
        @RequestParam(required = false) String domain,
        @RequestParam(required = false) String scene,
        @RequestParam(required = false) String providerId
    ) {
        return Result.success(queryService.list(domain, scene, providerId));
    }

    @GetMapping("/{id}")
    public Result<AiRuntimeConfigDetailDTO> detail(@PathVariable Long id) {
        return Result.success(queryService.getDetail(id));
    }

    @GetMapping("/version")
    public Result<Long> version() {
        return Result.success(queryService.getLatestVersion());
    }

    @PostMapping
    public Result<Long> save(
        @Valid @RequestBody SaveAiRuntimeConfigRequest request,
        @RequestParam(defaultValue = "api") String operator
    ) {
        return Result.success(commandService.save(request, operator));
    }

    @PatchMapping("/{id}/enabled")
    public Result<Void> toggleEnabled(
        @PathVariable Long id,
        @RequestParam boolean enabled,
        @RequestParam(defaultValue = "api") String operator
    ) {
        commandService.changeEnabled(id, enabled, operator);
        return Result.success(null);
    }

    @PostMapping("/refresh")
    public Result<RefreshAiRuntimeConfigResponse> refresh(
        @RequestParam(defaultValue = "api") String operator
    ) {
        return Result.success(commandService.refreshCache(operator));
    }
}
