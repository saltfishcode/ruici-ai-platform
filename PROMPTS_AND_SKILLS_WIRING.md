# Prompts and Skills Wiring Map

本文档用于沉淀当前项目中 `src/main/resources/prompts/` 与 `src/main/resources/skills/` 的注册方式、调用链路、触发条件与维护注意事项。

目标不是单纯列出资源文件，而是回答下面几个维护问题：

- 某个 prompt / skill 是否已经被代码接入。
- 它是“确定会触发”、"条件触发"，还是“仅注册但不保证实际 tool call”。
- 哪些业务链路走默认 `ChatClient`，哪些走 `plain` / `voice` client。
- 哪些 README / CLAUDE 中描述的能力已经落地，哪些仍然只是扩展目标。

---

## 1. 判定标准

本文档统一使用以下判定层级：

- **确定触发**：存在明确业务入口，且代码中能追到实际模型调用（例如 `.call()`、`.stream()` 或 `structuredOutputInvoker.invoke(...)`）。
- **条件触发**：代码已接线，但依赖异步消费、配置开关、provider 分支或特定输入条件。
- **仅注册**：资源或 ToolCallback 已注册到 Spring / ChatClient，但不能据此断言运行时一定发生 tool call。
- **未接入**：当前代码中没有发现可达调用链。

需要特别区分两件事：

1. `skills/*` 被后端代码读取并注入 Prompt。
2. `SkillsTool` 被注册到 Spring AI，并由模型在运行时真正发起 tool call。

前者在本项目中大量存在，后者只属于“具备能力”，不能默认视为每次都会发生。

---

## 2. 资源总览

### 2.1 Prompt 资源

当前 `src/main/resources/prompts/` 下有 14 个模板文件；按本次静态核对，已在当前主链路中发现明确引用：

- `resume-analysis-system.st`
- `resume-analysis-user.st`
- `knowledgebase-query-system.st`
- `knowledgebase-query-user.st`
- `knowledgebase-query-rewrite.st`
- `interview-question-skill-system.st`
- `interview-question-skill-user.st`
- `interview-question-resume-system.st`
- `interview-question-resume-user.st`
- `jd-parse-system.st`
- `interview-evaluation-system.st`
- `interview-evaluation-user.st`
- `interview-evaluation-summary-system.st`
- `interview-evaluation-summary-user.st`

### 2.2 Skill 资源

`src/main/resources/skills/` 下当前包含：

- `_shared/`
- `ai-agent-dev/`
- `algorithm/`
- `ali-backend/`
- `bytedance-backend/`
- `frontend/`
- `java-backend-tencent/`
- `java-backend/`
- `novel-expert/`
- `python-backend/`
- `system-design/`
- `tcm-qa/`
- `test-development/`

这些 skill 资源在当前项目中有两种使用方式：

- 作为 Spring AI `SkillsTool` 的技能根目录。
- 作为 `simulation` / `voice` 评估阶段的后端静态知识源，由业务代码直接加载 `SKILL.md`、`skill.meta.yml` 与 references。

---

## 3. Shared AI Wiring

### 3.1 SkillsTool 注册

`AgentUtilsConfiguration` 会把 `app.ai.agent-utils.skills-root` 对应的目录注册为 `interviewSkillsToolCallback`。

- 配置类：`src/main/java/com/ruici/ai/common/ai/AgentUtilsConfiguration.java`
- 配置属性：`src/main/java/com/ruici/ai/common/ai/AgentUtilsProperties.java`
- 默认值：`classpath:skills`
- yml 来源：`src/main/resources/application.yml`

行为特征：

- 若 `skillsRoot` 不存在，会直接抛异常，说明它不是可选注释性配置，而是实际启用逻辑的一部分。
- 这一步只能证明 **SkillsTool 已注册**，不能证明后续每次业务调用都会实际触发 tool call。
- 即使走默认 client，也还要满足 `interviewSkillsToolCallback` bean 实际存在，才会挂载 `defaultToolCallbacks(...)`。

判定：**仅注册**。

### 3.2 ChatClient 类型分层

`LlmProviderRegistry` 是整个项目理解 prompt / skill 使用情况的关键。

#### 默认 client

- 方法：`getChatClient(...)`、`getDefaultChatClient()`、`getChatClientOrDefault(...)`
- 行为：会尝试挂载 `defaultToolCallbacks(interviewSkillsToolCallback)`，并在配置允许时挂载 `ToolCallAdvisor`、memory、logger。

判定：**具备 skill tool 能力**。

#### plain client

- 方法：`getPlainChatClient(...)`
- 行为：显式构造不带 tools / advisors 的 `ChatClient`。

判定：**不会触发 SkillsTool**。

#### voice client

- 方法：`getVoiceChatClient(...)`
- 行为：同样显式构造 plain/no tools 的 `ChatClient`，目的是避免实时语音链路因为 tool calling 导致不稳定。

判定：**不会触发 SkillsTool**。

### 3.3 关键配置开关

以下配置会直接影响 prompt / skill 的实际行为：

- `app.ai.agent-utils.skills-root`
- `app.ai.default-provider`
- `app.ai.providers.*`
- `app.ai.advisors.enabled`
- `app.ai.advisors.tool-call-enabled`
- `app.ai.advisors.tool-call-conversation-history-enabled`
- `app.ai.advisors.stream-tool-call-responses`
- `app.ai.rag.rewrite.enabled`
- `app.ai.structured-*`

相关类：

- `src/main/java/com/ruici/ai/common/config/LlmProviderProperties.java`
- `src/main/java/com/ruici/ai/common/ai/StructuredOutputProperties.java`
- `src/main/java/com/ruici/ai/common/ai/AgentUtilsProperties.java`

---

## 4. Document 模块

### 4.1 对应资源

- `resume-analysis-system.st`
- `resume-analysis-user.st`

### 4.2 注册与加载位置

`ResumeAnalysisProperties` 负责绑定默认 prompt 路径：

- 配置类：`src/main/java/com/ruici/ai/modules/document/service/ResumeAnalysisProperties.java`
- 默认值：
  - `classpath:prompts/resume-analysis-system.st`
  - `classpath:prompts/resume-analysis-user.st`

`ResumeGradingService` 构造时将这两个模板读入为 `PromptTemplate`。

### 4.3 业务调用链

文档分析的核心链路为：

1. 文档上传进入 `ResumeUploadService`。
2. 任务进入 Redis Stream。
3. `AnalyzeStreamConsumer` 消费分析任务。
4. `ResumeGradingService.analyzeResume(...)` 组装 prompt。
5. `structuredOutputInvoker.invoke(...)` 发起模型调用并解析结构化返回。

关键文件：

- `src/main/java/com/ruici/ai/modules/document/listener/AnalyzeStreamConsumer.java`
- `src/main/java/com/ruici/ai/modules/document/service/ResumeGradingService.java`

### 4.4 Skill 使用情况

`ResumeGradingService` 使用的是 `llmProviderRegistry.getDefaultChatClient()`。

这意味着：

- **Prompt 调用**：确定发生。
- **SkillsTool 能力**：已挂载到默认 client。
- **实际 tool call**：不能仅凭接线断言一定发生，取决于模型是否发起工具调用。

### 4.5 判定

- Prompt：**确定触发**
- SkillsTool：**仅注册 / 可能参与，但不能保证每次触发**

---

## 5. Simulation 模块

`simulation` 是当前项目中 prompt / skill 关系最复杂的模块，需拆成“题目生成”、“JD 解析”、“评估”、“方向建模”四层理解。

### 5.1 对应 Prompt 资源

- `interview-question-skill-system.st`
- `interview-question-skill-user.st`
- `interview-question-resume-system.st`
- `interview-question-resume-user.st`
- `jd-parse-system.st`
- `interview-evaluation-system.st`
- `interview-evaluation-user.st`
- `interview-evaluation-summary-system.st`
- `interview-evaluation-summary-user.st`

### 5.2 题目生成：方向题与基于文档题

#### 注册位置

`InterviewQuestionProperties` 绑定四个出题模板路径：

- 配置类：`src/main/java/com/ruici/ai/modules/simulation/service/InterviewQuestionProperties.java`
- 前缀：`app.interview`

#### 业务调用链

主入口：

1. `InterviewController`
2. `InterviewSessionService.createSession(...)`
3. `InterviewQuestionService.generateQuestionsBySkill(...)`

在 `generateQuestionsBySkill(...)` 中会根据是否带文档上下文分成两支：

- **方向题**：`generateDirectionOnly(...)`
- **基于文档题**：`generateResumeQuestions(...)`

#### 方向题

特点：

- 使用 `interview-question-skill-system.st` 与 `interview-question-skill-user.st`
- 通过 `structuredOutputInvoker.invoke(...)` 调用模型
- 引入 `skillService.buildReferenceSection(...)` 注入分类参考内容
- 使用由 `InterviewSessionService` 传入的默认 client

这条链路说明：

- prompt 会确定触发。
- skill 资源也确定被业务代码读取并注入 prompt。
- 是否发生 Spring AI 的 tool call，不可保证。

判定：

- Prompt：**确定触发**
- 后端 skill 资源注入：**确定触发**
- SkillsTool：**仅注册 / 可能参与**

#### 基于文档题

特点：

- 使用 `interview-question-resume-system.st` 与 `interview-question-resume-user.st`
- 通过 `structuredOutputInvoker.invoke(...)` 调用模型
- 显式使用 `llmProviderRegistry.getPlainChatClient(null)`

这意味着：

- prompt 会确定触发。
- 这一分支显式绕过 skills tool / advisors。

判定：

- Prompt：**确定触发**
- SkillsTool：**不会触发**

### 5.3 JD 解析

`InterviewSkillService.parseJd(...)` 会：

- 加载 `jd-parse-system.st`
- 使用 `structuredOutputInvoker.invoke(...)`
- 通过默认 `ChatClient` 调用模型

同时，它会把当前已知 reference 文件列表拼入 prompt，指导模型把 JD 分类映射到现有技能参考。

判定：

- Prompt：**确定触发**
- SkillsTool：**仅注册 / 可能参与**
- skill metadata / references：**确定参与**

### 5.4 评估与报告

评估 prompt 不在 `modules/simulation` 目录下，而是在 shared 层的 `UnifiedEvaluationService` 中统一加载：

- `interview-evaluation-system.st`
- `interview-evaluation-user.st`
- `interview-evaluation-summary-system.st`
- `interview-evaluation-summary-user.st`

调用链分两种：

#### 同步生成报告

1. `InterviewSessionService.generateReport(...)`
2. `AnswerEvaluationService.evaluateInterview(...)`
3. `UnifiedEvaluationService.evaluate(...)`

#### 异步评估

1. `submitAnswer(...)` 最后一题完成后入 Redis Stream
2. `EvaluateStreamConsumer`
3. `AnswerEvaluationService`
4. `UnifiedEvaluationService.evaluate(...)`

评估阶段还会注入 `skillService.buildEvaluationReferenceSectionSafe(skillId)` 生成的参考基线。

判定：

- Prompt：**确定触发**（异步链路属于业务完成后确定发生）
- skill reference 注入：**确定触发**
- SkillsTool：**仅注册 / 可能参与**

### 5.5 三个大方向的落地情况

README 中描述的三个方向并不是纯口号，代码里已经有显式建模：

- 求职面试
- 专业答疑
- 职业沟通表达

落地方式主要体现在：

- `SimulationDirection`
- `SimulationScenarioType`
- `InterviewQuestionService.resolveDifficulty(...)`
- `scenarioType` 对 `objective`、`questionStyle`、`aiRoleLabel` 的注入

这说明三方向已经对出题风格和场景目标产生实际影响。

但当前实现仍需注意：

- 主要还是 `prompts + skill metadata + reference 注入` 模式。
- 我没有发现 `simulation` 主链路直接调用 `knowledgebase` 模块做 RAG 检索增强。

结论：

- 三个方向：**已落地为真实业务分支**
- “knowledgebase + prompts + skills 三者协同驱动 simulation 主流程”：**当前未完全落地**

---

## 6. Knowledgebase / RAG 模块

### 6.1 对应 Prompt 资源

- `knowledgebase-query-system.st`
- `knowledgebase-query-user.st`
- `knowledgebase-query-rewrite.st`

### 6.2 注册与加载位置

`KnowledgeBaseQueryProperties` 负责绑定：

- `systemPromptPath`
- `userPromptPath`
- `rewritePromptPath`
- `rewrite.enabled`

默认前缀：`app.ai.rag`

### 6.3 业务调用链

#### 同步问答

1. `KnowledgeBaseQueryService.answerQuestion(...)`
2. 向量检索 `retrieveRelevantDocs(...)`
3. 命中后构造 `systemPrompt + userPrompt`
4. 走 `chatClient.prompt().system(...).user(...).call()` 或 gateway 分支

#### 流式问答

1. `KnowledgeBaseQueryService.answerQuestionStream(...)`
2. query rewrite + 检索
3. 走 `.stream().content()` 或 gateway 分支

#### Query Rewrite

1. `rewriteQuestion(...)`
2. 使用 `knowledgebase-query-rewrite.st`
3. 仅在 `rewriteEnabled=true` 且问题非空时触发

### 6.4 Skill 使用情况

RAG 主要依赖的是：

- prompt 模板
- 向量检索结果
- provider / gateway 分支

它使用默认 `ChatClient`，因此 **具备 SkillsTool 能力**。但从当前实现看，它的主设计目标并不是“靠技能工具回答”，而是“靠检索上下文回答”。

另外需要注意 gateway 分支：

- 当 `OpenAiCompatibleGatewayClient.supports(providerId)` 为真时，会直接走 gateway client。
- 这条分支绕开了 `ChatClient.prompt()`，因此不会经过 `ChatClient` 上挂载的 tool callback / advisors。

### 6.5 判定

- 主问答 prompt：**确定触发**
- rewrite prompt：**条件触发**
- SkillsTool：**仅注册 / 可能参与**
- gateway 分支下的 SkillsTool：**不会参与**

---

## 7. Voice 模块

`voice` 必须拆成“实时语音对话”和“会后评估”两条链理解，因为两条链使用的 `ChatClient` 类型不同。

### 7.1 实时语音对话

主链路：

1. `VoiceInterviewWebSocketHandler`
2. `DashscopeLlmService.chat(...)` / `chatStreamSentences(...)`
3. `llmProviderRegistry.getVoiceChatClient(provider)`

这里的关键点是：

- `getVoiceChatClient(...)` 是 plain/no tools client。
- `VoiceInterviewPromptService.generateSystemPromptWithContext(...)` 会在系统提示中描述 role / skill 语境。
- 系统 prompt 文本里即便提到“可调用 skill tool”，运行时 client 也没有挂接 tools。

另外，开场白不是 prompt 模板生成，而是来自：

- `src/main/resources/voice-interview-opening.yml`

因此实时语音主链路的结论是：

- 使用了系统 prompt 组装逻辑，但**当前实时对话主链路不使用 `resources/prompts/*.st`**。
- Skill 语义可能通过业务服务注入到上下文，但**不会通过 SkillsTool tool call 执行**。

判定：

- `prompts/*.st`：**未接入**
- SkillsTool：**不会触发**
- `voice-interview-opening.yml`：**确定使用**

### 7.2 会后评估

主链路：

1. `VoiceInterviewService.endSession(...)` 或手动触发评估
2. `VoiceEvaluateStreamProducer` 入队
3. `VoiceEvaluateStreamConsumer`
4. `VoiceInterviewEvaluationService.generateEvaluation(...)`
5. `UnifiedEvaluationService.evaluate(...)`

这一分支会：

- 使用默认 `ChatClient`（`getChatClientOrDefault(provider)`）
- 注入 `skillService.buildEvaluationReferenceSectionSafe(session.getSkillId())`
- 复用 shared 的 `interview-evaluation-*` 与 `interview-evaluation-summary-*` prompt

判定：

- Prompt：**确定触发**
- skill reference 注入：**确定触发**
- SkillsTool：**仅注册 / 可能参与**

---

## 8. Schedule 模块

虽然用户当前重点不是 `schedule`，但从“尽量找全”的角度，仍建议记录它，因为它也包含 AI 调用。

### 8.1 Prompt 形态

`schedule` 当前没有使用 `src/main/resources/prompts/*.st` 中的模板文件。

`InterviewParseService` 使用的是类内内联常量 `PARSE_PROMPT`，不是 resources 下的模板文件。

### 8.2 业务调用链

1. `InterviewScheduleController`
2. `InterviewParseService.parse(...)`
3. 先规则解析
4. 规则失败后才进入 `parseWithAI(...)`
5. 使用 `llmProviderRegistry.getChatClientOrDefault(provider)` + `.call()`

### 8.3 Skill 使用情况

由于使用的是默认 `ChatClient`，理论上具备 tool callback 能力；但当前业务意图是结构化解析文本，不是使用 skill tool 完成复杂工具编排。

判定：

- `prompts/*.st`：**未接入**
- 内联 Prompt：**确定触发**
- SkillsTool：**仅注册 / 可能参与**

---

## 9. 配置类与默认路径速查

### 9.1 Prompt 路径配置类

- `ResumeAnalysisProperties`
  - 前缀：`app.resume.analysis`
  - 路径：`resume-analysis-system.st` / `resume-analysis-user.st`

- `InterviewQuestionProperties`
  - 前缀：`app.interview`
  - 路径：`interview-question-skill-*` / `interview-question-resume-*`

- `InterviewEvaluationProperties`
  - 前缀：`app.interview.evaluation`
  - 路径：`interview-evaluation-*` / `interview-evaluation-summary-*`

- `KnowledgeBaseQueryProperties`
  - 前缀：`app.ai.rag`
  - 路径：`knowledgebase-query-system.st` / `knowledgebase-query-user.st` / `knowledgebase-query-rewrite.st`

### 9.2 Skill 与 Tool 配置类

- `AgentUtilsProperties`
  - 前缀：`app.ai.agent-utils`
  - 默认 `skillsRoot=classpath:skills`

- `LlmProviderProperties`
  - 前缀：`app.ai`
  - 管理 provider、advisors、tool-call 相关开关

- `StructuredOutputProperties`
  - 前缀：`app.ai`
  - 管理结构化输出重试、错误注入、严格 JSON 指令与 metrics

---

## 10. 当前项目的真实结论

### 10.1 已经明确落地的部分

- `document` 的文档分析 prompt 已真实接入并使用。
- `simulation` 的题目生成、JD 解析、评估 prompt 已真实接入并使用。
- `simulation` 的 skill 资源并非摆设，后端会主动加载 `SKILL.md`、`skill.meta.yml` 与 references。
- `knowledgebase` 的 RAG 主问答、流式问答与 query rewrite prompt 已真实接入。
- `voice` 的会后评估复用了 simulation evaluation prompt；实时对话则走 voice 专用 plain client。

### 10.2 需要避免的误判

- **不能**因为 `SkillsTool` 已注册，就认定所有默认 client 链路都会实际发生 tool call。
- **不能**把 `simulation` 里的“skill”简单理解成“全靠 Spring AI 工具调用”；当前更真实的实现是“后端主动加载 skill 资源并把内容注入 prompt”。
- **不能**把 `voice` 实时链路当成 skill-enabled agent 流程；它明确绕开了 tools。
- **不能**把 `knowledgebase` 的 gateway 分支误判为也会经过 `ChatClient` 上的 tool callbacks。

### 10.3 当前尚未完全落地的方向

- `simulation` 虽然已经有三个方向和多个 skill pack，但我没有发现主链路直接接入 `knowledgebase` 模块做 RAG 增强。
- README 中“通过知识库、提示词模板和 Skill 组合出不同垂类能力”的目标，在 `simulation` 主流程上目前更接近“部分落地”。

---

## 11. 后续维护建议

### 11.1 新增 Prompt 时

- 优先新增 `@ConfigurationProperties` 绑定路径，不要把路径硬编码在多个 service 中。
- 明确记录它走的是默认 client、plain client 还是 voice client。
- 若是结构化输出，优先复用 `StructuredOutputInvoker`。

### 11.2 新增 Skill 时

- 如果目的是给 `simulation` / `voice` 评估提供参考内容，需要同时补齐：
  - `skills/<skillId>/SKILL.md`
  - `skills/<skillId>/skill.meta.yml`
  - 对应 reference 文件
- 如果目的是让模型在默认 client 下可进行 tool calling，则还要确认：
  - 该链路没有走 `plain` / `voice` client
  - `app.ai.advisors.tool-call-enabled=true`
  - provider 与 gateway 分支没有绕过 `ChatClient`

### 11.3 排障建议

当出现“为什么这个 skill 没生效”时，请按下面顺序排查：

1. 业务链路是否用了默认 `ChatClient`。
2. 是否走了 gateway / plain / voice 分支。
3. `skillsRoot` 是否正确，skill 目录是否存在。
4. 如果是 simulation / voice 评估参考内容问题，检查 `skill.meta.yml` 与 reference 文件映射是否完整。
5. 如果是 RAG rewrite 问题，检查 `app.ai.rag.rewrite.enabled`。

---

## 12. 关键代码索引

### Shared

- `src/main/java/com/ruici/ai/common/ai/AgentUtilsConfiguration.java`
- `src/main/java/com/ruici/ai/common/ai/AgentUtilsProperties.java`
- `src/main/java/com/ruici/ai/common/ai/LlmProviderRegistry.java`
- `src/main/java/com/ruici/ai/common/ai/StructuredOutputInvoker.java`
- `src/main/java/com/ruici/ai/common/ai/StructuredOutputProperties.java`
- `src/main/java/com/ruici/ai/common/config/LlmProviderProperties.java`
- `src/main/resources/application.yml`

### Document

- `src/main/java/com/ruici/ai/modules/document/service/ResumeAnalysisProperties.java`
- `src/main/java/com/ruici/ai/modules/document/service/ResumeGradingService.java`
- `src/main/java/com/ruici/ai/modules/document/listener/AnalyzeStreamConsumer.java`

### Simulation

- `src/main/java/com/ruici/ai/modules/simulation/service/InterviewQuestionProperties.java`
- `src/main/java/com/ruici/ai/modules/simulation/service/InterviewQuestionService.java`
- `src/main/java/com/ruici/ai/modules/simulation/service/InterviewSessionService.java`
- `src/main/java/com/ruici/ai/modules/simulation/skill/InterviewSkillService.java`
- `src/main/java/com/ruici/ai/common/evaluation/InterviewEvaluationProperties.java`
- `src/main/java/com/ruici/ai/common/evaluation/UnifiedEvaluationService.java`

### Knowledgebase

- `src/main/java/com/ruici/ai/modules/knowledgebase/service/KnowledgeBaseQueryProperties.java`
- `src/main/java/com/ruici/ai/modules/knowledgebase/service/KnowledgeBaseQueryService.java`
- `src/main/java/com/ruici/ai/common/ai/OpenAiCompatibleGatewayClient.java`

### Voice

- `src/main/java/com/ruici/ai/modules/voice/handler/VoiceInterviewWebSocketHandler.java`
- `src/main/java/com/ruici/ai/modules/voice/service/DashscopeLlmService.java`
- `src/main/java/com/ruici/ai/modules/voice/service/VoiceInterviewPromptService.java`
- `src/main/java/com/ruici/ai/modules/voice/service/VoiceInterviewEvaluationService.java`
- `src/main/java/com/ruici/ai/modules/voice/listener/VoiceEvaluateStreamConsumer.java`
- `src/main/resources/voice-interview-opening.yml`

### Schedule

- `src/main/java/com/ruici/ai/modules/schedule/service/InterviewParseService.java`
