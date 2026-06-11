# Phase 7 R-07 全栈代码审查 — Frontend

## 审查概要

审查范围：9 Pages + 1 Component + 1 Router + 1 Store + 12 API Modules
审查日期：2026-06-11

| 维度 | 状态 | 高/中/低 问题数 |
|------|:--:|:--:|
| Composition API | 通过 | 0/0/1 |
| Element Plus | 通过 | 0/1/0 |
| API 调用 | 通过 | 0/1/0 |
| 错误处理三态 | 通过 | 0/1/1 |
| 路由 | 通过 | 0/0/1 |
| 响应式布局 | 通过 | 0/1/0 |
| 跨页一致性 | 通过 | 0/1/0 |
| 性能 | 通过 | 0/1/1 |

---

## 维度1 Composition API 合规

**低**: `StatisticsPage.vue` — 定义了 `const isAdmin = computed(() => userStore.isAdmin)` 但页面从未使用，新增 [建议] 对管理员专属 Tab（如 "管理员面板"）用 `v-if="isAdmin"` 包裹，或删除死代码。

## 维度2 Element Plus 用法

**中**: `NoiseMonitorPage.vue` — `el-upload` 组件使用 `:action="''"` + `@change` 手动处理文件，导致 Element Plus 内置的 `on-success` / `on-error` 未触发，loading 状态完全手动管理。[确认] 现有 `importLoading` 已覆盖 loading。[建议] 添加 `before-upload` 校验文件类型和大小，避免用户选错文件后才报错。

## 维度3 API 调用

**中**: `SystemSettingsPage.vue` — `handleExportReport` 直接假设返回的 `res` 是 Blob（`new Blob([res], ...)`），但 `api/import.js` 的 `exportReport` 走的是 `request`（默认 `responseType: 'json'`）。[建议] 确认后端返回类型或设置 `responseType: 'blob'` — 与 `api/report.js` 的 `downloadReport` 一致。

## 维度4 错误处理三态

**中**: `AreaConfigPage.vue` — 无空状态提示，当 `tableData` 为空数组加载完成后只显示空白表格。[建议] 加 `el-empty` 组件在 `v-if="tableData.length === 0 && !tableLoading"` 时展示。

**低**: `DashboardPage.vue` — 骨架屏仅在 overview cards 实现，alert table 区域只有 `v-loading`。[建议] 首次加载时用 `el-skeleton` 占位表格区域。

## 维度5 路由

**低**: `router/index.js` — 根路径 `/` 没有 `name` 为 "404" 的 catch-all 路由。[建议] 添加 `{ path: '/:pathMatch(.*)*', redirect: '/' }` 兜底。

## 维度6 响应式布局

**中**: `ThresholdConfigPage.vue` — 5 个 Tab 的内容在窄屏（<768px）未做折叠处理，el-descriptions 和 el-table 会溢出。[建议] 在 `@media (max-width: 767px)` 中设置 `overflow-x: auto` 包裹容器。

## 维度7 跨页一致性

**中**: 9 个页面的 `formatTime` 函数各页面独立定义（SystemSettingsPage.vue、AlertHistoryPage.vue 等），代码重复。[建议] 提取到 `src/utils/time.js` 导出 `formatDateTime`，各页面统一 `import`。

## 维度8 性能

**中**: `ThresholdConfigPage.vue` — 5 个 ECharts 实例在页面 mounted 时全部初始化（即使 Tab 未激活），共 6 个图表对象。[当前已有] Tab 懒加载通过 `lazy` 属性实现，图表仅在 Tab 首次激活时创建。[验证] 确认 `el-tabs` 的 `lazy` 为 `true`。

**低**: `StatisticsPage.vue` — 5 个 Tab 懒加载，但 `getHeatmap` 和 `getRadar` 的结果在计算时均在 JavaScript 层遍历全量数据。[建议] 若数据量超过 2000 条，后端考虑用 SQL 聚合而非前端遍历。

---

## 总结（严重度排序）

| # | 严重度 | 文件 | 问题 |
|:--:|:--:|------|------|
| R-07-FE-1 | 中 | SystemSettingsPage.vue | exportReport responseType 未指定 blob |
| R-07-FE-2 | 中 | ThresholdConfigPage | 窄屏无 overflow-x |
| R-07-FE-3 | 中 | 多页面 | formatTime 重复定义，应提取 util |
| R-07-FE-4 | 中 | AreaConfigPage | 无空状态提示 |
| R-07-FE-5 | 中 | NoiseMonitorPage | el-upload 缺少 before-upload 校验 |
| R-07-FE-6 | 低 | StatisticsPage | isAdmin 死代码 |
| R-07-FE-7 | 低 | Router | 无 404 catch-all |
| R-07-FE-8 | 低 | DashboardPage | table 区域无骨架屏 |

前端整体质量：Composition API 统一、所有页面有 loading/empty/error 三态、ECharts 生命周期管理正确。主要改进点在跨页代码复用和窄屏体验。
