package com.ruici.ai.modules.simulation.model;

import java.util.Locale;

/**
 * 情景模拟类型。
 *
 * <p>当前平台内置三种场景，面试场景只是默认值之一，而非唯一场景。</p>
 */
public enum SimulationScenarioType {

  JOB_INTERVIEW(
      "job-interview",
      "求职面试",
      "围绕岗位能力、项目经验与岗位匹配度进行问答评估",
      "问题需偏向能力验证与真实业务场景"
  ),
  TCM_QA(
      "tcm-qa",
      "中医药答疑",
      "围绕中医药知识进行专业解读、辨析与场景化解释",
      "问题应聚焦概念辨析、辨证思路与安全边界"
  ),
  NOVEL_EXPERT(
      "novel-expert",
      "小说专家",
      "围绕人物、情节、世界观和文风提供创作分析建议",
      "问题应聚焦创作意图、结构推进与文风一致性"
  );

  private final String id;
  private final String displayName;
  private final String objective;
  private final String questionStyle;

  SimulationScenarioType(String id, String displayName, String objective, String questionStyle) {
    this.id = id;
    this.displayName = displayName;
    this.objective = objective;
    this.questionStyle = questionStyle;
  }

  public String id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }

  public String objective() {
    return objective;
  }

  public String questionStyle() {
    return questionStyle;
  }

  public static SimulationScenarioType fromNullable(String value) {
    if (value == null || value.isBlank()) {
      return JOB_INTERVIEW;
    }

    String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    for (SimulationScenarioType scenarioType : values()) {
      if (scenarioType.id.equals(normalized) || scenarioType.name().toLowerCase(Locale.ROOT).equals(normalized)) {
        return scenarioType;
      }
    }
    return JOB_INTERVIEW;
  }
}
