# Tasks

- [x] Task 1: 后端 apps.json 补充 descriptionMd 字段
  - [x] 为 gomoku 和 play-cs 两个条目添加 `descriptionMd` 字段

- [x] Task 2: AppsPage.vue 删除 status 相关代码
  - [x] 删除 `statusMap` 常量（第 17-33 行）
  - [x] 删除 `getStatusClass()` 函数（第 35-39 行）
  - [x] 删除 `getStatusLabel()` 函数（第 41-44 行）
  - [x] 删除模板中卡片标题旁的 `<span v-if="app.status">` 状态 badge

- [x] Task 3: apps.ts 清理 status 字段
  - [x] 从 AppConfig 接口中删除 `status` 属性
  - [x] 从 gomoku 和 play-cs 条目中删除 `status` 属性

# Task Dependencies
- 无依赖，可并行执行
