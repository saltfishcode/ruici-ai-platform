---
name: ruici-project-operator
description: 面向 ruici-ai-platform 项目的任何改动都必须调用此 skill。涵盖开发、修复、重构、接口联调、文档同步与安全检查。触发词：开发、修改、修复、重构、新增功能、改代码、改配置、fix、implement、refactor、add feature、bug fix、接口变更、文档更新。
---

# Ruici Project Operator

## 触发条件（必须匹配）

只要涉及以下任一场景，**必须**调用本 skill：

- 修改/新增/删除任何业务代码或配置文件
- 修复 Bug 或功能缺陷
- 新增功能或重构
- 接口变更（路径/参数/响应/错误码）
- 文档同步（word/api/README/CLAUDE.md）
- 安全相关操作（密钥、权限、鉴权）

## 核心原则（不可违反）

1. **先思考后动手**：任何改动前必须先与开发者沟通计划，确认意图和范围后再执行。禁止未经确认就开始写代码。
2. **详细注释**：业务代码和配置文件的改动必须加入清晰注释，说明意图、边界、兼容背景，方便后续维护。
3. **必须有测试**：功能改动必须补充或更新单元测试，测试通过后才能提交。
4. **文档必须同步**：改动完成后，`word/` 和 `api/` 目录下的相关 md 必须同步更新。
5. **安全零容忍**：提交内容不得包含任何真实密钥、Token、密码等敏感信息。

---

## 启动要求（每次任务先执行）

1. 读取 `CLAUDE.md`，确认项目架构与编码规范。
2. 确认当前任务涉及模块：`document / simulation / knowledgebase / schedule / voice`。
3. 确认本地环境使用 JDK 21（默认可能是 JDK 8，需显式切换）。

---

## 项目架构速览

### 平台定位

- 泛职业文档分析 + 多场景情景模拟平台。
- 后端：Spring Boot 4 + Java 21 + Spring AI。
- 前端：React 18 + TypeScript + Vite。

### 核心业务模块

- `modules/document`：文档上传、解析、去重、异步分析、导出。
- `modules/simulation`：情景模拟会话、题目、答题、评估、导出。
- `modules/knowledgebase`：知识库上传、向量化、RAG 问答、会话。
- `modules/schedule`：邀请解析与日程管理。
- `modules/voice`：实时语音交互（ASR → LLM → TTS）与评估。

### 分层约束

- `Controller → Service → Repository`
- Controller 只做路由/校验/委托，禁止业务编排。
- Service 做业务编排与跨基础设施调用。
- Repository 专注数据访问。

---

## 操作流程（强制遵循）

### 第一步：沟通确认

- 理解开发者的意图和目标。
- 提出执行计划（涉及哪些文件、改动范围、风险点）。
- **等待开发者确认后再动手**。

### 第二步：实施改动

- 按计划修改代码，遵循下方编码规范。
- 业务代码和配置文件**必须加详细注释**（意图、边界、兼容背景）。
- 禁止无意义注释（如"给变量赋值"），但复杂逻辑、安全策略、兼容处理必须注释。

### 第三步：补充测试

- 功能改动必须补充或更新单元测试。
- 测试框架：JUnit 5 + Mockito + AssertJ。
- 测试命名用中文 `@DisplayName` 表达业务意图。
- 提交前至少执行改动相关测试并确认通过。

### 第四步：同步文档

根据改动类型同步对应文档：

| 改动类型 | 必须同步 |
|---|---|
| 接口变更（路径/参数/响应/错误码/限流） | `api/` 对应模块 md |
| 功能修复/新增 | `word/目前已知问题-{version}.md` + `word/已实现改动总结-{version}.md` |
| 重大架构变更（模块职责/Provider 路由/异步管道） | `CLAUDE.md`（功能确定迭代完成后同步） |
| 重要功能迭代/部署变化/环境变化 | `README.md`（仅重大变更时同步） |

**`word/` 目录职责说明**：
- `目前已知问题-{version}.md`：当前版本问题全景表（已解决/未解决都列出，标注状态）。
- `已实现改动总结-{version}.md`：具体实现细节（做了什么、怎么验证的）。

**`README.md` / `CLAUDE.md` 不需要频繁改动**：
- `README.md`：项目介绍与部署说明，仅在重要功能迭代、部署方式变化、环境变化时更新。
- `CLAUDE.md`：代码架构说明，仅在功能确定迭代完成后同步更新。

### 第五步：安全自检 + 提交

- 检查所有新增/修改文件是否含敏感字符串（密钥、Token、密码、内网地址）。
- 确认无泄露后再提交。
- 提交信息要准确描述改动内容，不得包含敏感信息。

---

## 编码规范（强制）

1. **注释**：业务代码和配置文件改动必须加详细注释，说明意图和边界。
2. **异常**：统一 `BusinessException(ErrorCode, message)`，禁止 `RuntimeException`。
3. **返回**：对外统一 `Result<T>`。
4. **事务**：`@Transactional` 放 Service 层，事务内禁止调外部 API。
5. **命名**：优先 `record` 做不可变数据载体，禁止直接返回 Entity 给前端。
6. **导入**：禁止通配符 import。

---

## 安全规范

- 不得在任何可提交文件中写入真实密钥、Token、密码、证书私钥。
- 敏感配置只放 `.env`（不入库），仓库只保留 `.env.example`。
- 提交前必须自检所有改动文件，确认无敏感信息泄露。
- git commit message 中也不得包含敏感信息。

---

## 当前环境（Win11）

- JDK 21 路径（提前检查）：`D:\project\javazhong\Jdk21`
- 默认 `java` 可能是 JDK 8，Maven 验证前需显式切换：

```powershell
$env:JAVA_HOME='D:\project\javazhong\Jdk21'
$env:Path="$env:JAVA_HOME\\bin;$env:Path"
java -version
```
