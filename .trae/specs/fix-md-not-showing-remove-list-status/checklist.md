# Checklist

- [x] 后端 apps.json 中每个条目包含 `descriptionMd` 字段
- [x] AppsPage.vue 中不存在 `statusMap`、`getStatusClass`、`getStatusLabel`
- [x] AppsPage.vue 模板中无 `<span v-if="app.status">` 状态 badge
- [x] AppConfig 接口中无 `status` 属性定义
- [x] appsConfig 数组各条目中无 `status` 属性
- [x] TypeScript 编译通过（vue-tsc --noEmit）
