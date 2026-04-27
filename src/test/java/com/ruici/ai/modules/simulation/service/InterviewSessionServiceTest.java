package com.ruici.ai.modules.simulation.service;

import com.ruici.ai.common.exception.BusinessException;
import com.ruici.ai.modules.simulation.model.InterviewQuestionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("情景模拟会话服务测试")
class InterviewSessionServiceTest {

    @Nested
    @DisplayName("主问题计数")
    class MainQuestionCounting {

        @Test
        @DisplayName("只统计主问题，不把追问算入总题数")
        void shouldCountOnlyMainQuestions() {
            List<InterviewQuestionDTO> questions = List.of(
                InterviewQuestionDTO.create(0, "主问题1", "GENERAL", "综合能力", "topic-1", false, null),
                InterviewQuestionDTO.create(1, "追问1-1", "GENERAL", "综合能力-追问1", null, true, 0),
                InterviewQuestionDTO.create(2, "主问题2", "GENERAL", "综合能力", "topic-2", false, null),
                InterviewQuestionDTO.create(3, "追问2-1", "GENERAL", "综合能力-追问1", null, true, 2),
                InterviewQuestionDTO.create(4, "主问题3", "GENERAL", "综合能力", "topic-3", false, null)
            );

            assertThat(InterviewSessionService.countMainQuestions(questions)).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("简历文本校验")
    class ResumeTextValidation {

        @Test
        @DisplayName("有效的履历文本允许继续创建会话")
        void shouldAcceptMeaningfulResumeText() {
            String resumeText = "张三\n五年前端开发经验，负责多个项目交付。\n"
                + "工作经历：主导后台管理系统重构，优化性能与可维护性。\n"
                + "项目经历：负责招聘平台、数据报表与跨团队协作。\n"
                + "技能：Java、Spring Boot、React、PostgreSQL。";

            assertThatCode(() -> InterviewSessionService.validateResumeTextUsable(resumeText))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("过短文档拒绝作为基于简历的出题素材")
        void shouldRejectShortResumeText() {
            assertThatThrownBy(() -> InterviewSessionService.validateResumeTextUsable("太短了"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内容过短");
        }

        @Test
        @DisplayName("没有履历信号的噪声文本拒绝继续")
        void shouldRejectTextWithoutResumeSignals() {
            String noisyText = "天气真好，今天适合散步和拍照。"
                + "这里没有任何履历结构，只是普通随笔与旅行记录。"
                + "为了避免长度过短，这里再补一些日常描述，但依然不是个人背景材料。"
                + "文字继续延长，用来模拟一段很长但和履历毫无关系的生活感想、旅行碎片与天气记录。";

            assertThatThrownBy(() -> InterviewSessionService.validateResumeTextUsable(noisyText))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不像可用于面试的问题素材");
        }
    }
}
