# 文档分析接口

控制器：`src/main/java/com/ruici/ai/modules/document/ResumeController.java`

## 1. 上传并分析文档

- `POST /api/documents/upload`
- `Content-Type: multipart/form-data`
- 表单字段：
  - `file`
  - `profession`（可选，职业/岗位/专业方向）
  - `analysisDifficulty`（可选，`EASY / NORMAL / SHARP`，默认 `NORMAL`）
- 用途：上传职业文档并触发异步分析（分析任务通过 Redis Stream 入队）。
- 限流：`GLOBAL=5`，`IP=5`
- 业务约束：服务层默认限制文件大小 `10MB`；允许的 `Content-Type` 由配置 `app.resume.allowed-types` 控制。
- 返回：`Result<Map<String, Object>>`
  - `duplicate=false`：返回 `resume`（含 `id/filename/analyzeStatus=PENDING`）与 `storage`（含 `fileKey/fileUrl/resumeId`）
  - `duplicate=true`：优先返回 `analysis`（历史 `DocumentAnalysisResponse`）；若无历史分析则返回 `resume` 状态 + `storage`

## 2. 获取文档列表

- `GET /api/documents`
- 用途：获取已上传文档列表。
- 关键返回字段：
  - `profession`：当前文档最近一次分析所绑定的职业/岗位/专业方向
  - `analysisDifficulty`：最近一次分析所绑定的分析力度，取值 `EASY / NORMAL / SHARP`
  - `analyzeStatus` / `analyzeError`：异步分析状态与失败原因
  - `latestScore`、`interviewCount`：用于文档列表和后续情景模拟入口展示

## 3. 获取文档详情

- `GET /api/documents/{id}/detail`
- 用途：获取文档详情和分析历史。
- 关键返回字段：
  - 顶层 `profession`
  - 顶层 `latestAnalysisDifficulty`
  - `analyses[*].analysisDifficulty`
  - `interviews[*]`：关联的情景模拟历史

## 4. 导出分析报告

- `GET /api/documents/{id}/export`
- 用途：导出文档分析报告 PDF（二进制流）。
- 返回：`application/pdf`

## 5. 删除文档

- `DELETE /api/documents/{id}`
- 用途：删除文档及其关联记录。

## 6. 重新分析

- `POST /api/documents/{id}/reanalyze`
- 用途：手动触发重新分析。
- 兼容扩展：支持可选查询参数 `profession`、`analysisDifficulty`，用于按新的分析视角重新触发分析。
- 前端 2.0 约定：详情页会把用户当前输入的 `profession` 与 `analysisDifficulty` 直接透传给该接口，用于覆盖本次重分析视角。
- 限流：`GLOBAL=2`，`IP=2`

## 7. 预览/下载原始上传文件

- `GET /api/documents/{id}/original-file`
- 查询参数：
  - `disposition`（可选，`inline` / `attachment`，默认 `inline`）
- 用途：获取用户上传的原始文件（非解析后文本），支持浏览器内联预览或下载。
- 返回：二进制流（`ResponseEntity<byte[]>`），不包裹 `Result<T>`。
- 响应头：
  - `Content-Disposition`：根据 `disposition` 参数设置 `inline` 或 `attachment`，文件名 UTF-8 编码。
  - `Content-Type`：原始文件的 MIME 类型（来自数据库 `content_type` 字段）。
  - `Content-Length`：文件字节数。
  - `X-Content-Type-Options: nosniff`
- 安全策略：
  - `text/html`、`image/svg+xml`、`application/xhtml+xml`、`text/xml` 类型强制降级为 `attachment` + `application/octet-stream`，防止浏览器执行可执行内容。
- 限流：`GLOBAL=10`，`IP=5`
- 鉴权：**当前无访问控制**（已知问题，待登录态接入后补充 ownership 校验）。
- 错误处理：
  - 文件不存在（`RESUME_NOT_FOUND`）或通用 404：返回 HTTP 404。
  - 存储服务异常（`STORAGE_DOWNLOAD_FAILED`）：返回 HTTP 502。
  - 未知异常：返回 HTTP 500。
- 剩余已知问题（详见 `word/目前已知问题-v1.3.2.md` 第 4 节）：
  - 无访问控制（待登录态接入）。
  - 大文件全量加载到内存（当前 10MB 限制下可接受）。

## 8. 健康检查

- `GET /api/documents/health`
- 用途：提供文档模块状态检查。
