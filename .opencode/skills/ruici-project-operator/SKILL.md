# Ruici Project Operator

## 适用范围

用于 `ruici-ai-platform` 项目的日常开发、Bug 修复、功能重构、接口联调与安全检查。

## 启动要求（每次任务先执行）

1. 先读取并遵循：
   - `README.md`
   - `CLAUDE.md`
2. 先确认当前任务涉及模块：`document/simulation/knowledgebase/schedule/voice`。
3. 先确认本地环境使用 JDK 21，而不是系统默认 JDK 8。

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
- `modules/voice`：实时语音交互（ASR -> LLM -> TTS）与评估。

### 分层约束

- 请求链路：`Controller -> Service -> Repository`。
- Controller 只做路由/校验/委托，不写业务编排。
- Service 做业务编排与跨基础设施调用。
- Repository 专注数据访问。

## 当前环境约束（Win11）

- 宿主机：Win11。
- 本项目可用 JDK 21 路径(不一定, 提前检查)：`D:\project\javazhong\Jdk21`。
- 默认 `java` 可能是 JDK 8，所有 Maven 验证前需显式切换。

PowerShell 推荐(不一定, 提前检查)：

```powershell
$env:JAVA_HOME='D:\project\javazhong\Jdk21'
$env:Path="$env:JAVA_HOME\\bin;$env:Path"
java -version
```

## 代码修改规范（本项目强制）

1. 注释要求
   - 注释要详细说明复杂业务意图、边界条件、兼容背景。
   - 禁止无意义注释（如“给变量赋值”）。

2. 异常与返回
   - 业务异常统一使用 `BusinessException(ErrorCode, message)`。
   - 禁止 `RuntimeException` 直抛。
   - 对外遵循统一返回体 `Result<T>`。

3. 分层与事务
   - 禁止在 Controller 写业务逻辑。
   - `@Transactional` 放 Service 层。
   - 事务内禁止调用外部 API（LLM、S3 等）。

4. 命名与实现
   - 优先 `record` 做不可变请求/传输对象。
   - 禁止直接返回 Entity 给前端。
   - 禁止通配符 import。

5. 测试要求（必须）
   - 功能改动必须补充或更新单元测试。
   - 测试框架：JUnit 5 + Mockito + AssertJ。
   - 测试命名要表达业务意图，建议用中文 `@DisplayName`。
   - 提交前至少执行改动相关测试；涉及基础能力变更时执行更广测试。

## 文档联动规则（接口/重大重构必须同步）

出现以下情况时，代码改完必须同步更新文档：

- 新增/修改网络接口（路径、请求体、响应体、错误码、限流策略）。
- 重大功能重构（业务流程、模块职责、异步管道、Provider 路由变更）。

必须同步的文档位置：

- 项目说明文档：`README.md`、`CLAUDE.md`、`word/*.md`（按主题写变更记录）。
- 接口文档目录：`api/`（用户提到 `aip` 时按 `api/` 执行并在说明中注明）。

## 安全规范（防泄露，防误推送）

1. 密钥与敏感信息
   - 不得在任何可提交文件中写入真实密钥、Token、密码、证书私钥。
   - 包括但不限于：`README.md`、`api/`、`word/`、测试代码、脚本、配置样例。

2. 非 `.gitignore` 文件严格检查
   - 对于不在 `.gitignore` 的文件，写入前必须确认不包含：
     - API Key / Access Token / Secret / 密码
     - 宿主机用户名、绝对敏感路径、内网资产信息
     - 可复用会话凭据、私钥、公网可利用配置

3. 配置管理
   - 敏感配置只放本地环境变量或 `.env`（且 `.env` 不入库）。
   - 仓库中只保留脱敏示例（如 `.env.example`）。

4. 提交前自检
   - 检查所有新增/修改文件是否含敏感字符串。
   - 若发现敏感信息，先清理再进入提交流程。

## 推荐执行清单（每次任务）

1. 阅读 `README.md` + `CLAUDE.md`，确认模块边界与规范。
2. 切换到 JDK 21，执行最小可复现验证。
3. 实施代码修改并补充对应单元测试。
4. 涉及接口/重构时同步更新 `api/` 与相关 `*.md`。
5. 做敏感信息自检，确保无泄露后再进入提交流程。
