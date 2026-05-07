package com.ruici.ai.common.config.runtime;

import com.ruici.ai.common.config.runtime.controller.AiRuntimeConfigController;
import com.ruici.ai.common.config.runtime.dto.AiRuntimeConfigDetailDTO;
import com.ruici.ai.common.config.runtime.dto.AiRuntimeConfigListItemDTO;
import com.ruici.ai.common.config.runtime.dto.RefreshAiRuntimeConfigResponse;
import com.ruici.ai.common.config.runtime.dto.SaveAiRuntimeConfigRequest;
import com.ruici.ai.common.config.runtime.service.AiRuntimeConfigCommandService;
import com.ruici.ai.common.config.runtime.service.AiRuntimeConfigQueryService;
import com.ruici.ai.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI 运行时配置控制器测试")
class AiRuntimeConfigControllerTest {

    @Mock
    private AiRuntimeConfigQueryService queryService;
    @Mock
    private AiRuntimeConfigCommandService commandService;
    @InjectMocks
    private AiRuntimeConfigController controller;

    @Test
    @DisplayName("列表查询委托给 QueryService")
    void shouldDelegateListToQueryService() {
        given(queryService.list(null, null, null))
            .willReturn(List.of());

        Result<List<AiRuntimeConfigListItemDTO>> result = controller.list(null, null, null);

        assertThat(result.isSuccess()).isTrue();
        verify(queryService).list(null, null, null);
    }

    @Test
    @DisplayName("详情查询委托给 QueryService")
    void shouldDelegateDetailToQueryService() {
        given(queryService.getDetail(1L))
            .willReturn(new AiRuntimeConfigDetailDTO(1L, "KEY", "chat", "global",
                "p", "m", null, true, 100, 1L, null, null, null, null));

        Result<AiRuntimeConfigDetailDTO> result = controller.detail(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().configKey()).isEqualTo("KEY");
    }

    @Test
    @DisplayName("保存委托给 CommandService")
    void shouldDelegateSaveToCommandService() {
        var request = new SaveAiRuntimeConfigRequest(null, "K", "chat", "global",
            "p", "m", null, true, 100, null);
        given(commandService.save(request, "tester")).willReturn(42L);

        Result<Long> result = controller.save(request, "tester");

        assertThat(result.getData()).isEqualTo(42L);
    }

    @Test
    @DisplayName("启停委托给 CommandService")
    void shouldDelegateToggleToCommandService() {
        Result<Void> result = controller.toggleEnabled(1L, false, "tester");

        assertThat(result.isSuccess()).isTrue();
        verify(commandService).changeEnabled(1L, false, "tester");
    }

    @Test
    @DisplayName("刷新缓存委托给 CommandService")
    void shouldDelegateRefreshToCommandService() {
        given(commandService.refreshCache("tester"))
            .willReturn(new RefreshAiRuntimeConfigResponse("ok", 5L));

        Result<RefreshAiRuntimeConfigResponse> result = controller.refresh("tester");

        assertThat(result.getData().latestConfigVersion()).isEqualTo(5L);
    }
}
