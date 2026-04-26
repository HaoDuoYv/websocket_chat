# 修复 MD 不显示 + 移除列表状态标签 Spec

## Why

1. **MD 内容不显示**：`AppsPage.vue` 通过 `GET /api/apps` 从后端获取应用列表并**完全替换**本地 `appsConfig`。但后端 [apps.json](file:///e:\study\gitproject\websocket_chat\backend\src\main\resources\apps.json) 中没有 `descriptionMd` 字段，导致弹窗收到的 app 对象缺少该字段，`v-if="app?.descriptionMd"` 为 falsy，整个"功能描述"区块不渲染。
2. **列表"已完成"标签未移除**：上轮只清理了 `AppDetailModal.vue` 中的 status 代码，遗漏了 `AppsPage.vue` 卡片列表中的 status badge 及其关联的 `statusMap`/`getStatusClass`/`getStatusLabel`。

## What Changes

- **修复 Bug**：后端 `apps.json` 补充 `descriptionMd` 字段，使接口返回数据包含该字段
- **清理代码**：`AppsPage.vue` 删除所有 status 相关代码（statusMap、getStatusClass、getStatusLabel、模板中的 badge）
- **同步清理**：`apps.ts` 接口及条目中删除 `status` 字段（已无任何消费方）

## Impact

- Affected code:
  - `backend/src/main/resources/apps.json` — 添加 `descriptionMd`
  - `frontend/src/pages/AppsPage.vue` — 删除 status 标签及相关逻辑
  - `frontend/src/config/apps.ts` — 删除 `status` 字段

## ADDED Requirements

### Requirement: 后端返回 descriptionMd 字段

后端 `GET /api/apps` 返回的应用对象 SHALL 包含 `descriptionMd` 字段。

#### Scenario: play-cs 应用有 descriptionMd
- **WHEN** 前端调用 `GET /api/apps`
- **THEN** play-cs 条目包含 `"descriptionMd": "/md/play-cs.md"`

### Requirement: 列表卡片无状态标签

应用中心列表卡片 SHALL 不显示"已完成"/"开发中"/"规划中"状态标签。

#### Scenario: 卡片渲染
- **WHEN** 用户进入应用中心页面
- **THEN** 每张卡片的标题旁无状态 badge

## REMOVED Requirements

### Requirement: AppConfig.status 字段
**Reason**: 详情弹窗和列表卡片均已不再使用 status，无消费方。
**Migration**: 从接口定义和数据条目中直接删除。
