# AI 运行时动态配置落地说明 v1.3.0

> **本文档更新说明**：
> 本文最初撰写于 v1.3.0 启动阶段，反映设计意图与初始范围。
> 实际实施过程中，范围逐步扩大——embedding 和 voice 的 snapshot 接入比计划更早完成，
> Phase 7 管理接口与前端页面也已实现。
> 本文标记 ⚠️ 表示"初始设计如此，实际已超前"，标记 ✅ 表示"已实现"。
> 执行顺序与最终交付的完整对照见 `private/重构v3/AI模型动态配置执行计划.md` 第 5.3 节。
> 最终状态截止于 2026-05-07。

## 1. 背景

在 `v1.2.x` 之前，项目虽然已经具备多 Provider 架构，但 chat 链路里的 Provider / Model 选择仍然主要依赖：

1. 静态配置（`app.ai.default-provider`、`app.ai.providers.*`）
2. 个别模块自己的 provider 覆盖配置（例如 `app.ai.rag.llm-provider`）
3. 各业务服务在调用前自行决定使用哪个 provider / client

这种方式在功能还少时足够直接，但随着 `document`、`simulation`、`knowledgebase` 三个模块同时依赖 chat，
问题逐渐变得明显：

- provider/model 的优先级逻辑分散在多个业务服务里，不利于统一修改
- `LlmProviderRegistry` 过去更偏向按 provider 粗粒度缓存，模型切换后存在复用旧 client 的风险
- 数据库配置、请求级覆盖、静态配置之间缺少统一收敛点
- 想做运营侧或后台侧的“运行时调模型”，必须先把控制面与消费面拆开

因此，`v1.3.0` 的目标不是一次性把所有 AI 能力都做成动态切换，而是先把 **chat-first 运行时配置基础层**
落地，并且只接入 chat 业务链路。

## 2. 本次版本的明确边界

### 2.1 已纳入范围

- `document`：文档分析评分链路
- `simulation`：会话创建、题目生成、评估链路
- `knowledgebase`：RAG 改写、问答、流式回答链路

这些链路的共同点都是：本质依赖 chat model，且更容易遇到“需要临时切换模型 / fallback”的需求。

### 2.2 暂不纳入范围 ⚠️

> ⚠️ **初始设计**：以下能力在 v1.3.0 初始启动阶段暂不纳入。
> **实际情况**：在后续迭代中以下能力已部分完成，详见下文。

- `embedding` ✅

  初始设计理由：embedding 更适合按任务/批次级快照切换，不能在 chunk 级频繁查库。
  
  实际情况：**已按任务级 / 查询级快照落地**至知识库向量化与检索链路（`EmbeddingProviderRegistry`、`VectorizeStreamConsumer`），
  满足"批次级解析、不 chunk 级查库"的约束。

- `voice` ✅

  初始设计理由：voice 更适合按会话级快照切换，不能在通话进行中漂移 provider/model。
  
  实际情况：**已按会话级 LLM 快照落地**至实时对话与异步评估链路。
  实时对话使用 `voice client`（plain/no-tools），会后评估使用 `default client`，但两者共用同一份会话固化快照。
  `ASR/TTS` 仍保持静态稳定配置，不在当前范围接入动态热切换。

## 3. 设计原则

本次落地遵循以下原则：

1. **统一解析优先级**：业务模块不允许自行拼接 provider/model 来源顺序
2. **数据库动态配置优先，静态配置兜底**：运行时控制面先走数据库，失效时再落回配置文件
3. **只存非敏感控制信息**：数据库中不保存真实 API Key，只保存 providerId/modelName/fallbackModelName/scene/domain 等
4. **模型感知缓存**：chat client 缓存必须把 model/fallback/baseUrl/configVersion 纳入 key
5. **last-known-good 兜底**：当数据库配置异常或波动时，优先保证服务稳定，而不是把所有调用直接打挂

## 4. 新增的基础层

本次新增的核心代码位于：

```text
src/main/java/com/ruici/ai/common/config/runtime/
```

按职责分为以下子包：

| 子包 | 职责 | 包含文件 |
|---|---|---|
| `model/` | 核心枚举 | `AiRuntimeDomain`、`AiRuntimeScene`、`AiRuntimeConfigSource` |
| `snapshot/` | 快照与解析上下文 | `AiRuntimeConfigSnapshot`、`AiRuntimeResolveContext` |
| `entity/` | JPA 持久化实体 | `AiRuntimeConfigEntity`、`AiRuntimeConfigAuditEntity` |
| `repository/` | 数据访问仓库 | `AiRuntimeConfigRepository`、`AiRuntimeConfigAuditRepository` |
| `resolver/` | 解析器接口与实现 | `AiRuntimeConfigResolver`、`DefaultAiRuntimeConfigResolver` |
| `policy/` | 策略校验 | `AiRuntimePolicyService`、`DefaultAiRuntimePolicyService`、`AiRuntimeConfigValidationService` |
| `service/` | 管理业务服务 | `AiRuntimeConfigCommandService`、`AiRuntimeConfigQueryService` |
| `controller/` | REST 端点 | `AiRuntimeConfigController` |
| `dto/` | 数据传输对象 | 4 个 request/response record |

### 前端（Phase 7）

```text
frontend/src/pages/AiRuntimeConfigPage.tsx
frontend/src/api/aiRuntimeConfig.ts
```

- 侧边栏「模型配置」页面，支持列表查看、按 domain/scene 过滤、编辑弹窗、启用/禁用开关、刷新缓存按钮
- 编辑时弹窗按 domain 给出约束提示（chat 可任意 provider，embedding/asr/tts 仅限 dashscope）

其中可以这样理解：

- `Entity / Repository`：数据库控制面
- `ResolveContext`：一次解析请求的输入条件
- `Snapshot`：一次解析结果的不可变快照
- `PolicyService`：做约束与合法性校验
- `Resolver`：做真正的优先级解析与兜底策略

## 4a. AI 运行时配置解析流程

下面这个图表展示了一次 AI 模型调用从业务模块发出到拿到 ChatClient 的完整路径：

```text
┌─────────────────────────────────────────────────────────────────┐
│  业务 Service 调用                                                │
│  (simulation/document/knowledgebase/voice)                       │
│  例: InterviewQuestionService.generateDirectionOnly(...)          │
│      ResumeGradingService.analyzeResume(...)                      │
│      KnowledgeBaseQueryService.answerQuestion(...)                │
│      VoiceInterviewEvaluationService.generateEvaluation(...)       │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │  resolveChatConfig(context)
                         │  resolveEmbeddingConfig(context)
                         ▼
┌────────────────────────────────────────────────────────────────┐
│          DefaultAiRuntimeConfigResolver                         │
│                                                                │
│  入口 resolveConfig(context)                                     │
│     │                                                            │
│     ├── 有请求覆盖(isRequestOverridePresent)                       │
│     │   └──→ resolveRequestOverride() ──→ Snapshot(REQUEST_OVERRIDE)
│     │        跳过缓存，直接返回                                      │
│     │                                                            │
│     ├── snapshotCache 命中                                       │
│     │   └──→ 直接返回缓存 Snapshot (Hot Path, 最快)                 │
│     │                                                            │
│     └── 缓存未命中 → refreshSnapshot()                             │
│           │                                                      │
│           ├── loadSnapshotFromDatabase()                          │
│           │   ├── DB 找到启用配置 → Snapshot(DB_RUNTIME_CONFIG)     │
│           │   │    缓存该快照并返回                                 │
│           │   └── DB 未命中 → buildStaticSnapshot()               │
│           │        └──→ Snapshot(ENV_CONFIG)                      │
│           │             来自 app.ai.default-provider 等静态配置      │
│           │                                                      │
│           └── DB 异常 → 两级兜底                                    │
│                ├── lastKnownGood 存在 → Snapshot(LAST_KNOWN_GOOD) │
│                │    上次成功解析的缓存，已标记 stale=true            │
│                └── lastKnownGood 不存在 → buildStaticSnapshot()    │
│                      → Snapshot(ENV_CONFIG, stale=false)         │
└────────────────────────┬────────────────────────────────────────┘
                         │ 返回 AiRuntimeConfigSnapshot
                         ▼
┌────────────────────────────────────────────────────────────────┐
│          LlmProviderRegistry                                     │
│                                                                │
│  getChatClient(snapshot)                                         │
│     │                                                            │
│     ├── clientCache 命中                                          │
│     │   └──→ 返回缓存中 ChatClient (Hot Path, 不用重建 HTTP 连接)   │
│     │                                                            │
│     └── 缓存未命中 → 创建新 ChatClient                             │
│          缓存 key = providerId:clientType:modelName:             │
│                     fallback:normalizedBaseUrl:configVersion     │
│           │                                                      │
│           ├── clientType=default → 带 SkillsTool + Advisors      │
│           ├── clientType=plain   → 不带 tools / advisors         │
│           └── clientType=voice   → plain, 不带 tools / advisors  │
│                                                                │
│  创建的 ChatClient 用于:                                         │
│   · .call()          → 同步结构化输出                                 │
│   · .stream()        → 流式响应                                      │
│   · prompt().call()  → 带 system/user prompt 调用                   │
└────────────────────────────────────────────────────────────────┘

解析优先级（数字越小优先级越高）：
 ① REQUEST_OVERRIDE   (最优先，仅被允许的链路)
 ② DB_RUNTIME_CONFIG  (数据库动态配置，前端页面管理)
 ③ ENV_CONFIG         (application.yml 静态配置)
 ④ LAST_KNOWN_GOOD    (上次成功值，DB 不可用时兜底)
 ⑤ CODE_DEFAULT       (硬编码默认值，极端保护)

缓存体系（全部在 JVM 内存中）：
 ┌───────────────┬──────────────────┬──────────────────────────┐
 │ 缓存名称        │ 存储内容          │ 释放方式                   │
 ├───────────────┼──────────────────┼──────────────────────────┤
 │ snapshotCache  │ 配置快照           │ evictSnapshot / evictAll │
 │               │ (provider/model)  │ configVersion 变化后自动miss│
 ├───────────────┼──────────────────┼──────────────────────────┤
 │ lastKnownGood  │ 上次成功快照(只读)  │ 永不主动清除               │
 │               │ (DB 不可用时兜底)   │ 仅在重启后丢失              │
 ├───────────────┼──────────────────┼──────────────────────────┤
 │ clientCache    │ ChatClient 实例    │ evictChatClient / evictAll│
 │ (LlmProvider   │ (含 HTTP 连接池)   │ configVersion 变化后自动miss│
 │  Registry)     │                  │                          │
 └───────────────┴──────────────────┴──────────────────────────┘
```

## 5. 当前解析顺序

chat 链路当前统一按以下顺序解析：

```text
request override -> DB runtime config -> static env config -> code default
```

其中：

- `request override` 只有在当前链路明确允许时才生效
- `DB runtime config` 用于承载场景化的 provider/model/fallback 切换
- `static env config` 仍然是重要兜底来源，例如 `app.ai.default-provider` 与 `app.ai.providers.*`
- `code default` 是最后一层保护，避免极端情况下完全无解

如果数据库配置曾经成功命中过，resolver 还会保留 `last-known-good` 快照，降低临时异常带来的抖动。

## 6. LlmProviderRegistry 的变化

`LlmProviderRegistry` 不再只是“给我 providerId，我给你 ChatClient”。

本次改造后，它在 chat 场景下增加了两个关键职责：

1. 接收 `AiRuntimeConfigSnapshot` 创建 client
2. 使用模型感知缓存避免旧 client 误复用

缓存 key 不再只与 providerId 有关，而是与以下信息共同相关：

- providerId
- clientType
- modelName
- fallbackModelName
- baseUrl
- configVersion

这样当运营或配置侧调整模型时，即便 provider 没变，也不会错误复用旧 client。

## 7. 业务链路的实际影响

### 7.1 knowledgebase

- query rewrite
- RAG 问答
- 流式回答

这些路径现在先解析 runtime snapshot，再决定走兼容网关还是标准 `ChatClient`。

### 7.2 document

`ResumeGradingService` 不再在构造期固化 chat client，而是按调用时快照取 client。

### 7.3 simulation

- `InterviewSessionService` 会在创建会话时解析 snapshot
- 评估链路也改为按 snapshot 获取 client
- ⚠️ 初始设计只把 chat 链路收敛，不扩展到 voice 实时会话切换。
  **实际情况：voice 会话级 LLM 快照已在 `v1.3.x` 后续迭代中完成。**

## 8. 对外接口是否变化

### 初始阶段：没有新增必传接口字段

本次改动的重点是后端内部的运行时控制面，而不是前端接口契约。

对前端而言，现有 `document / simulation / knowledgebase` 调用方式保持不变；变化主要体现在后端内部如何决定实际使用的 chat model。

### ✅ Phase 7：新增公开 Controller 路径

```text
GET    /api/ai-runtime-config              # 列表（domain/scene/providerId 过滤）
GET    /api/ai-runtime-config/{id}         # 详情
GET    /api/ai-runtime-config/version      # 最新版本号
POST   /api/ai-runtime-config              # 增/改配置
PATCH  /api/ai-runtime-config/{id}/enabled # 启停
POST   /api/ai-runtime-config/refresh      # 刷新缓存
```

这些接口不要求认证，前端可直接对接，详细请求/响应格式见 `api/ai-runtime-config.md`。

## 9. 测试与验证

### 初始设计阶段新增的测试

```text
src/test/java/com/ruici/ai/common/config/runtime/DefaultAiRuntimeConfigResolverTest.java
src/test/java/com/ruici/ai/common/config/runtime/DefaultAiRuntimePolicyServiceTest.java
src/test/java/com/ruici/ai/common/ai/OpenAiCompatibleGatewayClientTest.java
src/test/java/com/ruici/ai/modules/simulation/service/InterviewSessionServiceTest.java
```

### ✅ 后续迭代中补充的测试（覆盖 runtime + voice + kb + management）

```text
src/test/java/com/ruici/ai/modules/voice/service/VoiceInterviewServiceTest.java
src/test/java/com/ruici/ai/modules/voice/service/DashscopeLlmServiceTest.java
src/test/java/com/ruici/ai/modules/voice/service/VoiceInterviewEvaluationServiceTest.java
src/test/java/com/ruici/ai/modules/document/service/ResumeGradingServiceTest.java
src/test/java/com/ruici/ai/modules/knowledgebase/service/KnowledgeBaseQueryServiceTest.java
src/test/java/com/ruici/ai/common/config/runtime/AiRuntimeConfigCommandServiceTest.java
src/test/java/com/ruici/ai/common/config/runtime/AiRuntimeConfigControllerTest.java
```

### ⏳ 计划内尚未发布的测试

```text
src/test/java/com/ruici/ai/common/config/runtime/AiRuntimeConfigValidationServiceTest.java
```

该测试用例在 `private/重构v3/AI模型动态配置执行计划.md` 第 15.1 节中被列为最小门禁之一，
但在当前交付阶段尚未实现，建议后续迭代中补全。

同时，为了在 Windows + JDK 21 环境下稳定执行 Mockito 测试，补充了：

```text
src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker
```

用于显式使用 subclass mock maker，避免 inline mock maker 在本机环境下自附加失败。

已验证：

```bash
mvn -q -DskipTests -T 1 compile
mvn -q "-Dtest=LlmProviderRegistryTest,OpenAiCompatibleGatewayClientTest,DefaultAiRuntimeConfigResolverTest,DefaultAiRuntimePolicyServiceTest,InterviewSessionServiceTest" test
```

结果：当前聚焦编译与测试通过。

最新全量验证结果：

```bash
mvn "-Dtest=LlmProviderRegistryTest,KnowledgeBaseQueryServiceTest,DashscopeLlmServiceTest,VoiceInterviewServiceTest,VoiceInterviewEvaluationServiceTest,ResumeGradingServiceTest,DefaultAiRuntimeConfigResolverTest,DefaultAiRuntimePolicyServiceTest,AiRuntimeConfigCommandServiceTest,AiRuntimeConfigControllerTest" test
# Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
```

## 10. 后续演进建议

`v1.3.0` 的初始交付重点是把运行时动态配置的 **chat foundation** 落下来。

在实际迭代过程中，embedding 与 voice 的 snapshot 已提前完成接入。
当前已交付状态，与计划 `private/重构v3/AI模型动态配置执行计划.md` 的对照：

| 阶段 | 计划内容 | 当前状态 |
|---|---|---|
| Phase 0-4: Chat | simulation/document/knowledgebase 接入 resolver | ✅ 已完成 |
| Phase 5: Embedding | 任务/批次级快照 | ✅ 已完成 |
| Phase 6: Voice | 会话级 LLM 快照 | ✅ 已完成 |
| Phase 7: 管理能力 | 管理接口、启停、刷新、审计 | ✅ 已完成 |
| Phase 7.5: 前端模型选择 | 前端页面/API 层/路由/导航 | ✅ 已完成 |
| ASR/TTS 动态切换 | 按会话级快照 | ✅ 已完成 |

### 本轮补全结果

1. **`AiRuntimeConfigValidationServiceTest` 已补全**，覆盖 `chat / embedding / asr / tts` 保存校验门禁
2. **ASR/TTS 会话级 snapshot 已接入**，语音会话创建时固化 `ASR/TTS` 快照并在 WebSocket 实时链路消费
3. **多实例缓存失效传播方案已落地**，通过 Redis Topic 广播运行时缓存失效事件

### 本轮验证

```bash
mvn "-Dtest=AiRuntimeConfigValidationServiceTest,DefaultAiRuntimeConfigResolverTest,AiRuntimeConfigCommandServiceTest,VoiceInterviewServiceTest,DashscopeLlmServiceTest" test
# Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
```
