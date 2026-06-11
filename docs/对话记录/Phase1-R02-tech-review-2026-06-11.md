# Phase 1 R-02 概要设计审核报告 · 2026-06-11

## 审核元数据
- 审核日期：2026-06-11
- 使用模型：Claude Opus 4.8
- 输入摘要：docs/TECH_DESIGN.md §1-§5，含架构图、10 模块划分、8 页面路由、5 个 Mermaid 流程图、技术选型 3 表

---

## 审核报告

### 维度 1：与 PRD 的双向追溯

逐功能对照 PRD §3（17 个功能，P0-1~P2-5）→ TECH_DESIGN §2.1 模块表：

| PRD 功能 | TECH_DESIGN 模块 | 匹配 |
|---------|-----------------|:---:|
| P0-1 用户注册登录 | 用户模块 UserController | ✅ |
| P0-2 噪声数据采集 | 噪声数据模块 NoiseController | ✅ |
| P0-3 业务规则阈值判断 | 阈值判断模块 ThresholdController | ✅ |
| P0-4 仪表盘 | 仪表盘模块 DashboardController | ✅ |
| P0-5 告警记录 | 告警模块 AlertController | ✅ |
| P0-6 功能区配置 | 功能区配置模块 AreaController | ✅ |
| P0-7 噪声数据列表 | 噪声数据模块 NoiseController | ✅ |
| P1-1 统计自适应阈值 | **统计/可视化模块 StatisticsController** | ⚠️ issue-1 |
| P1-2 混合阈值模型 | **统计/可视化模块 StatisticsController** | ⚠️ issue-1 |
| P1-3 历史数据可视化 | 统计/可视化模块 StatisticsController | ✅ |
| P1-4 阈值规则配置 | 阈值判断模块 ThresholdController | ✅ |
| P1-5 高级筛选导出 | 噪声数据模块 NoiseController | ✅ |
| P2-1 数据导入导出 | 噪声数据模块 NoiseController（导入）/ **导出报表入口缺失** | ⚠️ issue-9 |
| P2-2 AI 噪声分类 | AI 分类模块 AiController | ✅ |
| P2-3 定时报告 | 报告模块 ReportController | ✅ |
| P2-4 多维度统计分析 | 统计/可视化模块 StatisticsController | ✅ |
| P2-5 ccswitch 面板 | ccswitch 模块 CcswitchController | ✅ |

- **issue-1** [严重度：高]：P1-1（统计自适应阈值计算）和 P1-2（混合阈值模型）在 §2.1 模块表中被分配到 StatisticsController/StatisticsService，但这两个功能本质是**阈值判断逻辑**（向 noise_record 写入 is_abnormal + judged_by_model），不是只读统计/可视化
  - **位置**：TECH_DESIGN §2.1 模块总览表 · §2.2.7 统计/可视化模块
  - **推演**：Phase 4 coder 基于此表实现 → StatisticsService 需注入 NoiseRecordMapper → ThresholdService 和 StatisticsService 都可能写 is_abnormal → 两个 Service 对同一字段的写入逻辑分散 → P1-3 模型性能对比时数据来源不一致 → 且"Statistics"命名误导（既做写又做读）
  - **修复建议**：二选一 ——（a）将 P1-1 和 P1-2 移到 ThresholdController/ThresholdService（推荐，因为它们是阈值判断逻辑的延续）；或（b）在 StatisticsService 职责中显式标注 NoiseRecordMapper 依赖 + 标注"也负责 P1 阶段阈值判断写入"

- **issue-9** [严重度：中]：P2-1 的 `GET /api/data/export-report`（导出 Excel 报表）在 §2.2 模块职责表中没有明确归属——§2.2.2 噪声数据模块只列出了"数据导入 | POST /api/data/import"，但未列出导出报表 API
  - **位置**：TECH_DESIGN §2.2.2 噪声数据模块职责表
  - **修复建议**：在 §2.2.2 职责表中补充"导出报表 | GET /api/data/export-report · P2-1 · 管理员专属"，或将其移到 ReportController

### 维度 2：架构分层

- **issue-5** [严重度：低]：§1.1 整体架构图标注"Controller 层 (8 个)"，但 §2.1 模块表实际列出 10 个 Controller（User/Noise/Threshold/Dashboard/Alert/Area/Statistics/Report/Ai/Ccswitch）
  - **位置**：TECH_DESIGN §1.1 架构图 ASCII
  - **修复建议**：将"Controller 层 (8 个)"改为"Controller 层 (10 个，含 P0 7 个 + P1/P2 3 个)"

- **issue-3** [严重度：中]：§2.1 模块总览中"阈值判断模块"的功能编号列仅标注"P0-3"，但 P1-1 和 P1-2 同样是阈值计算逻辑（被错误地分配到了统计模块）。如果 P1-1/P1-2 移到 ThresholdController，这个模块的功能编号应更新为"P0-3, P1-1, P1-2"
  - **位置**：TECH_DESIGN §2.1 模块总览表"阈值判断模块"行
  - **修复建议**：与 issue-1 联动修复——P1-1/P1-2 移入后，更新功能编号为"P0-3, P1-1, P1-2"

### 维度 3：模块职责划分

- **issue-2** [严重度：中]：§2.1 模块总览中 StatisticsService 的 Mapper 列标注"—(聚合查询)"，暗示它只需读操作。但如果 P1-1/P1-2 留在此处，StatisticsService 需要向 noise_record 写入 is_abnormal 和 judged_by_model——必须依赖 NoiseRecordMapper
  - **位置**：TECH_DESIGN §2.1 模块总览表"统计/可视化模块"行
  - **修复建议**：与 issue-1 联动——若 P1-1/P1-2 移到 ThresholdService，则此问题自动解决；若保留在 StatisticsService，则标注 NoiseRecordMapper 依赖

- **issue-8** [严重度：中]：TECH_DESIGN 全文未引用 PRD R-01-issue-9 修复中引入的 `noise_record.judged_by_model` 字段（枚举：RULE_BASED / ADAPTIVE / HYBRID）。这是 P0→P1 迁移策略的关键字段，P1-3 模型性能对比依赖它来区分新旧数据——设计文档不记录此字段会导致 db-designer 漏建、coder 漏写
  - **位置**：TECH_DESIGN §2.2.2（噪声数据模块）或 §2.2.7（统计模块）
  - **修复建议**：在 §2.2.2 噪声数据模块职责表补充"judged_by_model 字段 | 标记每条 noise_record 的判定模型来源，枚举 RULE_BASED/ADAPTIVE/HYBRID，P0 历史记录为 RULE_BASED，P1 新记录按实际模型标记"

### 维度 4：路由设计

- **issue-4** [严重度：低]：§3.1 路由表中 `/threshold-config` 的权限列标注"管理员（查看）/ 管理员（编辑）"——PRD §3 P1-4 明确"普通用户不可访问此功能"，仅为管理员专属。双重标注"(查看)/(编辑)"在 PRD 中无对应粒度区分
  - **位置**：TECH_DESIGN §3.1 路由表 `/threshold-config` 行
  - **修复建议**：改为"管理员"（单一行），与 PRD P1-4 权限约束一致

- **issue-10** [严重度：低]：§3.1 路由表 `/area-config` 优先级为 P0，`/threshold-config` 优先级为 P1——但 PRD §5 映射表 P0-3 关联了 ThresholdConfigPage（阈值展示）。ThresholdConfigPage 在 P0 阶段已存在（仅展示阈值，不可编辑），路由应同时支持 P0 只读和 P1 编辑两种模式
  - **位置**：TECH_DESIGN §3.1 路由表
  - **修复建议**：`/threshold-config` 路由标注为"P0（只读展示）/ P1（编辑配置）"，或保持 P1 并在路由守卫中让 P0 阶段管理员可见但只读

### 维度 5：流程图

- **issue-6** [严重度：低]：§1.1 整体架构图中，ccswitch 配置服务用虚线框（`┌ - - - - - - - ┐`）单独标出，但没有画出 SpringBoot → Flask ccswitch 的 HTTP 连接线，而 §1.3 文字和 §4.5 Mermaid 图明确展示了这条调用链路
  - **位置**：TECH_DESIGN §1.1 ASCII 架构图
  - **修复建议**：在架构图中补充 `SpringBoot (CcswitchService) ──HTTP──> Flask ccswitch (Port 5000)` 连线，保持图文一致

### 维度 6：技术选型

- 无 issue。所有技术选型（SpringBoot 3.5.14 / Vue 3.5.34 / MySQL 8.4 / MyBatis-Plus 3.5.15 / JJWT 0.13.0 / Element Plus 2.13.7 / Pinia 3.0.4 / Axios 1.15.2 / Vite 8.0.0 / pnpm 10.33.4）与 CLAUDE.md §一·一 完全一致，备选方案对比表理由充分，安全方案 9 项完整。

---

## 修复行动建议

按严重度排序：

### 高严重度（1 个）
1. **issue-1**：将 P1-1 和 P1-2 从 StatisticsController 移到 ThresholdController——这是阈值判断逻辑，不是统计可视化。推荐方案 (a)

### 中严重度（4 个）
2. **issue-2**：联动 issue-1——P1-1/P1-2 移出后 StatisticsService 的 Mapper 依赖问题自动解决
3. **issue-3**：联动 issue-1——更新阈值判断模块功能编号为"P0-3, P1-1, P1-2"
4. **issue-8**：在 §2.2.2 补充 `judged_by_model` 字段说明
5. **issue-9**：P2-1 导出报表 API 补充归属

### 低严重度（4 个）
6. **issue-5**：§1.1 架构图 Controller 数量改为 10
7. **issue-4**：路由表 /threshold-config 权限改为"管理员"
8. **issue-10**：ThresholdConfigPage 路由标注 P0 只读/P1 编辑
9. **issue-6**：§1.1 架构图补充 SpringBoot→ccswitch HTTP 连线
