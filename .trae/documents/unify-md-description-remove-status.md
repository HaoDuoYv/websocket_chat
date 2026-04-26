# 统一 MD 描述 + 移除状态标签

## 需求

1. **MD 内容直接映射到功能描述**：以后所有应用都通过 `descriptionMd` 指向 md 文件，详情弹窗的"功能描述"区域直接展示 MD 渲染结果，不再需要 `description` 纯文本字段做 fallback。无 md 文件的应用不显示"功能描述"区块。
2. **移除"已完成"状态标签**：Header 中标题旁的绿色"已完成" badge 去掉。

## 实施步骤

### Step 1: AppDetailModal.vue — 改造功能描述 + 移除状态标签

**模板改动：**
- **Header 区域**：删除第 29-37 行的 `<span v-if="app?.status">` 状态标签及其关联的 `statusClass` / `statusLabel`
- **Body 功能描述区块**：
  - 条件从 `v-if="app?.description || app?.descriptionMd"` 改为仅 `v-if="app?.descriptionMd"`
  - 删除 `<p v-else>{{ app.description }}</p>` fallback 分支，只保留 `<div v-html="mdHtml">` Markdown 渲染分支

**脚本改动：**
- 删除不再使用的 `statusMap`、`statusLabel`、`statusClass` computed 属性
- 保留 `loadMarkdown` 和 `watch(app)` 逻辑不变

### Step 2: apps.ts — 清理 description 字段

- 从 AppConfig 接口中可选保留 `description`（不强制删除接口定义，保持兼容），但各应用条目中可移除冗余的 `description` 字段
- 所有应用必须配置 `descriptionMd`

### Step 3: apps.json — 同步清理后端配置

- 后端 `apps.json` 中同步移除或保留 description 均可（后端只做透传）

## 影响范围

| 文件 | 改动 |
|------|------|
| `AppDetailModal.vue` | 删除 status 标签、简化描述区块为纯 Markdown |
| `apps.ts` | 可选清理 description 字段 |
| `apps.json` | 无强需求改动 |

## 不涉及

- 后端 `AppController.java` — 无需改动
- `gomoku.md` / `play-cs.md` — 内容无需变动
