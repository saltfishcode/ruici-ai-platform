package com.ruici.ai.common.ai;

import com.ruici.ai.common.exception.BusinessException;
import com.ruici.ai.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("结构化输出重试测试")
class StructuredOutputInvokerTest {

    @Mock
    private Logger log;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Test
    @DisplayName("重试提示词不应回填原始脏响应全文")
    void shouldNotEchoMalformedModelResponseIntoRetrySystemPrompt() throws Exception {
        StructuredOutputProperties properties = new StructuredOutputProperties();
        properties.setMaxAttempts(2);
        properties.setIncludeLastError(true);
        properties.setRetryUseRepairPrompt(true);
        properties.setRetryAppendStrictJsonInstruction(true);
        properties.setErrorMessageMaxLength(200);
        properties.setMetricsEnabled(false);
        StructuredOutputInvoker invoker = new StructuredOutputInvoker(properties, null);

        String malformedResponse = "Could not parse the given text to the desired target type: \"Here is the JSON response for the given scenario: ```json [ { \\\"question\\\": \\\"bad\\\" } ]```\" into class com.ruici.ai.modules.simulation.service.InterviewQuestionService$QuestionListDTO";

        Method method = StructuredOutputInvoker.class.getDeclaredMethod(
            "buildRetrySystemPrompt",
            String.class,
            Exception.class
        );
        method.setAccessible(true);
        String retryPrompt = (String) method.invoke(
            invoker,
            "system prompt with format",
            new RuntimeException(malformedResponse)
        );

        assertThat(retryPrompt)
            .contains("上次失败原因")
            .contains("模型返回内容不是合法 JSON")
            .doesNotContain("Here is the JSON response for the given scenario")
            .doesNotContain("Could not parse the given text to the desired target type");
    }

    @Test
    @DisplayName("invoke 在两次失败后仍抛业务异常")
    void shouldThrowBusinessExceptionAfterRetryFailures() {
        StructuredOutputProperties properties = new StructuredOutputProperties();
        properties.setMaxAttempts(2);
        properties.setIncludeLastError(true);
        properties.setRetryUseRepairPrompt(true);
        properties.setRetryAppendStrictJsonInstruction(true);
        properties.setErrorMessageMaxLength(200);
        properties.setMetricsEnabled(false);
        StructuredOutputInvoker invoker = new StructuredOutputInvoker(properties, null);

        BeanOutputConverter<QuestionListDTO> outputConverter = new BeanOutputConverter<>(QuestionListDTO.class);

        given(chatClient.prompt().system(anyString()).user(anyString()).call().entity(any(BeanOutputConverter.class)))
            .willThrow(new RuntimeException("Could not parse the given text to the desired target type: Here is the JSON response"))
            .willThrow(new RuntimeException("Range of input length should be [1, 3072]"));

        assertThatThrownBy(() -> invoker.invoke(
            chatClient,
            "system prompt with format",
            "user prompt",
            outputConverter,
            ErrorCode.INTERVIEW_QUESTION_GENERATION_FAILED,
            "方向题生成失败：",
            "方向题",
            log
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("方向题生成失败：")
            .hasMessageContaining("3072 tokens");
    }

    private record QuestionListDTO(List<QuestionDTO> questions) {}

    private record QuestionDTO(String question, String type, String category,
                               String topicSummary, List<String> followUps) {}
}
