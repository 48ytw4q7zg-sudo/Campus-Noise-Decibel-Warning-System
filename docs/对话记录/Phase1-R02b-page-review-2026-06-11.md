# Phase 1 R-02b 页面原型审核报告 · 2026-06-11

## 审核元数据
- 审核日期：2026-06-11
- 使用模型：Claude Opus 4.8
- 输入摘要：TECH_DESIGN.md §6，8 页面全量覆盖，PRD.md §3+§5 双向对账

---

## 审核报告

### 维度 1：§6 内部完整性

全量页面覆盖：8/8 ✅（LoginPage / DashboardPage / NoiseMonitorPage / AlertHistoryPage / AreaConfigPage / ThresholdConfigPage / StatisticsPage / SystemSettingsPage）。

- **issue-1** [严重度：中]：P0-1 GET /api/user/profile（个人信息查看）和 PUT /api/user/password（修改密码）的 UI 入口在整个 §6 中缺失
  - **位置**：TECH_DESIGN §6 LoginPage（仅含登录/注册表单）· 全局无个人信息/修改密码 UI
  - **推演**：Phase 5 vue-page-coder 发现 API 已定义但无页面原型 → 自行决定放哪 → 可能放进 LoginPage（不合理）、自家头下拉（未设计）、或遗漏
  - **修复建议**：二选一 ——（a）在新增的 `### 全局布局组件 AppLayout` 子节中，el-header 用户名区域增加下拉菜单「个人信息」「修改密码」两个入口，个人信息用 el-dialog 展示（用户名/角色/注册时间），修改密码用 el-dialog（原密码/新密码/确认密码）；或（b）新建独立的 UserProfilePage，路由 `/profile`

- **issue-2** [严重度：低]：NoiseMonitorPage 批量导入弹窗标注 "(el-dialog · P1-5)"——但 POST /api/noise/batch 是 P0-2 的 API，不是 P1-5
  - **位置**：TECH_DESIGN §6 NoiseMonitorPage ASCII 布局（第 774 行附近）
  - **修复建议**：将 `(el-dialog · P1-5)` 改为 `(el-dialog · P0-2)`

- **issue-3** [严重度：低]：StatisticsPage 字段行为表提到"模型性能对比：P1-3 柱状图：固定阈值 vs 业务规则 vs 统计自适应 vs 混合的准确率/误报率"，但 ASCII 布局中未画出对应卡片（仅有 4 个卡片：时间序列图/功能区统计/多维度分析/报告入口）
  - **位置**：TECH_DESIGN §6 StatisticsPage
  - **修复建议**：在 ASCII 布局中增加第 5 个卡片「模型性能对比」或合并到功能区统计卡片内

- **issue-9** [严重度：低]：DashboardPage 侧栏 ASCII 中写"功能区"作为菜单项，但 §3.3 AppLayout 结构 + §3.1 路由表中该菜单项名为"功能区配置"
  - **位置**：TECH_DESIGN §6 DashboardPage ASCII 布局侧栏区域
  - **修复建议**：将侧栏中"功能区"改为"功能区配置"（对齐 §3.1 路由表 AreaConfigPage 的完整名称）

### 维度 2：页面 ↔ PRD 字段对账

**参考集**（PRD §3 各功能数据约束段全部业务字段）：

| 来源 | 字段 | 用途 |
|------|------|------|
| P0-1 | username, password, role | 注册/登录 |
| P0-2 | time_point, location, decibel, device_id | 噪声数据 |
| P0-3 | is_abnormal, threshold_value, time_segment | 阈值判断 |
| P0-5 | alert_time, location, decibel, threshold, alert_type, confirm_status, confirmed_by | 告警 |
| P0-6 | area_name, noise_sensitivity, default_threshold, description, status | 功能区 |
| P1-1 | window_size, k_value | 自适应阈值参数 |
| P1-4 | location, time_segment, threshold, description, status | 阈值规则 |
| P2-2 | noise_type, confidence | AI 分类 |

**被检集**（§6 各页面引用字段）：

| §6 页面 | 引用字段 | PRD 出处 | 匹配 |
|---------|---------|---------|:---:|
| LoginPage | 用户名, 密码, 角色 | P0-1 ✅ | ✅ |
| DashboardPage | 功能区, 分贝值, 阈值, 异常状态 | P0-4 ✅ | ✅ |
| NoiseMonitorPage | 时间, 功能区, 分贝值, 异常状态, 设备ID | P0-2/P0-7 ✅ | ✅ |
| AlertHistoryPage | 时间, 功能区, 分贝值, 阈值, 告警类型, 确认状态 | P0-5 ✅ | ✅ |
| AreaConfigPage | 功能区名称, 敏感度, 默认阈值, 描述, 状态 | P0-6 ✅ | ✅ |
| ThresholdConfigPage | 功能区, 时段, 阈值, 说明, 窗口大小, k值 | P0-3/P1-1/P1-4 ✅ | ✅ |
| StatisticsPage | 时间序列/功能区分贝/异常率/告警次数 | P1-3 ✅ | ✅ |
| SystemSettingsPage | 导入文件/置信度/报告周期/ccswitch配置 | P2-1/P2-2/P2-3/P2-5 ✅ | ✅ |

**差集/对账结论**：对账通过，无差集。所有 §6 页面字段均在 PRD 中有出处。

### 维度 3：按钮 ↔ API 对账

**参考集**（PRD §3 全部已声明 API · 去重后 37 个）：

| 功能 | API | 方法 |
|------|-----|------|
| P0-1 | /api/user/register, /api/user/login, /api/user/profile, /api/user/password | POST/GET/PUT |
| P0-2 | /api/noise/record, /api/noise/batch, /api/noise/latest | POST/GET |
| P0-3 | /api/threshold/current, /api/threshold/check/{id} | GET/POST |
| P0-4 | /api/dashboard/overview, /api/dashboard/area/{location} | GET |
| P0-5 | /api/alert/list, /api/alert/{id}, /api/alert/{id}/confirm, /api/alert/{id}/resolve | GET/PUT |
| P0-6 | /api/area/list, /api/area/{id} | GET/PUT |
| P0-7 | /api/noise/list, /api/noise/{id} | GET |
| P1-1 | /api/threshold/adaptive/current, /api/threshold/adaptive/config | GET/PUT |
| P1-2 | /api/threshold/hybrid/status, /api/threshold/hybrid/performance | GET |
| P1-3 | /api/statistics/timeseries, /api/statistics/area-summary, /api/statistics/model-performance | GET |
| P1-4 | /api/threshold/rule/list, /api/threshold/rule, /api/threshold/rule/{id}, /api/threshold/rule/reload | GET/POST/PUT/DELETE/POST |
| P1-5 | /api/noise/advanced-list, /api/noise/export | GET |
| P2-1 | /api/data/import, /api/data/export-report | POST/GET |
| P2-2 | /api/ai/classify, /api/ai/config | POST/PUT |
| P2-3 | /api/report/list, /api/report/{id}, /api/report/generate, /api/report/config | GET/POST/PUT |
| P2-4 | /api/statistics/multi-dim, /api/statistics/heatmap, /api/statistics/radar | GET |
| P2-5 | /api/ccswitch/status, /api/ccswitch/reload | GET/POST |

**正向差集**（§6 UI 按钮 → 无对应 API）：

| UI 按钮 | 位置 | PRD API 对账 | 判定 |
|---------|------|------------|:---:|
| LoginPage [登录] | LoginPage §6 | → POST /api/user/login ✅ | ✅ |
| LoginPage [注册] | LoginPage §6 | → POST /api/user/register ✅ | ✅ |
| DashboardPage 功能区卡片点击 | DashboardPage §6 | → GET /api/dashboard/area/{location} ✅ | ✅ |
| NoiseMonitorPage [手动录入] | NoiseMonitorPage §6 | → POST /api/noise/record ✅ | ✅ |
| NoiseMonitorPage [批量导入] | NoiseMonitorPage §6 | → POST /api/noise/batch ✅ | ✅ |
| NoiseMonitorPage [查询] | NoiseMonitorPage §6 | → GET /api/noise/list ✅ | ✅ |
| NoiseMonitorPage [详情] | NoiseMonitorPage §6 | → GET /api/noise/{id} ✅ | ✅ |
| NoiseMonitorPage [重新判断] | NoiseMonitorPage §6 | → POST /api/threshold/check/{id} ✅ | ✅ |
| AlertHistoryPage [确认] | AlertHistoryPage §6 | → PUT /api/alert/{id}/confirm ✅ | ✅ |
| AlertHistoryPage [处置] | AlertHistoryPage §6 | → PUT /api/alert/{id}/resolve ✅ | ✅ |
| AreaConfigPage [编辑] | AreaConfigPage §6 | → PUT /api/area/{id} ✅ | ✅ |
| ThresholdConfigPage [新增规则] | ThresholdConfigPage §6 | → POST /api/threshold/rule ✅ | ✅ |
| ThresholdConfigPage [重载配置(ccswitch)] | ThresholdConfigPage §6 | → POST /api/threshold/rule/reload ✅ | ✅ |
| ThresholdConfigPage [保存配置] | ThresholdConfigPage §6 | → PUT /api/threshold/adaptive/config ✅ | ✅ |
| SystemSettingsPage [上传并导入] | SystemSettingsPage §6 | → POST /api/data/import ✅ | ✅ |
| SystemSettingsPage [导出Excel报表] | SystemSettingsPage §6 | → GET /api/data/export-report ✅ | ✅ |
| SystemSettingsPage [手动触发分类] | SystemSettingsPage §6 | → POST /api/ai/classify ✅ | ✅ |
| SystemSettingsPage [保存配置]（AI） | SystemSettingsPage §6 | → PUT /api/ai/config ✅ | ✅ |
| SystemSettingsPage [手动生成报告] | SystemSettingsPage §6 | → POST /api/report/generate ✅ | ✅ |
| SystemSettingsPage [重载配置]（ccswitch） | SystemSettingsPage §6 | → POST /api/ccswitch/reload ✅ | ✅ |
| StatisticsPage [查看报告列表] | StatisticsPage §6 | → GET /api/report/list ✅ | ✅ |

**正向差集结论**：全部 UI 按钮均有对应 PRD API，无孤儿按钮 ✅

**反向差集**（PRD 已声明 API → §6 无 UI 入口）：

| API | 归属 | §6 缺失 |
|-----|------|--------|
| GET /api/user/profile | P0-1 | 无个人信息查看 UI |
| PUT /api/user/password | P0-1 | 无修改密码 UI |
| GET /api/user/profile + PUT /api/user/password | — | **= issue-1（已记录）** |

其余 35 个 API 均有 §6 UI 入口。反向差集仅 2 个，与 issue-1 重叠。✅

**实现优先级一致性**：全部 P0 页面按钮对应 P0 API ✅。ThresholdConfigPage P0 仅引用 GET /api/threshold/current（P0-3）· P1 面板引用 P1 API ✅。

### 维度 4：页面 ↔ §3 路由对账

| §6 页面 | §6 URL | §3 URL | URL 匹配 | 优先级匹配 | 跳转目标存在 | 被引用为跳转目标 |
|---------|--------|--------|:---:|:---:|:---:|:---:|
| LoginPage | `/login` | `/login` | ✅ | P0=P0 ✅ | 跳 `/`✅ /login ✅ | ✅（DashboardPage 点击退出 → /login） |
| DashboardPage | `/` | `/` | ✅ | P0=P0 ✅ | 跳 /noise-monitor /alert-history /login ✅ 4 个目标全部存在 ✅ | ✅（登录成功 → /） |
| NoiseMonitorPage | `/noise-monitor` | `/noise-monitor` | ✅ | P0=P0 ✅ | — 本页自刷新 | ✅（Dashboard 卡片 + 侧栏跳入） |
| AlertHistoryPage | `/alert-history` | `/alert-history` | ✅ | P0=P0 ✅ | — 本页自刷新 | ✅（侧栏跳入） |
| AreaConfigPage | `/area-config` | `/area-config` | ✅ | P0=P0 ✅ | 非管理员→/ | ✅（侧栏管理员跳入） |
| ThresholdConfigPage | `/threshold-config` | `/threshold-config` | ✅ | P0+P1=P0+P1 ✅ | 跳 / | ✅（侧栏管理员跳入） |
| StatisticsPage | `/statistics` | `/statistics` | ✅ | P1=P1 ✅ | 告警详情弹窗 /settings ✅ | ✅（侧栏跳入） |
| SystemSettingsPage | `/settings` | `/settings` | ✅ | P2=P2 ✅ | — 本页操作 | ✅（侧栏+StatisticsPage→/settings） |

**对账结论**：8/8 URL 完全匹配，优先级全部一致，无断链，无孤儿页。

- **issue-4** [严重度：低]：StatisticsPage §6 中"查看报告列表"按钮跳转目标写"`/settings`（报告区域）或本页报告 tab"——§3 路由表无 `/statistics` 页面下的报告 tab，且 `/settings` 是 P2 页面，P1 StatisticsPage 引用 P2 页面。当前 P1 阶段此按钮实际不可用
  - **位置**：TECH_DESIGN §6 StatisticsPage 跳转关系
  - **修复建议**：标注"查看报告列表"按钮在 P2 阶段启用，P1 阶段 el-button :disabled 或 v-if="false"

### 维度 5：全局 UI 一致性

逐项核对：

| 检查项 | 状态 | 说明 |
|--------|:---:|------|
| AppLayout 独立子节 | ❌ | §6 全文无 `### 全局布局组件 AppLayout` 子节 |
| 顶栏（el-header）原型 | 隐式 | DashboardPage ASCII 中有"校园噪音分贝预警员系统 用户名 [退出]"仅一行文字，无完整字段行为/组件/跳转 |
| 侧栏角色菜单规则 | 隐式 | 各页面 ASCII 侧栏中部分菜单项标注 `[管理员]`，但未统一定义角色菜单映射表 |
| 登录页独立布局说明 | ✅ | LoginPage 明确标注"独立（无 AppLayout）" |
| 全局加载/错误提示 | ❌ | 无全局加载态（el-loading/skeleton）和全局错误提示策略定义 |

- **issue-5** [严重度：高]：§6 未定义独立 `### 全局布局组件 AppLayout` 子节。AppLayout 被 7 个页面 ASCII 引用但从未作为原型独立设计——包含 el-header（logo + 用户名展示 + 退出按钮）、el-aside（侧栏菜单 + 角色可见性规则）、el-main（router-view）
  - **位置**：TECH_DESIGN §6 · 应在 LoginPage 之前或作为独立子节追加
  - **推演**：Phase 5 vue-page-coder 看到各页面 ASCII 中"AppLayout"但找不到其原型 → 自行解读 → 不同页面对 AppLayout 的理解不一致 → el-header 高度/样式/响应式断点各写一套 → 8 页面风格分裂
  - **修复建议**：新增 `### 全局布局组件 AppLayout` 子节，包含：
    1. ASCII 布局（el-container > el-header + el-container > el-aside + el-main）
    2. Element Plus 组件列表（el-container/el-header/el-aside/el-main/el-menu/el-dropdown/el-avatar）
    3. 字段行为：用户名从 Pinia userStore 读取，退出按钮清除 token 跳 /login
    4. 角色菜单映射表（普通用户 vs 管理员各见哪些菜单项）
    5. 响应式断点：≥992px 侧栏 200px / 768-991px 折叠 64px / <768px el-drawer

- **issue-6** [严重度：中]：全局角色菜单映射表未统一定义。各页面 ASCII 侧栏零星标 `[管理员]`，但无完整映射表说明"哪些菜单项对普通用户可见、哪些仅管理员可见"
  - **位置**：TECH_DESIGN §6 各页面侧栏区域
  - **修复建议**：与 issue-5 联动——在 AppLayout 子节中补充完整角色菜单映射表：

| 菜单项 | URL | 普通用户 | 管理员 |
|--------|-----|:---:|:---:|
| 仪表盘 | / | ✅ | ✅ |
| 噪声监测 | /noise-monitor | ✅ | ✅ |
| 告警历史 | /alert-history | ✅ | ✅ |
| 功能区配置 | /area-config | ❌ | ✅ |
| 阈值配置 | /threshold-config | ❌ | ✅ |
| 统计分析 | /statistics | ✅ | ✅ |
| 系统设置 | /settings | ❌ | ✅ |

- **issue-7** [严重度：中]：P0-1 的 GET /api/user/profile 和 PUT /api/user/password 无 §6 UI（与 issue-1 合并——通过在 AppLayout 顶栏 el-dropdown 中提供入口）
  - **位置**：TECH_DESIGN §6 · 缺失位置 = 应存在的 AppLayout 子节 el-header 区域

- **issue-8** [严重度：低]：ThresholdConfigPage 有 [重载配置(ccswitch)] 按钮（调用 POST /api/threshold/rule/reload），SystemSettingsPage 也有 [重载配置] 按钮（调用 POST /api/ccswitch/reload）——两个不同 API 的按钮标签高度相似，coder 可能混淆
  - **位置**：TECH_DESIGN §6 ThresholdConfigPage + SystemSettingsPage
  - **修复建议**：ThresholdConfigPage 按钮改为"重载阈值规则"，SystemSettingsPage 保持"重载配置"

---

## 修复行动建议

按严重度排序：

### 高严重度（1 个 · 必须立即修）
1. **issue-5**：新增 `### 全局布局组件 AppLayout` 子节 —— 7 个页面依赖但无原型，不修会导致 Phase 5 页面风格分裂

### 中严重度（3 个）
2. **issue-1**：P0-1 profile/password 缺失 UI——与 issue-5 联动，在 AppLayout el-header 增加下拉菜单
3. **issue-6**：全局角色菜单映射表未统一——与 issue-5 联动，在 AppLayout 子节中补充
4. **issue-7**：与 issue-1 同一根源——AppLayout 顶栏需含个人信息下拉入口

### 低严重度（4 个）
5. **issue-2**：NoiseMonitorPage 批量导入对话框标签 P1-5 → P0-2
6. **issue-3**：StatisticsPage 补充模型性能对比卡片
7. **issue-4**：StatisticsPage "查看报告列表"按钮标注 P2 阶段启用
8. **issue-8**：两个"重载配置"按钮区分命名
9. **issue-9**：DashboardPage 侧栏"功能区"→"功能区配置"
