# 场景日程接口

控制器：`src/main/java/com/ruici/ai/modules/schedule/InterviewScheduleController.java`

## 路径兼容

- 推荐新前端使用：`/api/schedule`
- 兼容旧入口：`/api/simulation-schedule`

## 兼容字段说明

- 当前请求 / 响应 DTO 仍使用 `interviewTime`、`interviewType` 等字段名。
- 这些字段现在按“场景开始时间 / 交互形式”理解，不代表只能表达面试。

## 1. 解析邀约文本

- `POST /parse`
- 请求体：
  - `rawText`
  - `source`：可选，常见值有 `feishu`、`tencent`、`zoom`
- 用途：把原始邀约文本解析成结构化排期信息。

## 2. 创建日程

- `POST /`
- 用途：创建场景日程记录。

## 3. 获取单条日程

- `GET /{id}`

## 4. 获取日程列表

- `GET /`
- 可选查询参数：
  - `status`
  - `start`
  - `end`

## 5. 更新日程

- `PUT /{id}`

## 6. 删除日程

- `DELETE /{id}`

## 7. 更新状态

- `PATCH /{id}/status`
- `PUT /{id}/status`
- 查询参数：`status`
