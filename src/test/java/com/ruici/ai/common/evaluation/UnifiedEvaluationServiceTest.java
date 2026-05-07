package com.ruici.ai.common.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分批评估 DTO 的 Jackson 反序列化回归测试。
 * <p>
 * 背景：模型返回的结构化 JSON 通过 BeanOutputConverter + Jackson 反序列化为 record DTO。
 * v1.3.0 曾出现 record 显式使用 @JsonCreator/@JsonProperty 时，
 * Jackson 报 "No fallback setter/field defined for creator property 'score'"，
 * 导致提前交卷时批次评估失败。
 * <p>
 * 修复：去掉 record 上的 @JsonCreator/@JsonProperty，依赖 Java record 原生构造器
 * （Jackson 2.12+ 原生支持）。
 */
@DisplayName("统一评估服务 DTO 反序列化")
class UnifiedEvaluationServiceTest {

    private record QuestionEvalDTO(
        int questionIndex,
        int score,
        String feedback,
        String referenceAnswer,
        List<String> keyPoints
    ) {}

    private record BatchReportDTO(
        int overallScore,
        String overallFeedback,
        List<String> strengths,
        List<String> improvements,
        List<QuestionEvalDTO> questionEvaluations
    ) {}

    @Test
    @DisplayName("Jackson 可以正确反序列化 QuestionEvalDTO 纯 record（无 @JsonCreator）")
    void shouldDeserializeQuestionEvalRecord() {
        BeanOutputConverter<QuestionEvalDTO> converter = new BeanOutputConverter<>(QuestionEvalDTO.class);
        String json = """
            {
                "questionIndex": 2,
                "score": 85,
                "feedback": "回答逻辑清晰",
                "referenceAnswer": "参考：AOP 原理",
                "keyPoints": ["切面", "切入点"]
            }
            """;

        QuestionEvalDTO result = converter.convert(json);

        assertThat(result).isNotNull();
        assertThat(result.questionIndex()).isEqualTo(2);
        assertThat(result.score()).isEqualTo(85);
        assertThat(result.feedback()).isEqualTo("回答逻辑清晰");
        assertThat(result.referenceAnswer()).isEqualTo("参考：AOP 原理");
        assertThat(result.keyPoints()).containsExactly("切面", "切入点");
    }

    @Test
    @DisplayName("Jackson 可以正确反序列化嵌套 BatchReportDTO（含 List<QuestionEvalDTO>）")
    void shouldDeserializeBatchReportWithNestedEvaluations() {
        BeanOutputConverter<BatchReportDTO> converter = new BeanOutputConverter<>(BatchReportDTO.class);
        String json = """
            {
                "overallScore": 78,
                "overallFeedback": "整体表现良好",
                "strengths": ["逻辑清晰", "表达流畅"],
                "improvements": ["深度不足"],
                "questionEvaluations": [
                    {
                        "questionIndex": 0,
                        "score": 90,
                        "feedback": "很好",
                        "referenceAnswer": "答案A",
                        "keyPoints": ["P1", "P2"]
                    },
                    {
                        "questionIndex": 1,
                        "score": 66,
                        "feedback": "一般",
                        "referenceAnswer": "答案B",
                        "keyPoints": ["P3"]
                    }
                ]
            }
            """;

        BatchReportDTO result = converter.convert(json);

        assertThat(result).isNotNull();
        assertThat(result.overallScore()).isEqualTo(78);
        assertThat(result.overallFeedback()).isEqualTo("整体表现良好");
        assertThat(result.strengths()).containsExactly("逻辑清晰", "表达流畅");
        assertThat(result.improvements()).containsExactly("深度不足");
        assertThat(result.questionEvaluations()).hasSize(2);
        assertThat(result.questionEvaluations().get(0).score()).isEqualTo(90);
        assertThat(result.questionEvaluations().get(1).score()).isEqualTo(66);
    }

    @Test
    @DisplayName("Jackson 可以反序列化 int 字段为零值的 JSON（score=0, questionIndex=0）")
    void shouldDeserializeRecordWithZeroIntValues() {
        BeanOutputConverter<QuestionEvalDTO> converter = new BeanOutputConverter<>(QuestionEvalDTO.class);
        // 这是"提前交卷/未作答"场景的典型返回
        String json = """
            {
                "questionIndex": 0,
                "score": 0,
                "feedback": "未作答",
                "referenceAnswer": "",
                "keyPoints": []
            }
            """;

        QuestionEvalDTO result = converter.convert(json);

        assertThat(result).isNotNull();
        assertThat(result.questionIndex()).isZero();
        assertThat(result.score()).isZero();
        assertThat(result.feedback()).isEqualTo("未作答");
        assertThat(result.keyPoints()).isEmpty();
    }
}
