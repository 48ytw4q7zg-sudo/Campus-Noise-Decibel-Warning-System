# Phase 3 R-04 API 设计审核报告 · 2026-06-11

## 审核元数据
- 审核日期: 2026-06-11
- 使用模型: Claude Opus 4.8 (与 api-designer 同源自审)
- 输入摘要: docs/API_DESIGN.md · 11 模块 52 接口 (P0 21 + P1 13 + P2 18) · §1-§4 四节完整

## 审核报告

### 维度 1: RESTful 规范性

- **issue-1** [严重度: 中]: POST /api/thresholds/check/{noiseRecordId} 的 "check" 是动词（RPC 风格），非标准 RESTful 资源操作
  - **位置**: API_DESIGN §2.3 #2 + §3.3
  - **修复建议**: 改为子资源动作 `POST /api/noise/records/{id}/check`，语义更清晰（对噪声记录执行阈值检查），controller 归属也跟 NoiseController 对齐

- **issue-2** [严重度: 低]: 路径参数 {noiseRecordId} 使用 camelCase，RESTful 惯例推荐 kebab-case
  - **位置**: API_DESIGN §3.3 POST /api/thresholds/check/{noiseRecordId}
  - **修复建议**: 改为 `{noise-record-id}`，或简化为标准 `{id}` (既然路径已含 check 动词，id 语义自明)

- **issue-3** [严重度: 低]: `/api/noise` 模块根路径用单数名词（noise 是 mass noun），子资源 `/api/noise/records` 又用复数，风格不统一
  - **位置**: API_DESIGN §2.2 模块前缀 `/api/noise`
  - **修复建议**: 方案A — 保持现状（noise 作为业务域名词可接受）；方案B — 改为 `/api/noise-records` 全路径复数。推荐方案A，但需在 §1 接口约定中加一句说明

### 维度 2: 完整性

- **issue-4** [严重度: 中]: P2 报告模块 (§3.9) 的 GET /api/reports、POST /api/reports、PUT /api/reports/config 缺少成功响应 JSON 示例
  - **位置**: API_DESIGN §3.9
  - **修复建议**: 补充完整的成功响应 JSON（report 列表含 id/reportPeriod/periodStart/periodEnd/status/createTime，POST 返回 {id}）

- **issue-5** [严重度: 中]: §3.8 P2 统计接口 (GET /api/statistics/multi-dim、/heatmap、/radar) 缺少成功响应 JSON 示例，仅给了请求参数表
  - **位置**: API_DESIGN §3.8 末尾
  - **修复建议**: 为 3 个 P2 统计接口各补充成功响应 JSON 示例（至少 multi-dim 和 heatmap 各一个）

- **issue-6** [严重度: 中]: 错误码 5000 在 §3.2 POST /api/noise/records/batch 中使用，但 §4.3 业务异常码表 5001-5099 起号是 5001，5000 未在范围内定义
  - **位置**: API_DESIGN §3.2 第 322 行 vs §4.3 5001-5099
  - **修复建议**: §4.3 将 5001-5099 改为 5000-5099，或 §3.2 将 5000 改为 5001（与导出上限共用同一 code），推荐前者

### 维度 3: 一致性

- **issue-7** [严重度: 高]: 错误码 2001 "用户不存在" 放在 §4.3 的 2001-2099 (噪声数据) 码段，但语义上属于用户模块 (1xxx)。且 PRD §4.1 明确将用户错误码归入 1xxx
  - **位置**: API_DESIGN §4.3 第 1538 行 "2001=用户不存在"
  - **修复建议**: 将 2001 "用户不存在" 移入 1001-1099 (用户/认证)，例如复用 1006；或扩展现有码段。同时将 §3.1 GET /api/users/me 异常响应中的 2001 改为对应新码

### 维度 4: 安全性

- **issue-8** [严重度: 中]: POST /api/noise/records 传感器上报通道无需认证（"传感器无需认证"），但没有任何替代安全措施说明（IP 白名单、共享密钥、速率限制等），存在未授权写入攻击面
  - **位置**: API_DESIGN §3.2 POST /api/noise/records
  - **修复建议**: 在接口描述中补充安全约束：① 建议生产环境加 X-API-Key Header 或 IP 白名单 ② 教学阶段标注"无需认证仅限演示环境"。在异常响应表中增加 429 "请求过于频繁"

- **issue-9** [严重度: 中]: 多个 P2 管理员接口 (§3.9 POST /api/reports, PUT /api/reports/config; §3.10 PUT /api/ai/config) 标注了"角色限制：仅管理员"但异常响应表中缺少 403 行，下游 coder 可能漏加角色校验
  - **位置**: API_DESIGN §3.9 第 1380-1407 行, §3.10 第 1438-1449 行
  - **修复建议**: 为所有管理员专属接口的异常响应表补充 `| 403 | 越权访问 | 非管理员 |` 行

- **issue-10** [严重度: 低]: POST /api/auth/login 无失败次数限制或账户锁定机制说明
  - **位置**: API_DESIGN §3.1 POST /api/auth/login
  - **修复建议**: 标注"教学简化：不实现登录失败锁定"，防下游 coder 误以为需实现

### 维度 5: 业务覆盖

- **issue-11** [严重度: 中]: §2.4 阈值规则模块 #1 GET /api/thresholds/rules 标注 P0，但 PRD 中 P1-4 阈值规则配置界面为 P1。P0-3 只需 GET /api/thresholds/current 即可满足。P0 阶段即暴露完整规则列表接口但不提供 CRUD 属于半成品
  - **位置**: API_DESIGN §2.4 #1
  - **修复建议**: 方案A(推荐) — 保持 P0 标注但加注释说明 "P0 阶段仅返回启用的规则列表供仪表盘使用，CRUD 为 P1"；方案B — 降为 P1，P0 阶段仅靠 GET /api/thresholds/current 满足需求

- **issue-12** [严重度: 低]: §2.7 功能区配置模块 GET /api/areas 标注角色限制为"管理员"，但 PRD P0-4 仪表盘需要读取功能区名称和阈值。P0-4 是普通用户可访问的，普通用户需能调用此接口或另有仪表盘专用聚合接口
  - **位置**: API_DESIGN §2.7 #1
  - **修复建议**: 方案A — GET /api/areas 改为"普通用户/管理员"均可调用（仪表盘间接调用此接口获取功能区名称）；方案B — 仪表盘所需数据由 GET /api/dashboard/overview 聚合返回，GET /api/areas 维持管理员专属。推荐方案B（已在 §3.5 聚合返回 location + thresholdValue）

### 维度 6: 跨文档对账(强制 4 类对账 · 任一失败 = 高严重度)

#### 6.1 接口 ↔ PRD「API 形态」对账

- **参考集**(PRD §3 各功能 API 形态·关键差异接口):

| PRD 功能 | PRD URL 形态 |
|---|---|
| P0-1 注册 | `POST /api/user/register` |
| P0-1 登录 | `POST /api/user/login` |
| P0-1 个人信息 | `GET /api/user/profile` |
| P0-1 修改密码 | `PUT /api/user/password` |
| P0-2 传感器上报 | `POST /api/noise/record` |
| P0-2 批量导入 | `POST /api/noise/batch` |
| P0-2 最新数据 | `GET /api/noise/latest` |
| P0-3 当前阈值 | `GET /api/threshold/current` |
| P0-3 手动判断 | `POST /api/threshold/check/{noiseRecordId}` |
| P0-4 仪表盘概览 | `GET /api/dashboard/overview` |
| P0-4 功能区详情 | `GET /api/dashboard/area/{location}` |
| P0-5 告警列表 | `GET /api/alert/list` |
| P0-5 告警详情 | `GET /api/alert/{id}` |
| P0-5 确认告警 | `PUT /api/alert/{id}/confirm` |
| P0-5 处置告警 | `PUT /api/alert/{id}/resolve` |
| P0-6 功能区列表 | `GET /api/area/list` |
| P0-6 修改功能区 | `PUT /api/area/{id}` |
| P0-7 噪声列表 | `GET /api/noise/list` |
| P0-7 噪声详情 | `GET /api/noise/{id}` |

- **被检集**(API_DESIGN §2 接口清单对应 URL):

| API_DESIGN URL |
|---|
| `POST /api/auth/register` |
| `POST /api/auth/login` |
| `GET /api/users/me` |
| `PUT /api/users/me/password` |
| `POST /api/noise/records` |
| `POST /api/noise/records/batch` |
| `GET /api/noise/records/latest` |
| `GET /api/thresholds/current` |
| `POST /api/thresholds/check/{noiseRecordId}` |
| `GET /api/dashboard/overview` |
| `GET /api/dashboard/areas/{location}` |
| `GET /api/alerts` |
| `GET /api/alerts/{id}` |
| `PUT /api/alerts/{id}/confirm` |
| `PUT /api/alerts/{id}/resolve` |
| `GET /api/areas` |
| `PUT /api/areas/{id}` |
| `GET /api/noise/records` |
| `GET /api/noise/records/{id}` |

- **差集 / 结论**:

- **issue-13** [严重度: 高]: **正向差异 — PRD 声明但 API_DESIGN URL 形态不同的有 15 个接口**（如上表所示）。PRD 统一使用单数名词 (`/api/user`, `/api/noise`, `/api/alert`, `/api/area`, `/api/threshold`)，而 API_DESIGN 统一使用 RESTful 复数 `/api/users`, `/api/noise/records`, `/api/alerts`, `/api/areas`, `/api/thresholds`。此外 TECH_DESIGN §2.2 各模块职责表中也引用了 PRD 风格的单数 URL（如 `POST /api/user/register`、`GET /api/noise/list`）。

  **这不是简单的命名风格差异——三份核心文档（PRD↔TECH↔API_DESIGN）对同一接口的 URL 描述不一致，Phase 4/5 的 developer 会困惑"到底用哪个 URL"。API_DESIGN 作为接口设计权威源的 RESTful 选择是正确的，但 PRD 和 TECH_DESIGN 中的旧 URL 引用未同步更新。**

  修复建议：① API_DESIGN §1 接口约定中增加一行说明"本设计采用 RESTful 复数命名，与 PRD §3 中的 API 形态简写可能不同，以本文为准"；② TECH_DESIGN §2.2 各模块职责表中的 URL 需全局替换为 API_DESIGN 中的正式 URL（详见 /tech-designer 应用修复）。PRD 中的 API 形态字段标注为"指示性简写"亦可。

- **issue-14** [严重度: 高]: **反向差异 — API_DESIGN 有但 PRD 找不到明确出处的接口**：§2.10 AI 分类模块 2 个接口 (`POST /api/ai/classify`, `PUT /api/ai/config`) 和 §2.11 ccswitch 模块 2 个接口 (`GET /api/ccswitch/status`, `POST /api/ccswitch/reload`) 在 PRD §3 全量功能中均有明确对应（P2-2 和 P2-5），经核实无反向孤儿 API。对账通过。

#### 6.2 接口 ↔ DATABASE 字段对账

- **参考集**(DATABASE_DESIGN §3 各表全部字段):
  - user: id, username, password, role, status, create_time, update_time, is_deleted
  - noise_record: id, location, decibel, time_point, device_id, is_abnormal, judged_by_model, noise_type, noise_duration, create_time, update_time
  - threshold_rule: id, location, time_segment, threshold_value, description, status, version, create_time, update_time
  - alert_log: id, noise_record_id, location, decibel, threshold_value, alert_type, confirm_status, confirmed_by, remark, version, create_time, update_time
  - area_config: id, area_name, noise_sensitivity, default_threshold, description, status, window_size, k_value, version, create_time, update_time
  - report: id, report_period, period_start, period_end, content, status, create_time, update_time

- **被检集**(API_DESIGN §3 所有接口请求/响应字段):
  - 逐表对账结果已在审核过程中完成，关键发现如下：

- **差集 / 结论**:

- **issue-15** [严重度: 高]: **字段编码不一致 — 所有接口响应 JSON 使用 camelCase (`createTime`, `timePoint`, `deviceId`, `isAbnormal`, `judgedByModel`, `noiseType`, `noiseDuration`, `thresholdValue`, `alertType`, `confirmStatus`, `confirmedBy`, `areaName`, `noiseSensitivity`, `defaultThreshold`, `windowSize`, `kValue`, `reportPeriod`, `periodStart`, `periodEnd`)，JSON ↔ Java 字段映射正常（Jackson 自动转换 snake_case → camelCase）。经逐表对比 DATABASE_DESIGN §3，所有 API 字段均可追溯到对应数据库列，无"接口有但数据库无"的孤儿字段。**

  **但存在一个类型映射潜在问题**：API 中 `decibel` 使用 `Double` 类型（JSON number），数据库为 `DECIMAL(5,1)`。Java 端应使用 `BigDecimal` 而非 `Double`（CLAUDE.md §二·二 明确禁止 DECIMAL 用 FLOAT/DOUBLE 防精度丢失）。API_DESIGN 中标注的 `Double` 会误导下游 entity-coder 使用 `Double` 类型。建议在 §1 接口约定中加一句"decibel 字段后端使用 BigDecimal，JSON 序列化为 number，前端解析为 Number"。

  修复建议：§1 接口约定表增加一行 `| DECIMAL 精度 | decibel 字段后端用 BigDecimal，JSON number，前端 Number（保留 1 位小数）|`。

- **对账通过**: 除上述类型提示外，所有业务字段均可在 DATABASE 中找到对应列，无孤儿字段。

#### 6.3 接口 ↔ TECH §3 UI 按钮对账

- **参考集**(TECH_DESIGN §6 各页面原型中的可交互元素):
  - AppLayout: 个人信息弹窗(GET /api/users/me)、修改密码弹窗(PUT /api/users/me/password)、退出登录
  - DashboardPage: 功能区卡片点击(路由跳转)、自动轮询(GET /api/dashboard/overview)
  - NoiseMonitorPage: 手动录入(POST /api/noise/records)、批量导入(POST /api/noise/records/batch)、详情(GET /api/noise/records/{id})、重新判断(POST /api/thresholds/check/{id})、导出CSV(GET /api/noise/records/export)、高级筛选(GET /api/noise/records/search)
  - AlertHistoryPage: 确认(PUT /api/alerts/{id}/confirm)、处置(PUT /api/alerts/{id}/resolve)
  - AreaConfigPage: 编辑(PUT /api/areas/{id})
  - ThresholdConfigPage: 新增规则(POST /api/thresholds/rules)、编辑(PUT)、删除(DELETE)、重载阈值规则(POST /api/thresholds/rules/reload)、保存自适应配置(PUT /api/thresholds/adaptive/config)
  - StatisticsPage: 时间序列图查询(GET /api/statistics/timeseries)、功能区统计(GET /api/statistics/areas)、模型性能(GET /api/statistics/models)、多维度(GET /api/statistics/multi-dim)、热力图(GET /api/statistics/heatmap)、雷达图(GET /api/statistics/radar)、查看报告(GET /api/reports)
  - SystemSettingsPage: 上传导入(POST /api/data/import)、导出Excel(GET /api/data/export-report)、AI分类(POST /api/ai/classify)、AI配置(PUT /api/ai/config)、手动生成报告(POST /api/reports)、报告配置(PUT /api/reports/config)、重载配置(POST /api/ccswitch/reload)

- **被检集**(API_DESIGN §2 接口清单):
  所有 52 个接口已在 §2 列出。

- **差集 / 结论**:
  - **正向**: 所有 UI 按钮均有对应 API ✓
  - **反向**: 所有 API_DESIGN 接口均有 UI 入口或内部调用方 ✓
  - **但**: TECH_DESIGN §2.2 各模块职责表中引用的 URL（如 `POST /api/user/register`、`GET /api/noise/list`）与 API_DESIGN 中正式 URL（`POST /api/auth/register`、`GET /api/noise/records`）不一致，与 issue-13 属同一根因。

- **对账通过（URL 差异已包含在 issue-13）**

#### 6.4 错误码段 ↔ PRD §4.1 错误码规范对账

- **参考集**(PRD §4.1 统一错误码规范):
  | 码段 | 模块 |
  | 1xxx | 用户模块 |
  | 2xxx | 阈值/统计模块 |
  | 3xxx | 规则配置模块 |
  | 4xxx | 告警模块 |
  | 5xxx | 数据导入导出模块 |
  | 6xxx | 功能区配置模块 |
  | 7xxx | 系统/ccswitch 模块 |

- **被检集**(API_DESIGN §4.3 业务异常码表):
  | 码段 | API_DESIGN 分配 |
  | 1001-1099 | 用户/认证 |
  | 2001-2099 | 噪声数据 |
  | 3001-3099 | 阈值规则 |
  | 4001-4099 | 告警 |
  | 5001-5099 | 数据导入导出 |
  | 6001-6099 | 功能区配置 |
  | 7001-7099 | ccswitch |
  | 8001-8099 | 报告 (PRD 无此段) |
  | 9001-9099 | AI 分类 (PRD 无此段) |

- **差集 / 结论**:

- **issue-16** [严重度: 高]: **2xxx 码段模块归属冲突 — PRD §4.1 将 2xxx 分配给"阈值/统计模块"，API_DESIGN §4.3 将 2001-2099 分配给"噪声数据"模块。** 且 API_DESIGN 在噪声数据码段中混入了"用户不存在"(2001)这类应属于 1xxx 的错误码（已在 issue-7 中指出）。

  修复建议：① 统一 2xxx 归属——方案A(推荐)：PRD 的"阈值/统计"拆分为两个独立段，2001-2099 给噪声数据，2100-2199 给阈值判断，2200-2299 给统计；方案B：将噪声数据错误码移入新段(如 0xxx 或维持现状调整 PRD)。② 无论选哪个方案，PRD §4.1 需同步更新。

- **issue-17** [严重度: 高]: **API_DESIGN 新增了 8001-8099(报告) 和 9001-9099(AI 分类) 两个码段，PRD §4.1 中未定义。** 正向差异——API_DESIGN 作为后续文档扩展了新模块的码段，但 PRD 未同步。

  修复建议：PRD §4.1 表增加 8xxx(报告模块) 和 9xxx(AI 分类模块) 两行。注意此修改需在 srs-writer 应用修复中完成。

### 维度 7: 反例推演(推演过程显式记录 · 不只给结论)

#### 7.1 资源不存在推演: 每个含 {id} 接口的 404 行为

**推演过程**: 依次对 9 个含路径参数的接口推演"客户端传入不存在的 id"的场景：

| 接口 | id 不存在时的行为 | 是否明示？ |
|---|---|---|
| GET /api/noise/records/{id} | 2003 "噪声记录不存在" | ✓ |
| POST /api/thresholds/check/{noiseRecordId} | 2003 "噪声记录不存在" | ✓ |
| PUT /api/thresholds/rules/{id} | 2003 "规则不存在" | ✓ |
| DELETE /api/thresholds/rules/{id} | 2003 "规则不存在" | ✓ |
| GET /api/alerts/{id} | 4001 "告警不存在" | ✓ |
| PUT /api/alerts/{id}/confirm | 4001 "告警不存在" | ✓ |
| PUT /api/alerts/{id}/resolve | (同 PUT /api/alerts/{id}/confirm) | ✓ |
| PUT /api/areas/{id} | 6001 "功能区不存在" | ✓ |
| GET /api/reports/{id} | 8001 "报告不存在" | ✓ |
| GET /api/dashboard/areas/{location} | 2002 "功能区不存在" | ✓ |

**结论**: 所有含路径参数的接口均已明示"资源不存在"错误响应。✓ 无 issue。

#### 7.2 越权推演: 角色越权 + 行级权限

**推演过程**:

**场景 A — 普通用户调用管理员专属接口**: 假设普通用户 `user_id=2, role=普通用户` 携带有效 JWT 调用 `PUT /api/areas/1`。API_DESIGN 标注"角色限制：仅管理员"，异常响应表含 403 "越权访问"。预期：LoginInterceptor 放行(JWT 有效) → Controller/Service 层检查 role != 管理员 → 返回 403 + "越权访问"。✓ 管理员专属接口均含 403。

但以下 P2 接口虽标注"角色限制：仅管理员"却未在异常响应表中显式列出 403：
- POST /api/reports (§3.9)
- PUT /api/reports/config (§3.9)
- PUT /api/ai/config (§3.10)

→ **issue-9 已覆盖**。

**场景 B — 同角色行级权限**: 假设用户 A (`user_id=2`) 调用 `PUT /api/users/me/password`。API_DESIGN 标注"行级权限：从 JWT 解析 userId，仅操作自己的 user 记录"。✓ 推演通过。

**场景 C — 普通用户查看/修改他人告警**: 假设普通用户调用 `PUT /api/alerts/1/confirm`（告警 id=1 不是他创建的）。该接口标注角色限制为"仅管理员"，普通用户直接 403。✓ 推演通过。

**场景 D — 传感器未认证访问管理接口**: 传感器通道 POST /api/noise/records 无需认证，但 API_DESIGN 说明"传感器上报无需认证（内部接口），管理员手动录入需登录"。这引入了二义性——同一个接口，无 token 时走传感器通道，有 token 时走管理员通道。Service 层如何区分？需在接口描述中补充"通过 token 有无区分通道：无 token → sensorMode(仅写数据)，有 token + 管理员 role → adminMode(可写数据含 deviceId 覆盖)"。

- **issue-18** [严重度: 中]: **POST /api/noise/records 双通道（传感器 vs 管理员）的区分机制未明确**。同一个 URL、同一个 HTTP 方法，通过"有无 token"隐式区分行为，这在 Phase 4 Service 层实现时会产生歧义
  - **位置**: API_DESIGN §3.2 POST /api/noise/records
  - **修复建议**: 明确标注"无 Authorization Header → 传感器通道（仅写 location/decibel/timePoint，deviceId 从 Header 或默认取）; 有 Authorization Header + role=管理员 → 管理员通道"

#### 7.3 并发幂等推演: 状态变更类接口的幂等性方案

**推演过程**: 依次对状态变更类接口推演"客户端连点 2 次 / 网络重发"的场景：

| 接口 | 幂等性方案 | 是否明示？ | 推演结果 |
|---|---|---|---|
| POST /api/auth/register | DB UNIQUE(username) | ✓ (1001) | 第二次返回 1001 ✓ |
| POST /api/noise/records | 允许重复(传感器场景) | ✓ | 第二次正常写入 ✓ |
| POST /api/noise/records/batch | 无 | ✗ | 第二次产生重复数据 |
| POST /api/thresholds/check/{id} | is_abnormal IS NOT NULL 跳过 | ✓ (2004) | 第二次返回 2004 ✓ |
| POST /api/thresholds/rules | UNIQUE(location,time_segment) | ✓ (3001) | 第二次返回 3001 ✓ |
| PUT /api/thresholds/rules/{id} | 乐观锁 version | ✓ (3002) | 第二次返回 3002 ✓ |
| DELETE /api/thresholds/rules/{id} | 无明示 | ✗ | 第一次成功，第二次 2003 |
| PUT /api/alerts/{id}/confirm | WHERE confirm_status='未确认' | ✓ (4002) | 第二次返回 4002 ✓ |
| PUT /api/alerts/{id}/resolve | WHERE confirm_status='已确认' | ✓ | 第二次返回 4002 ✓ |
| PUT /api/areas/{id} | 乐观锁 version | ✓ (6002) | 第二次返回 6002 ✓ |
| POST /api/reports | 同周期去重检查 | ✓ | 第二次跳过 ✓ |
| POST /api/ai/classify | CAS WHERE noise_type IS NULL | ✓ | 第二次跳过 ✓ |
| POST /api/ccswitch/reload | 前端按钮防抖 | ✗ (仅前端) | 服务端无幂等保护 |

- **issue-19** [严重度: 中]: **DELETE /api/thresholds/rules/{id} 重复删除的幂等行为未定义**。推演：第一次 DELETE 成功(200)，第二次 DELETE 同一 id → 返回 2003 "规则不存在"。但异常的 HTTP 语义模糊——应该返回 200(幂等成功)还是 404(不存在)？API_DESIGN 未明示，下游 coder 可能各自实现不同行为
  - **位置**: API_DESIGN §3.4 DELETE /api/thresholds/rules/{id}
  - **修复建议**: 异常响应表补充一行 `| 2003 | 规则不存在 | id 不存在或已被删除 |`（标注幂等行为：重复删除返回相同 2003 或改为返回 200 视为幂等成功）

- **issue-20** [严重度: 低]: **POST /api/ccswitch/reload 仅前端防抖，无服务端幂等方案**。P2 功能且操作频率低，风险可控，但仍建议标注
  - **位置**: API_DESIGN §3.11 POST /api/ccswitch/reload
  - **修复建议**: 补充"教学简化：不实现服务端幂等，前端防抖已满足 P2 演示需求"

- **issue-21** [严重度: 低]: **POST /api/noise/records/batch 无幂等保护**。批量导入可能被重试，产生重复数据。PRD §P0-2 说"不做幂等去重"，但 API_DESIGN 至少应标注此限制
  - **位置**: API_DESIGN §3.2 POST /api/noise/records/batch
  - **修复建议**: 补充"教学简化：批量导入不做幂等去重，重复提交会写入重复数据"

#### 7.4 分页/边界/空集合推演: pageSize 上限 + 空集合返回值 + 排序白名单

**推演过程**:

**场景 A — pageSize=999999**: 对 GET /api/noise/records、GET /api/alerts、GET /api/reports 三个分页接口推演：
- GET /api/noise/records: "pageSize 1-100（默认 20），超限返回 400" ✓
- GET /api/alerts: "pageSize 1-100（默认 20）" ✓
- GET /api/reports: "pageSize 1-100" ✓

**场景 B — 空集合**: 推演所有列表接口无数据时的返回：
- GET /api/noise/records: `data.records: []` ✓
- GET /api/noise/records/search: 同 GET /api/noise/records ✓
- GET /api/alerts: `data.records: []` ✓
- GET /api/reports: `data.records: []` ✓
- GET /api/thresholds/rules: `data: []` ✓
- GET /api/dashboard/overview: 某功能区无数据时 `data: null` ✓
- GET /api/noise/records/latest: 某功能区无数据时元素 `data: null` ✓
- GET /api/statistics/timeseries: `data.points: []` ✓

**场景 C — 排序注入**: 推演是否有 sortBy 白名单：
- GET /api/noise/records: sortBy 白名单 `time_point`/`decibel`/`create_time` ✓
- GET /api/alerts: 无 sortBy 参数，默认 `create_time DESC` (无用户可控排序，安全) ✓
- GET /api/reports: 无 sortBy 参数 ✓

- **issue-22** [严重度: 低]: **GET /api/noise/records/search 未显式声明 sortBy 白名单约束**，仅写"其余参数同 GET /api/noise/records"。虽可通过引用继承白名单，但显式标注更安全
  - **位置**: API_DESIGN §3.2 GET /api/noise/records/search
  - **修复建议**: 在"其余参数同…"后补充"（含 sortBy 白名单 + pageSize 上限约束）"

**空 body/空字段推演**:
- POST /api/auth/register: 缺 username → 400 "参数校验失败" ✓
- POST /api/auth/login: 缺 password → 400 "参数校验失败" ✓
- POST /api/noise/records: 缺 location → 400 ✓

**结论**: 分页安全基本到位，仅 issue-22 为低严重度。

## 修复行动建议

**按严重度排序的修复优先级**:

### 🔴 高严重度 (5 条 — 必须修复后再进 Phase 4)

1. **issue-7**: 错误码 2001 "用户不存在" 归属错误 → 移入 1xxx 用户码段
2. **issue-13**: PRD ↔ API_DESIGN ↔ TECH_DESIGN 三文档 URL 命名全面不一致 → API_DESIGN §1 加权威源声明 + TECH_DESIGN 同步更新 URL
3. **issue-16**: 2xxx 错误码段归属冲突 (PRD 说阈值/统计，API_DESIGN 说噪声数据) → 两文档需协调统一
4. **issue-17**: API_DESIGN 新增 8xxx/9xxx 码段，PRD 未同步 → PRD §4.1 需补
5. **issue-15**: API 字段类型 Double → 应标注 BigDecimal 防精度丢失

### 🟡 中严重度 (9 条 — Phase 4 开始前修复)

6. **issue-1**: POST /api/thresholds/check/{noiseRecordId} 动词风格改子资源
7. **issue-4**: P2 报告模块缺成功响应 JSON
8. **issue-5**: P2 统计接口缺成功响应 JSON
9. **issue-6**: 错误码 5000 未在 §4.3 定义范围内
10. **issue-8**: POST /api/noise/records 无认证通道缺安全约束说明
11. **issue-9**: 多个 P2 管理员接口缺 403 异常响应行
12. **issue-11**: 阈值规则列表 P0/P1 优先级标注存疑
13. **issue-18**: 传感器/管理员双通道区分机制未明确
14. **issue-19**: DELETE 规则幂等行为未定义

### 🟢 低严重度 (8 条 — 可延后但不建议)

15. **issue-2**: 路径参数 camelCase
16. **issue-3**: /api/noise 模块路径命名风格
17. **issue-10**: 登录锁机制说明
18. **issue-12**: GET /api/areas 角色限制讨论
19. **issue-20**: POST /api/ccswitch/reload 幂等标注
20. **issue-21**: POST /api/noise/records/batch 幂等限制标注
21. **issue-22**: GET /api/noise/records/search sortBy 白名单显式声明
