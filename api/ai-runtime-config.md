# AI 运行时配置管理接口

控制器：`src/main/java/com/ruici/ai/common/config/runtime/AiRuntimeConfigController.java`

## 概述

提供数据库级别的 AI 模型运行时选择能力。前端可直接对接这些接口，构建"模型选择"页面。

### domain（能力域）可选值

| 值 | 说明 | 可切换的粒度 |
|---|---|---|
| `chat` | 通用对话模型 | 默认任意已配置 provider（当前为第三方 GPT 代理）；可切换 provider 与 model |
| `embedding` | 向量化模型 | 仅限 dashscope 内部的模型切换 |
| `asr` | 语音识别模型 | 仅限 dashscope 内部的模型切换 |
| `tts` | 语音合成模型 | 仅限 dashscope 内部的模型切换 |

### scene（业务场景）可选值

`global` / `simulation` / `knowledgebase` / `voice` / `document`

### domain × scene 适用示例

| domain | scene | 作用于 |
|---|---|---|
| chat | global | 全局聊天默认（兜底） |
| chat | simulation | 情景模拟题目生成与评估 |
| chat | knowledgebase | 知识库 RAG 问答与改写 |
| chat | document | 文档分析评分 |
| embedding | knowledgebase | 知识库向量化与检索 |
| embedding | global | 全局向量化默认（兜底） |
| asr | voice | 语音识别 |
| tts | voice | 语音合成 |

---

## 1. 查询配置列表

- `GET /api/ai-runtime-config`
- 查询参数（均为可选）：
  - `domain`：按能力域过滤
  - `scene`：按业务场景过滤
  - `providerId`：按 Provider 过滤
- 用途：获取所有 AI 运行时配置列表，支持组合过滤。
- 返回：`Result<List<AiRuntimeConfigListItemDTO>>`

## 2. 获取配置详情

- `GET /api/ai-runtime-config/{id}`
- 路径参数：`id` 配置主键
- 用途：获取单条配置的详细字段。
- 返回：`Result<AiRuntimeConfigDetailDTO>`

## 3. 获取最新版本号

- `GET /api/ai-runtime-config/version`
- 用途：获取所有配置中的最大版本号，前端可据此判断是否需要重新拉取配置。
- 返回：`Result<Long>`

## 4. 新增或修改配置

- `POST /api/ai-runtime-config`
- 请求体 `SaveAiRuntimeConfigRequest`：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | Long | 修改时必填 | 新增时传 null |
| `configKey` | String | 是 | 逻辑配置键，如 `THIRD_PARTY_MODEL` |
| `domain` | String | 是 | `chat` / `embedding` / `asr` / `tts` |
| `scene` | String | 是 | `global` / `simulation` / `knowledgebase` / `voice` / `document` |
| `providerId` | String | 否 | Provider 标识，如 `third-party` / `dashscope` |
| `modelName` | String | 是 | 模型名，如 `gpt-5.2` / `text-embedding-v3` |
| `fallbackModelName` | String | 否 | 回退模型名（仅 chat 域生效） |
| `enabled` | Boolean | 是 | 是否启用 |
| `priority` | Integer | 否 | 优先级，默认 100 |
| `remark` | String | 否 | 业务说明 |

- 查询参数（均为可选）：
  - `operator`：操作人标识，默认 `api`
- 用途：新增或更新配置。更新时自动递增 `configVersion`，触发下游缓存失效。
- 返回：`Result<Long>`（配置主键）

## 5. 启用或禁用配置

- `PATCH /api/ai-runtime-config/{id}/enabled?enabled=true`
- 路径参数：`id` 配置主键
- 查询参数：
  - `enabled`：必填，`true` 启用 / `false` 禁用
  - `operator`：操作人标识，默认 `api`
- 用途：开关配置。禁用后解析器自动按优先级降级到下一条可用配置。
- 返回：`Result<Void>`

## 6. 刷新运行时缓存

- `POST /api/ai-runtime-config/refresh`
- 查询参数：
  - `operator`：操作人标识，默认 `api`
- 用途：清空解析器快照缓存与所有 ChatClient 缓存，下次请求重新读取 DB。
- 返回：`Result<RefreshAiRuntimeConfigResponse>`
  - `message`：`"缓存已刷新"`
  - `latestConfigVersion`：当前最大版本号

## 安全说明

该模块仅存储非敏感控制信息（provider/model/scene/domain 等），**不存储 API Key 或认证凭据**。
所有密钥仍然通过 `application.yml` / `.env` 静态配置，不在运行时接口中暴露或修改。
