# Vue阈值配置页生成 提示词演化

## v1 — 初始版本

```
写一个 Vue 页面，显示阈值配置，可以用表格展示功能区、时段、阈值的对应关系，支持增删改。
```

## 问题与不足

v1 产物是一个单表格页面，缺少功能区多 Tab 切换（图书馆/食堂/操场/宿舍），没有自适应阈值和混合模型的可视化。表格直接用 el-table 展示 JSON 数据，没有关联 API 调用。增删改用 `prompt()` 和 `confirm()` 原生弹窗而非 Element Plus 的 el-dialog。代码使用 Options API 写法，不符合 CLAUDE.md 规定的 `<script setup>` 组合式 API。

## v2 — 改进版本

```
基于 docs/API_DESIGN.md 阈值接口 + docs/TECH_DESIGN.md §6 ThresholdConfigPage 原型，生成阈值配置页面。

功能要求：
1. 元素加 Tabs：4 个功能区 Tab（图书馆/食堂/操场/宿舍） + 自适应阈值 Tab + 混合模型 Tab
2. 每个功能区 Tab 内：el-table 展示该功能区的阈值规则（时段、阈值 dB、状态、操作），支持 el-dialog 新增/编辑
3. 自适应阈值 Tab：展示滑动窗口参数卡片（窗口大小、k 值、均值、标准差、上下限），含刷新按钮
4. 混合模型 Tab：运行状态面板（当前模式标签、异常率进度条、触发条件状态）+ 性能指标卡片

技术约束：<script setup> + Element Plus 2.13.7 + axios 请求走 src/api/threshold.js、管理员权限判断用 isAdmin ref
```

## 改进效果

v2 实现了 6 个 Tab（4 功能区 + 自适应 + 混合），Element Plus 组件使用正确。但混合模型 Tab 缺少 ECharts 折线图（P1-3 要求），自适应阈值卡片在 `windowRecordCount == 0` 时展示混乱。管理员按钮逻辑散落在模板里通过 `v-if="isAdmin"` 判断，没有抽出 `usePermission` composable。5 个 Tab 共用一个 `fetchData` 导致首次加载全部请求同时发出。

## v3 — 优化版本

```
基于 docs/TECH_DESIGN.md §6 ThresholdConfigPage + docs/API_DESIGN.md 阈值/统计接口 + CLAUDE.md §三 前端规范，生成阈值配置页。

硬约束：
1. <script setup> 组合式 API，const/let 禁止 var，单引号加分号
2. 路由：/threshold-config，组件名 ThresholdConfigPage.vue，放 src/views/
3. 5 个 Tab（4 功能区 + 自适应 + 混合），每个 Tab 懒加载（watch activeTab 切换时才发请求）
4. 功能区 Tab：el-table 列 = 时段/阈值dB/状态(el-switch)/操作(编辑/删除)，el-dialog 表单含 rules 校验
5. 自适应 Tab：el-card 栅格 2 列展示各功能区 μ±kσ 参数，空状态用 el-empty 组件
6. 混合模型 Tab：上方 el-descriptions 运行状态 + 中部 ECharts 折线图（近 24h 分贝曲线 + 阈值线 + 异常散点标注）+ 下方异常率 el-progress
7. 权限控制：抽出 usePermission composable → isAdmin computed，按钮显隐统一用 v-permission 指令
8. API 层：src/api/threshold.js 封装 7 个请求函数（按 Tab 分组注释），统一走 request.js axios 实例
9. 代码拆分：页面 ≤ 300 行 → 抽出 AdaptiveTab.vue + HybridTab.vue 子组件
10. 响应式：el-table 小屏添加横向滚动，el-col 响应式断点 xs/sm/md/lg

禁止：Options API、原生 alert/confirm、硬编码 API URL、fetchUserRole 重复调用
```

## 最终效果

v3 产出的 ThresholdConfigPage.vue 主文件 178 行 + 2 个子组件，低于 300 行上限。Tab 懒加载使首次渲染仅发 1 请求（v2 发 6 请求），页面 LCP 从 2.8s 降至 0.9s。ECharts 折线图正确标注异常散点（红色标记 + tooltip 显示分贝值）。代码评审 R-06 中该页面维度零 issue，usePermission composable 被 DashboardPage 和 StatisticsPage 复用。
