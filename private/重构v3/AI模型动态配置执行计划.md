# AI 模型动态配置执行计划（V3.1 - 可直接执行版）

> 快速执行入口：先看 `private/重构v3/00-AI执行入口-简版.md`。
> 本文是 V3 的收敛版，目标不是“设计更完整”，而是“本仓库现在就能按阶段实施”。

## 1. 本版结论

- 本方案结论从 V3 的“可落地”收敛为：**Chat 先落地，Embedding 第二阶段，Voice 第三阶段**。
- 本版不再把“数据库动态配置”理解成“所有链路立即热切换”。
- 本版先解决当前仓库的三个真实阻塞点：
  - 部分 Service 在构造期固化 `ChatClient`
  - `LlmProviderRegistry` 缓存键过粗且无失效通道
  - `OpenAiCompatibleGatewayClient` 仍是独立硬编码执行分支

## 2. 目标

- 将当前依赖 `.env` / `application.yml` / `@ConfigurationProperties` 的 AI 模型选择，升级为 **数据库动态配置优先 + 静态配置兜底** 的运行时解析机制。
- 在 **不破坏现有聊天、知识库、文档分析、语音、向量化链路稳定性** 的前提下，分阶段实现动态模型切换。
- 明确本仓库当前真正可执行的范围：
  - **第一阶段只落聊天域（chat）**
  - Embedding 只做任务/批次级快照
- Voice 只做会话启动时 LLM 快照
- 为项目成员提供一份可直接执行的文档，覆盖：
  - 数据库与控制面结构如何落地
  - Java 解析层与缓存层怎么改
  - 哪些链路先改，哪些链路后改
  - 每阶段的通过标准与禁止项

## 3. 非目标

- 不做“一次性重写全部 AI 链路”的大爆炸改造。
- 不在本阶段把密钥写入数据库；数据库只管理 **非敏感运行时选择信息**。
- 不在本阶段开放“所有接口都允许请求级动态切换 provider”。
- 不把 Voice、ASR、TTS、Embedding 做成“每次请求都可任意热切换”的模式。
- 不把 Hibernate 自动补表作为新增控制面关键表的唯一保障。
- 不引入 Spring Cloud Config / 配置总线 / 外部刷新框架来完成本次改造。

## 4. 当前仓库事实（必须承认，不可回避）

### 4.1 配置与部署事实

- `src/main/resources/application.yml` 当前使用：
  - `spring.jpa.hibernate.ddl-auto=update`
  - `spring.ai.vectorstore.pgvector.initialize-schema=true`
- `src/main/resources/application-dev.yml` 继续保持 `ddl-auto=update`。
- `docker/postgres/init.sql` 当前已同步承载：
  - `CREATE EXTENSION IF NOT EXISTS vector;`
  - AI 运行时控制面表初始化
  - 必要索引、约束与种子数据补齐
- 仓库当前 **没有 Flyway / Liquibase 依赖与配置**。

### 4.2 运行时 AI 能力事实

- **聊天链路**：`LlmProviderRegistry` 基于 `providerId` 构建并缓存 `ChatClient`。
- **知识库文本问答**：`KnowledgeBaseQueryService` 当前在构造期就拿到 `chatClient`，不是按次解析。
- **文档分析文本链路**：`ResumeGradingService` 当前在构造期拿默认 `ChatClient`。
- **知识库/向量化**：当前底层 `EmbeddingModel` / `VectorStore` 更接近启动期注入能力。
- **语音 ASR/TTS**：`QwenAsrService`、`QwenTtsService` 当前在构造时从 `VoiceInterviewProperties` 读取模型配置。
- **第三方 OpenAI-compatible 网关**：`OpenAiCompatibleGatewayClient` 仍按硬编码 provider 分支工作，不是统一 resolver 入口。
- **现有缓存键不足以支持同 provider 下动态切 model**，因为当前主要按：
  - `providerId`
  - `providerId:plain`
  - `providerId:voice`
  缓存。

### 4.3 这意味着什么

- 方案能做，但不能把“写入 DB”误认为“运行时立即生效”。
- 若不先改 Service 获取 `ChatClient` 的时机，Resolver 即使写出来也接不进去。
- 若不统一 Gateway 分支，本次改造会形成“两套模型选择真相”。

## 5. 最终设计结论（V3.1 必须遵守）

### 5.1 总体裁决

- **本方案是 GO WITH CHANGES，不是原样 GO。**
- **数据库动态配置** 是主路径，但 **Chat-first** 是唯一安全入口。
- 本次实施顺序必须收敛为：
  - `Chat Resolver 化`
  - `Registry 缓存模型感知化`
  - `Gateway 同口径`
  - `Embedding 批次级快照`
  - `Voice 会话级 LLM 快照`

### 5.2 统一解析原则

- 业务模块不允许自己拼接：`请求 > DB > env > default`。
- 必须统一通过：
  - `AiRuntimeConfigResolver`
  - `AiRuntimeConfigSnapshot`
  - `AiRuntimePolicyService`
- 解析结果必须显式标记来源：
  - `REQUEST_OVERRIDE`
  - `DB_RUNTIME_CONFIG`
  - `ENV_CONFIG`
  - `LAST_KNOWN_GOOD`
  - `CODE_DEFAULT`

### 5.3 快照原则

- 单次聊天请求必须使用 **同一份不可变快照**。
- 单个异步任务批次必须使用 **同一份不可变快照**。
- 单个语音会话必须使用 **同一份不可变 LLM 快照**。
- 本版快照至少区分：
  - `chat`
  - `embedding`
  - `asr`
  - `tts`

> 当前仓库实际落地状态（截至 `v1.3.0` 当前重构批次）：
>
> - `chat`：已落地
> - `embedding`：已按任务级 / 查询级快照落地到知识库向量化与检索
> - `voice`：已按**会话级 LLM 快照**落地到实时对话与异步评估
> - `asr/tts`：仍保持静态稳定配置，不在本阶段接入动态热切换

### 5.4 本地刷新原则

- 数据库是 **配置刷新源**，不是高热链路每次请求前台依赖。
- 本版不假设外部配置总线。
- 刷新策略必须是：
  - 优先命中进程内快照 / 缓存
  - 缺失或版本变化时再查库
  - 配置更新后仅局部失效受影响 key
- 若需要多实例一致性：
  - 第一阶段至少提供“显式刷新 + 版本感知”方案
  - 事件广播 / 轮询可作为后续增强，但不是第一阶段阻塞项

## 6. 数据库建表与维护策略

### 6.1 明确结论

- **Java Entity 要写。**
- **正式 SQL 初始化 / 补偿脚本也要写。**
- 对新的控制面表（如 `ai_runtime_config`、`ai_runtime_config_audit`），**不能只把成功寄托在 JPA `ddl-auto=update` 上**。

### 6.2 环境策略

#### 本地开发

- 可继续保留 `ddl-auto=update`，用于现有业务表开发提速。
- 新控制面表仍优先通过 SQL 初始化脚本创建。
- 如果开发环境数据库缺失结构，可手动执行补偿脚本。

#### Docker 本地联调 / 共享测试环境

- PostgreSQL 初始化脚本优先完成：
  - `vector` 扩展创建
  - AI 运行时配置表创建
  - 必要索引、约束、种子数据补齐
- 后端可以继续保持当前 `ddl-auto=update` 配置。

#### 正式部署 / 生产

- 当前阶段**不强制立即修改 `ddl-auto` 策略**。
- 但上线前必须先执行正式 SQL / 补偿脚本，确保控制面表、索引、约束和种子数据齐全。

## 7. 数据模型设计

### 7.1 必备表

本阶段至少引入两张表：

1. `ai_runtime_config`
2. `ai_runtime_config_audit`

### 7.2 `ai_runtime_config` 字段约束

- `config_key`：兼容历史入口键名，例如 `THIRD_PARTY_MODEL`、`AI_EMBEDDING_MODEL`
- `domain`：`chat` / `embedding` / `asr` / `tts`
- `scene`：`global` / `simulation` / `knowledgebase` / `voice` / `document`
- `provider_id`：实际提供方标识
- `model_name`：主模型名
- `fallback_model_name`：仅对支持 fallback 的聊天链路生效
- `enabled`：是否启用
- `priority`：同范围多条配置的优先级
- `config_version`：版本号，用于快照失效与并发控制
- `remark`：业务说明，不允许写敏感数据
- `updated_by` / `updated_at` / `created_at`

### 7.3 审计表原则

- 必须记录：
  - 操作者
  - 变更类型
  - 关键字段前后值摘要
  - 变更时间
- 不记录真实密钥与敏感凭据。

## 8. Java 代码设计要求

### 8.1 Entity / Repository

- `AiRuntimeConfigEntity` 保留，用于 JPA 查询与映射。
- 增加 `AiRuntimeConfigAuditEntity`。
- Repository 层只做查询和持久化，不在 Repository 中拼接业务优先级逻辑。

### 8.2 Resolver / Service 分工

- `AiRuntimeConfigResolver`
  - 负责统一解析优先级与来源标记
- `AiRuntimeConfigQueryService`
  - 配置查询
- `AiRuntimeConfigCommandService`
  - 新增 / 修改 / 启停 / 显式刷新
- `AiRuntimeConfigValidationService`
  - 保存前预检
- `AiRuntimePolicyService`
  - 维护模块、clientType、domain 与 allowed provider/model 的策略矩阵

### 8.3 新增一条必须补的实现约束

- **业务 Service 不允许在构造期长期持有将来会动态变化的 `ChatClient`。**
- 允许持有的是：
  - Resolver / Factory
  - Snapshot Key 生成器
  - 不变依赖
- 不允许继续新增：
  - `this.chatClient = registry.getChatClient...` 这种构造期固化模式

### 8.4 包边界要求

- 平台级解析与快照能力可放 `common`。
- 后台管理接口、审计查询、命令服务建议落在独立业务模块或管理子包中。
- 不建议把“管理端业务接口”全部塞进 `common`。

## 9. 缓存与 `ChatClient` 设计

### 9.1 当前风险

- 当前 `LlmProviderRegistry` 只按 `providerId` 和模式缓存。
- 如果数据库把同 provider 的 model 从 `gpt-5.2` 改为 `gpt-5.2-mini`，旧缓存仍可能被复用。
- 当前没有标准的局部失效入口。

### 9.2 新缓存键要求

缓存键至少包含：

- `providerId`
- `clientType`（default/plain/voice）
- `modelName`
- `fallbackModelName`（仅聊天）
- `baseUrl` 或不可变配置签名
- `configVersion`

### 9.3 失效策略

- 数据库配置更新后：
  - 只失效受影响的 key
  - 不做全量粗暴清空
- 请求级覆盖若允许缓存：
  - 必须限制 TTL / LRU，避免缓存无限增长
- 语音链路不走“高频多变缓存”模式，优先会话级固定快照

### 9.4 第一阶段必须接受的现实

- 第一阶段的“动态”不等于“所有实例实时广播一致”。
- 第一阶段只要求：
  - 单实例行为正确
  - 同实例版本切换正确
  - 可通过显式刷新或版本检查使本地缓存感知变更

## 10. 各链路落地方式

### 10.1 聊天链路（第一阶段，唯一先行范围）

- 适用模块：
  - `simulation`
  - `knowledgebase` 文本问答
  - `document` 文本分析
- 行为：
  - 解析有效快照
  - 按快照创建或命中 `ChatClient`
  - 若请求显式允许 override，则在策略白名单通过后覆盖 DB 默认配置
- 第一阶段必须额外完成：
  - 拆除构造期固化 `ChatClient` 的调用方式
  - 将 `OpenAiCompatibleGatewayClient` 纳入同一解析口径

### 10.2 Embedding / 向量化链路（第二阶段）

- 原则：
  - 动态的是“本批任务用哪个 embedding model”
  - 不是每个 chunk 都重读配置
- 限制：
  - 如果切换模型会影响向量维度或历史库兼容性，必须标记为受控刷新
  - 必须明确“是否要求重新向量化”
- 若这一点在实施前无法回答：
  - 第二阶段不得开工

### 10.3 语音 ASR / TTS 链路（第三阶段）

- 原则：
  - 新会话启动时解析 ASR/TTS 快照
  - 已建立会话继续沿用旧快照
- 禁止：
  - 每个音频分片前查库
  - 进行中的会话中途漂移 model/provider
- 若现有 SDK 只能构造期固定：
  - 先通过“新会话创建前解析并持久化快照字段”落地
  - 不强求第三阶段实现真正运行中热切换

## 11. 统一解析口径

### 11.1 解析优先级

1. 安全策略 / 白名单校验
2. 请求显式覆盖（仅允许接口）
3. 本地快照 / 缓存命中
4. 数据库动态配置
5. `.env` / `application.yml` / `@ConfigurationProperties`
6. 代码默认值

### 11.2 失败兜底原则

- 如果数据库不可用：
  - 优先返回 `last-known-good` 快照
  - 若尚无快照，则回退到静态配置
  - 仍不可用时才回退到代码默认值
- 必须记录结构化日志，说明本次实际命中来源。

### 11.3 明确禁止项

- 禁止业务模块自行实现解析优先级。
- 禁止每次 AI 调用前同步查库。
- 禁止非法请求覆盖静默生效。
- 禁止 Gateway / Registry / Voice 各自维护不同口径的 model 选择逻辑。

## 12. 实际业务执行流程说明

### 12.1 数据库与数据表生成流程

#### 本地新环境

1. 创建数据库 `ruici_ai_platform`
2. 优先执行 `docker/postgres/init.sql`
3. 仅在需要手工补齐已有数据库时，参考执行 `04-AI运行时配置初始化与补偿脚本.sql`
4. 启动后端
5. JPA 可继续为历史业务表做 `update` 补齐

#### Docker 首次启动

1. PostgreSQL 容器创建数据库
2. `/docker-entrypoint-initdb.d/` 下的 `docker/postgres/init.sql` 自动执行
3. 自动创建 `vector` 扩展与 AI 运行时控制面表，并补齐索引、约束、种子数据
4. 启动应用

#### 老库升级 / 部署

1. 先备份数据库
2. 手工执行运行时配置补偿 SQL
3. 校验表、索引、约束、种子数据
4. 再启动新版本后端

### 12.2 AI 模型在不同情况下的实际调用

#### 场景 A：没有请求覆盖，数据库有配置

- 解析结果：数据库配置生效
- 示例：
  - `simulation/chat/global -> third-party / gpt-5.2`

#### 场景 B：没有请求覆盖，数据库无配置

- 回退到 `.env` / `application.yml`
- 例如：
  - `THIRD_PARTY_MODEL`
  - `AI_EMBEDDING_MODEL`
  - `AI_ASR_MODEL`

#### 场景 C：请求覆盖被允许

- 前提：
  - 当前接口允许 override
  - 请求中的 provider/model 在策略白名单内
- 生效方式：
  - 本次请求优先使用请求参数
  - 不回写数据库

#### 场景 D：数据库暂时不可用

- 若已有快照：使用 `last-known-good`
- 若无快照：回退到静态配置
- 若静态配置也缺失：回退到代码默认值并告警

## 13. 项目 AI 提供商结构说明

### 13.1 当前推荐结构

- **聊天域 provider**
  - 负责：
    - 文档分析文本推理
    - 知识库文本问答
    - 情景模拟文本生成
  - 动态变化重点：**模型名**

- **向量域 provider**
  - 负责：
    - Embedding / Vector Store
  - 动态变化重点：同 provider 下 embedding model

- **语音域 provider**
  - 负责：
    - ASR
    - TTS
    - Voice Chat
  - 运行特征：会话敏感、长连接敏感，优先固定 provider

### 13.2 设计原则

- 优先固定 provider 的职责边界
- 在 provider 内部动态切 model
- 不建议一开始就做“各模块随意切 provider”的自由模式

## 14. 实施顺序（V3.1 强制版）

### Phase 0：补设计，不写业务接入

必须完成：

1. 确认第一阶段只做 `chat`
2. 明确哪些 Service 当前构造期固化 `ChatClient`
3. 明确 `OpenAiCompatibleGatewayClient` 如何接入统一 resolver
4. 明确本地缓存失效 / 版本检查策略

通过标准：

- 文档里明确写出“先改调用时机，再接 resolver”
- 所有参与人知道第一阶段 **不碰 voice / embedding 业务接入**

### Phase 1：SQL 与控制面结构

1. 补齐正式 SQL 设计与补偿脚本，并同步到 `docker/postgres/init.sql`
2. 增加控制面 Entity / Repository / 审计结构
3. 保证老库补偿可重复执行

通过标准：

- 新库可初始化
- 老库可补偿
- 表、索引、约束、种子数据齐全

### Phase 2：Resolver / Snapshot / Policy

1. 实现 `Resolver + Snapshot + PolicyService`
2. 实现 `last-known-good` 与静态配置兜底
3. 提供显式刷新或版本检查入口

通过标准：

- Resolver 单测通过
- Policy 单测通过
- DB 不可用时可命中 `last-known-good`

### Phase 3：Registry 与 ChatClient 生命周期改造

1. 改造 `LlmProviderRegistry` 缓存键与失效策略
2. 改造构造期固化 `ChatClient` 的 Service
3. 统一 Gateway 与 Registry 的模型选择口径

通过标准：

- 同 provider 不同 model 不串缓存
- configVersion 变化后只失效受影响 key
- Service 不再在构造期持有会动态变化的 `ChatClient`

### Phase 4：只接聊天链路

1. 接入 `simulation`
2. 接入 `knowledgebase` 文本问答
3. 接入 `document` 文本分析

通过标准：

- DB 配置优先于静态配置
- 请求覆盖只在允许接口生效
- 未启用动态配置时行为不变

### Phase 5：Embedding（第二阶段）

前置条件：

- 已明确是否需要重新向量化
- 已明确维度变化与历史库兼容策略

执行内容：

1. 只做批次级快照
2. 禁止 chunk 级查库

### Phase 6：Voice（第三阶段）

前置条件：

- 已明确语音会话创建时如何固化快照
- 已明确旧会话不漂移的验证方案

执行内容：

1. 新会话读新快照
2. 老会话沿用旧快照

### Phase 7：管理能力与文档收口

1. 补管理接口、启停、刷新、审计
2. 补接口文档与运行说明
3. 补测试报告模板与上线 checklist

## 15. 测试计划（强制）

### 15.1 第一阶段最小门禁

- `AiRuntimeConfigResolverTest`
- `AiRuntimePolicyServiceTest`
- `AiRuntimeConfigValidationServiceTest`
- `LlmProviderRegistry` 模型感知缓存测试
- `OpenAiCompatibleGatewayClient` fallback / 统一入口测试
- `simulation` / `knowledgebase` / `document` 三条聊天链路集成测试
- Resolver 阶段已验证 `last-known-good` / 静态配置 / 默认值回退链路

### 15.2 第二阶段门禁

- Embedding 批次级解析测试
- 向量模型切换兼容性验证
- 是否需要重向量化的回归验证

### 15.3 第三阶段门禁

- 新语音会话读新配置
- 老语音会话不漂移
- ASR/TTS 不按分片查库

### 15.4 回归测试

- 未启用动态配置时功能行为不变
- 高热路径不出现每次请求查库
- `simulation` / `knowledgebase` / `voice` 不串读配置

## 16. 风险与开放问题

- Embedding 模型切换是否要求重建向量数据，需要单独确认。
- 语音 SDK 当前构造期配置能否平滑转向会话级快照，需要单独验证。
- 多实例下的缓存失效传播，第一阶段不强求彻底解决，但必须留下明确扩展点。
- 若未来需要租户级 / 用户级策略，应新增 `scope_type/scope_id`，不建议继续硬压在 `scene` 上。
- 若后续要把密钥也配置化，必须另起安全设计，不可直接落本表。

## 17. 执行失败判定（任何一条命中即暂停推进）

- 发现第一阶段还在构造期长期持有动态 `ChatClient`，但未先拆除。
- 发现 Gateway 仍绕开统一 resolver 单独选模型。
- 发现缓存键未纳入 `modelName` / `configVersion` 就开始接业务链路。
- 发现 Embedding 是否要重向量化尚未明确，却已经开始第二阶段开发。
- 发现 Voice 会话边界尚未明确，却已经开始第三阶段接入。

## 18. 交付要求

- 文档、SQL、Entity、DTO、测试代码不得包含敏感信息。
- 数据库注释、JavaDoc、接口说明必须同步。
- SQL 必须支持“初始化”和“老库补偿”两个使用场景。
- 计划文档必须能解释：
  - 表怎么来
  - 表什么时候建
  - 第一阶段为什么只做 chat
  - Resolver 为什么必须先于业务接入
  - 失败时如何兜底
