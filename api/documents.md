# 文档分析接口

控制器：`src/main/java/com/ruici/ai/modules/document/ResumeController.java`

## 1. 上传并分析文档

- `POST /api/documents/upload`
- `Content-Type: multipart/form-data`
- 表单字段：`file`
- 用途：上传职业文档并直接返回分析结果。
- 说明：若命中重复文档，会返回历史分析结果。
- 说明：当前文档分析响应模型已经收敛到 `modules/document/model/DocumentAnalysisResponse`，不再依赖 simulation 模块中的历史 DTO。

## 2. 获取文档列表

- `GET /api/documents`
- 用途：获取已上传文档列表。

## 3. 获取文档详情

- `GET /api/documents/{id}/detail`
- 用途：获取文档详情和分析历史。

## 4. 导出分析报告

- `GET /api/documents/{id}/export`
- 用途：导出文档分析报告 PDF。

## 5. 删除文档

- `DELETE /api/documents/{id}`
- 用途：删除文档及其关联记录。

## 6. 重新分析

- `POST /api/documents/{id}/reanalyze`
- 用途：手动触发重新分析。

## 7. 健康检查

- `GET /api/documents/health`
- 用途：提供文档模块状态检查。
