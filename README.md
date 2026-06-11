# 校园噪音分贝预警员系统

> 基于 **"业务规则 + 统计自适应 + 混合阈值"** 动态模型的校园噪声监测预警平台
>
> **目标**: 异常检测准确率 ≥92%，F1 分数 ≥91%，误报率 ≤5%
>
> 技术栈：SpringBoot 3.5.14 + Vue 3.5.34 + MySQL 8.4 + Python Flask ccswitch + ECharts 6
>
> | Q-CR 检测 | 得分 | 54 Iron Laws | 状态 |
> |:---|:---:|:---:|:---:|
> | 第 6 轮 Omega v1.1 | **92.15/100** | 96.3% 合规 (52/54) | 0 P0/P1 阻塞项 |
> | 需求覆盖率 | 93.8% (15/16 ≥90%) | 系统完整度 | 96.5% |

---

## 项目概述

校园噪音分贝预警员系统面向**普通用户**和**管理员**两类角色，覆盖**图书馆、食堂、操场、宿舍**四大核心功能区，通过 `传感器数据采集 → 动态阈值计算 → 异常检测 → 告警推送` 的完整业务闭环，实现校园噪声的精准识别与实时预警。

> **工程落地说明**: 本系统是研究报告《校园噪音分贝预警（规则+异常检测类）》的 **Java Web 工程落地版本**。研究报告以 Python（Pandas+Matplotlib）完成实验验证和算法原型，本系统将核心算法（业务规则动态阈值、统计自适应阈值、混合阈值模型）完整迁移至 SpringBoot + Vue 全栈架构，并将 Matplotlib 静态图表升级为 ECharts 交互式可视化。算法逻辑、阈值参数、实验结论与研究报告保持一致，差异仅在实现语言和部署形态（实验脚本 → 生产级 Web 系统）。

### 系统运行链接

| 服务 | 地址 | 说明 |
|------|------|------|
| **前端** | `http://localhost:5173` | Vue 3 开发服务器 (Vite HMR) |
| **后端** | `http://localhost:8080` | SpringBoot REST API |
| **ccswitch** | `http://localhost:5000` | Flask 配置微服务 (P2) |
| **Gitee** | [仓库地址](https://gitee.com/qinxinwei123/campus-noise-decibel-warning-system) | Git 代码仓库 |

### 核心特色 — 混合动态阈值模型

本研究突破传统固定阈值（GB 3096-2008 昼间55dB/夜间45dB）的刚性局限，设计了三种动态阈值模型：

| 模型 | 原理 | 准确率 | 误报率 | 适用场景 |
|------|------|:---:|:---:|------|
| **业务规则动态阈值** | "功能区+时段"二维规则字典，基于 GB 50118/GB 3096 预定义 | 88.7% | 5.3% | 静态功能区(图书馆/宿舍) |
| **统计自适应阈值** | 滑动窗口 μ±k×σ 动态计算，窗口 10-15 分钟 | 89.4% | 8.2% | 动态功能区(食堂/操场) |
| **混合阈值模型 ★** | 常规用统计自适应，触发条件满足时回退业务规则 | **92.6%** | **4.0%** | 全场景最优 |

**为什么混合模型最优？** — 践行"**业务规则 > 算法**"的核心理念：
- 图书馆 >50dB 算异常（安静需求高），操场 >70dB 才算异常（活动容忍度高）
- 纯算法(LOF/孤立森林)只能识别"数值异常"，无法区分"场景正常的数值波动"
- 业务规则贡献约 70% 的性能提升，算法优化贡献 30%
- 3 重触发条件：连续 3 窗口异常率 >10% | 特殊时段(午休/夜间静校) | 分贝骤升 ≥15dB

> 📖 详见研究报告 `03-选题库-学生标定卡/校园噪音分贝预警（规则+异常检测类）研究报告.md`

---

## 系统架构

```
┌──────────────────────────────────────────────────────────────────┐
│                    前端 (Vue 3.5.34 + Element Plus 2.13.7)        │
│  LoginPage → DashboardPage → NoiseMonitorPage → AlertHistoryPage │
│  AreaConfigPage → ThresholdConfigPage → StatisticsPage            │
│  SystemSettingsPage                                                │
│  Axios (/api/*) + Pinia stores (user.js)                          │
│  ECharts 6 (时间序列/柱状图/雷达图/热力图)                          │
└────────────────────────┬─────────────────────────────────────────┘
                         │ HTTP REST + JWT Bearer Token
┌────────────────────────▼─────────────────────────────────────────┐
│                后端 (SpringBoot 3.5.14 · JDK 21)                  │
│  11 Controller → 9 Service → 6 Mapper (65 源文件)                  │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐   │
│  │ LoginInter-   │ │ GlobalEx-     │ │ @Scheduled 定时模拟器  │   │
│  │ ceptor (JWT)  │ │ ceptionHandler│ │ (每 5 分钟自动生成数据) │   │
│  └──────────────┘ └──────────────┘ └────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  动态阈值计算引擎 (ThresholdServiceImpl)                   │   │
│  │  · 业务规则: 功能区+时段二维字典 → 三级兜底 (规则→默认→55) │   │
│  │  · 统计自适应: 滑动窗口 μ±kσ, windowSize/kValue 可配置   │   │
│  │  · 混合模型: autoJudgeWithHybrid() 自动选择最优策略       │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────┬─────────────────────────────────────────┘
                         │ JDBC (MyBatis-Plus 3.5.15)
┌────────────────────────▼─────────────────────────────────────────┐
│               MySQL 8.4 LTS (noise_db · utf8mb4)                  │
│  user | noise_record | threshold_rule | alert_log | area_config   │
│  report (P2)                                                       │
│  6 表 · 5 索引 · 2 外键 · 乐观锁 version                          │
└──────────────────────────────────────────────────────────────────┘

┌ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ┐
│        ccswitch 配置服务 (Python Flask · Port 5000 · P2)         │
│  从 ~/.claude/settings.json 实时读取 AI 模型配置                 │
│  SSE 推送 + 文件监控 + 热更新 + 阈值参数管理                      │
└ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ┘
```

---

## 运行环境说明

### 必备环境

| 组件 | 版本要求 | 验证命令 | 安装指南 |
|------|---------|---------|---------|
| **JDK** | 21+ | `java -version` | [Adoptium](https://adoptium.net/) |
| **Node.js** | 24 LTS (Krypton) | `node -v` | [nodejs.org](https://nodejs.org/) |
| **pnpm** | 10.33+ | `pnpm -v` | `npm i -g pnpm` |
| **MySQL** | 8.4 LTS | `mysql --version` | [dev.mysql.com](https://dev.mysql.com/downloads/) |
| **Maven** | 3.9+ | `mvn -v` | [maven.apache.org](https://maven.apache.org/) |
| **Python** | 3.9+ (ccswitch) | `python --version` | [python.org](https://www.python.org/) |

### Windows 环境变量配置

```powershell
# 确保以下命令在 PowerShell 中可用
java -version        # JDK 21
node -v              # Node.js 24
pnpm -v              # pnpm
mysql --version      # MySQL 8.4
mvn -v               # Maven 3.9

# JWT 密钥（生产环境必设，本地开发可选，默认使用内置值）
$env:JWT_SECRET = "your-production-secret-key"
```

---

## 快速开始

### 一键启动（Windows 推荐）

双击项目根目录的 **`一键启动前后端.bat`**，自动完成：

1. 环境检测（Java / Node.js / pnpm / Maven / MySQL / Python）
2. 数据库初始化（建库 + 建表 + 种子数据）
3. ccswitch 配置服务启动（Flask :5000）
4. 后端编译 + 启动（SpringBoot :8080）
5. 前端依赖安装 + 启动（Vite :5173）
6. 自动打开浏览器 `http://localhost:5173`

### 手动启动

#### 1. 数据库初始化

```bash
mysql -u root -p < sql/01-init.sql
# 创建 noise_db 数据库 + 6 张业务表 + 预置管理员 admin/admin123 + 12 条阈值规则
```

编辑 `backend/src/main/resources/application.yml` 填写本地 MySQL 密码。

#### 2. 启动 ccswitch 配置服务（P2 加分项，可选）

```bash
cd ccswitch_service
pip install -r requirements.txt
python app.py
# → http://localhost:5000
```

#### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
# → http://localhost:8080
```

#### 4. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
# → http://localhost:5173
```

**首次使用**: 访问注册页创建管理员账号（角色选择"管理员"），或直接使用预置账号 `admin / admin123` 登录。

---

## 数据集

### 原始数据集

项目用到的 142 条校园噪声数据集位于 `data/campus_noise_dataset.csv`，格式如下：

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| time_point | datetime | 噪声记录时间点（精确到分钟） | 2025-10-20 08:30:00 |
| location | string | 四大功能区 | 图书馆/食堂/操场/宿舍 |
| decibel | float | 噪声分贝值（A计权） | 35.2 ~ 85.7 dB(A) |
| label | string | 人工标注（场景适配标准） | 正常/干扰 |

**数据统计特征**:

| 功能区 | 样本量 | 平均分贝 | 标准差 | 最高/最低 | 异常占比 |
|------|:---:|:---:|:---:|:---:|:---:|
| 图书馆 | 38 | 48.3 | 6.7 | 62.5/35.2 | 18.4% |
| 食堂 | 36 | 63.8 | 8.2 | 78.4/47.1 | 27.8% |
| 操场 | 34 | 68.9 | 9.5 | 85.7/52.3 | 32.4% |
| 宿舍 | 34 | 51.2 | 7.4 | 72.1/38.5 | 20.6% |

> 数据集构建方法详见研究报告 §3.1。数据预处理（IQR 异常值剔除五条操场极值后）137 条有效记录，训练集 96 条/测试集 41 条，5 折交叉验证。

### Orange 工作流

Orange 异常检测验证工作流配置文件：`data/orange_workflow_config.json`

包含 9 个 Widget + 10 条连接：
- **File** → 加载 `campus_noise_dataset.csv`
- **Select Columns** → 选择 decibel 特征 + label 目标
- **Data Sampler** → 70:30 训练/测试划分
- **Outliers (LOF)** → 局部离群因子（contamination=0.1, n_neighbors=5）
- **Outliers (Isolation Forest)** → 孤立森林（contamination=0.1, n_estimators=100）
- **Confusion Matrix ×2** → LOF/孤立森林混淆矩阵
- **Test & Score** → 5 折交叉验证
- **Scatter Plot** → 异常点可视化

在 Orange Data Mining 中导入 `campus_noise_dataset.csv`，按上述工作流配置即可复现研究报告中 Orange 备选验证实验。

---

## 项目结构

```
CampusNoiseDecibelWarningSystem/
├── backend/                                    # SpringBoot 后端 (65 源文件)
│   ├── src/main/java/com/example/noise/
│   │   ├── common/                             # Result<T> / BusinessException / GlobalExceptionHandler
│   │   │   │                                   # UnauthorizedException / ForbiddenException (10 种异常处理)
│   │   ├── config/                             # CORS / MyBatisPlus / WebMvc
│   │   ├── controller/                         # 11 个 Controller (DTO 入参 + @Valid 校验)
│   │   ├── entity/                             # 6 个 Entity + dto/ (17 DTO)
│   │   ├── interceptor/                        # JWT LoginInterceptor
│   │   ├── mapper/                             # 6 个 Mapper (含 9 个 @Select SQL)
│   │   ├── service/                            # 9 个 Service 接口
│   │   │   └── impl/                           # 9 个 Service 实现 (TOCTOU 防护已闭合)
│   │   └── util/                               # JwtUtils + SensitiveDataUtil (日志脱敏)
│   ├── src/main/resources/
│   │   └── application.yml                     # 主配置 (JWT 环境变量兜底: ${JWT_SECRET:default})
│   ├── src/test/                               # 45 个单元测试全部通过
│   └── pom.xml
├── frontend/                                   # Vue 3 前端 (29 文件)
│   └── src/
│       ├── api/                                # 12 个 API 模块 (含 Blob 拦截器)
│       ├── components/                         # AppLayout (响应式三档)
│       ├── router/                             # Vue Router 5 + 路由守卫 + 全路由懒加载
│       ├── stores/                             # Pinia user store
│       └── views/                              # 9 个页面组件
├── ccswitch_service/                           # Flask 配置服务 (P2)
│   ├── app.py                                  # Flask 主应用 (SSE+文件监控+批量阈值)
│   ├── ccswitch.py                             # ~/.claude/settings.json 读取
│   ├── config.py                               # 配置管理 (ccswitch优先+.env回退)
│   ├── threshold_rules.json                    # 12 条预置阈值规则
│   └── .env.example                            # 环境变量模板
├── data/                                       # 数据集
│   ├── campus_noise_dataset.csv                # 142 条原始噪声数据 (4 字段)
│   └── orange_workflow_config.json             # Orange 工作流配置 (9 Widget)
├── docs/                                       # 设计文档
│   ├── 00-选题标定.md                          # R-00 选题标定卡
│   ├── PRD.md                                  # 需求规格说明书 (13 功能, R-01 已审)
│   ├── TECH_DESIGN.md                          # 概要设计 (§1-§6, R-02 已审)
│   ├── DATABASE_DESIGN.md                      # 数据库设计 (6 表, R-03 已审)
│   ├── API_DESIGN.md                           # API 接口设计 (52 接口, R-04 已审)
│   ├── DEPLOY.md                               # 部署文档 (8 节)
│   └── 对话记录/                                # 审查记录 (R-00~R-08 + Q-CR Round 1~6)
├── sql/
│   └── 01-init.sql                             # 建库 + 6 表 + 种子数据 (12 阈值规则)
├── ai-records/                                 # AI 提示词演化记录
├── 一键启动前后端.bat                            # Windows 一键启动脚本
├── docker-compose.yml                          # 4 服务编排 (MySQL+Backend+Frontend+ccswitch)
├── Dockerfile                                  # 后端 Docker 镜像
├── CLAUDE.md                                   # AI 编码规则 (项目宪法)
└── README.md                                   # 本文件
```

---

## 功能清单（P0+P1+P2 全部完成）

### P0 必做功能

| # | 功能 | 后端实现 | 前端页面 | 对接研究报告 |
|:---:|------|------|------|------|
| P0-1 | 用户注册登录 + JWT | UserServiceImpl + BCrypt + JJWT 0.13.0 | LoginPage | — |
| P0-2 | 噪声数据采集与存储 | createRecord(双通道) + @Scheduled 模拟器 | NoiseMonitorPage | §3.1 数据集构建 |
| P0-3 | 业务规则动态阈值判断 | getCurrentThreshold() + 三级兜底链 | ThresholdConfigPage | §3.3.1 二维规则字典 |
| P0-4 | 实时噪声监测仪表盘 | DashboardController + 10s 自动刷新 | DashboardPage | — |
| P0-5 | 异常告警记录与推送 | AlertLogServiceImpl + 确认→处置状态流转 | AlertHistoryPage | §3.4 异常检测实现 |
| P0-6 | 功能区配置管理 | AreaConfigService + 乐观锁 | AreaConfigPage | §3.1.3 统计特征表 |
| P0-7 | 噪声数据列表与筛选 | queryPage + 多条件 AND 筛选 | NoiseMonitorPage | — |

### P1 应做功能

| # | 功能 | 后端实现 | 前端页面 | 对接研究报告 |
|:---:|------|------|------|------|
| P1-1 | 统计自适应阈值计算 | getAdaptiveThreshold() + sliding window + μ±kσ | ThresholdConfigPage | §3.3.2 滑动窗口 |
| P1-2 | **混合阈值模型** | autoJudgeWithHybrid() + 3 触发条件 | ThresholdConfigPage | **§3.3.3 最优方案** |
| P1-3 | 历史数据可视化 | StatisticsService + 6 API | StatisticsPage (ECharts 5 Tab) | §3.5 可视化实现 |
| P1-4 | 阈值规则配置界面 | ThresholdRule CRUD + ccswitch 热更新 | ThresholdConfigPage | §3.3.1 规则字典 |
| P1-5 | 噪声高级筛选/CSV导出 | searchAdvanced + export CSV (CSV 注入防护) | NoiseMonitorPage | — |

### P2 可选功能

| # | 功能 | 后端实现 | 前端页面 |
|:---:|------|------|------|
| P2-1 | 数据导入导出(CSV+Excel) | NoiseController(parseCsvLine+BOM) | NoiseMonitorPage/SystemSettingsPage |
| P2-2 | AI 辅助噪声分类 | AiServiceImpl(10 条启发式规则+CAS 更新) | SystemSettingsPage |
| P2-3 | 定时报告生成 | ReportServiceImpl(@Scheduled cron 每日 6:00 + TOCTOU 防护) | SystemSettingsPage |
| P2-4 | 多维度统计分析 | heatmap/radar/multi-dim API | StatisticsPage (热力图+雷达图 Tab) |
| P2-5 | ccswitch 配置热更新 | CcswitchService + SSE 推送 | SystemSettingsPage |

---

## API 接口完整列表

### 认证 `/api/auth`
- `POST /api/auth/register` — 注册（公开，BCrypt 加密，用户名唯一索引防重）
- `POST /api/auth/login` — 登录（公开，返回 JWT token + 角色）

### 用户 `/api/users`
- `GET /api/users/me` — 个人信息（需登录）
- `PUT /api/users/me/password` — 修改密码（需登录，校验原密码）

### 噪声数据 `/api/noise`
- `POST /api/noise/records` — 传感器上报/管理员手动录入（双通道识别）
- `POST /api/noise/records/batch` — 批量导入（管理员，单次上限 5000）
- `GET /api/noise/records/latest` — 各功能区最新数据（仪表盘用，批量查询优化）
- `GET /api/noise/records` — 分页列表（时间/功能区/分贝范围/异常状态筛选）
- `GET /api/noise/records/{id}` — 详情
- `GET /api/noise/records/search` — 高级筛选（P1，keyword+noiseType+排序白名单）
- `GET /api/noise/records/export` — CSV 导出（管理员，上限 10000 条，UTF-8 BOM，CSV 注入防护）

### 阈值判断 `/api/thresholds`
- `GET /api/thresholds/current?location=XX` — 当前业务规则阈值（含三级兜底链）
- `POST /api/thresholds/check/{id}` — 手动触发判断（管理员，幂等：is_abnormal NOT NULL 跳过）
- `GET /api/thresholds/adaptive/current?location=XX` — 统计自适应阈值（μ±kσ，ccswitch 优先）
- `PUT /api/thresholds/adaptive/config` — 配置自适应参数（管理员，windowSize 5-100 / kValue 1.0-5.0）
- `GET /api/thresholds/hybrid/status` — 混合模型运行状态（当前模式/异常率/触发原因）
- `GET /api/thresholds/hybrid/performance` — 混合模型实时性能指标（accuracy/precision/recall/f1/fpr）

### 阈值规则 `/api/thresholds/rules`
- `GET /api/thresholds/rules` — 规则列表（支持按功能区筛选，仅返回 status=1 启用规则）
- `POST /api/thresholds/rules` — 新增规则（管理员，`CreateThresholdRuleRequest` DTO + @Valid 校验）
- `PUT /api/thresholds/rules/{id}` — 修改规则（管理员，乐观锁 version 校验，`UpdateThresholdRuleRequest` DTO）
- `DELETE /api/thresholds/rules/{id}` — 删除规则（管理员）
- `POST /api/thresholds/rules/reload` — 重载规则通知 ccswitch（管理员）

### 仪表盘 `/api/dashboard`
- `GET /api/dashboard/overview` — 四大功能区实时概览（分贝值+阈值+指示灯状态+最后更新时间）
- `GET /api/dashboard/areas/{location}` — 功能区详情（最近 10 条+当日统计 min/max/avg/abnormalCount）

### 告警 `/api/alerts`
- `GET /api/alerts` — 告警列表分页（按时间倒序，支持功能区/日期筛选）
- `GET /api/alerts/{id}` — 告警详情
- `PUT /api/alerts/{id}/confirm` — 确认告警（管理员，乐观锁，状态机：未确认→已确认→已处置）
- `PUT /api/alerts/{id}/resolve` — 处置告警（管理员，需先确认，支持填写处理备注）

### 统计可视化 `/api/statistics`
- `GET /api/statistics/timeseries` — 时间序列（分贝曲线+异常点标注，上限 2000 点）
- `GET /api/statistics/areas` — 功能区统计汇总（平均分贝/异常率/告警次数，含全局 summary）
- `GET /api/statistics/models` — 四模型性能对比（固定阈值/业务规则/统计自适应/混合阈值）
- `GET /api/statistics/multi-dim` — 多维度交叉分析（xDim × yDim + alert_count，P2）
- `GET /api/statistics/heatmap` — 热力图数据（功能区×时段→平均分贝矩阵，P2）
- `GET /api/statistics/radar` — 雷达图数据（各功能区多维度指标+STDDEV/噪声类型数，P2）

### 其余模块
- `GET/PUT /api/areas` — 功能区配置 CRUD
- `GET/POST /api/reports` — 报告列表 + 手动生成（TOCTOU 防护已闭合）
- `POST /api/ai/classify` — AI 规则分类触发
- `GET /api/ccswitch/status` — ccswitch 配置服务状态
- `POST /api/data/import` — CSV 文件导入（UUID 重命名防路径穿越）
- `GET /api/data/export-report` — 完整报表 CSV 导出（3 部分：噪声数据+功能区统计+告警统计）

---

## 测试

### 后端测试 (JUnit 5 + Mockito + AssertJ)

```bash
cd backend
mvn compile          # 编译 65 源文件 (0 错误)
mvn test             # 45 个单元测试全部通过
mvn package -DskipTests  # 打包为 noise-1.0.0.jar
```

| 测试类 | 用例数 | 覆盖方法 | 状态 |
|------|:---:|------|:---:|
| ThresholdServiceImplTest | 29 | getCurrentThreshold / checkRecord / createRule / updateRule / deleteRule / reloadRules / getAdaptiveThreshold / updateAdaptiveConfig / getHybridStatus / autoJudgeWithHybrid | ✅ |
| AlertLogServiceImplTest | 16 | createAlert / queryPage / getDetail / confirmAlert / resolveAlert / 乐观锁冲突 / 状态流转 / 并发 | ✅ |

### 前端开发

```bash
cd frontend
pnpm install         # 安装依赖 (Vite 8 + Element Plus + ECharts 6)
pnpm dev             # 开发服务器 http://localhost:5173 (HMR)
pnpm build           # 生产构建到 dist/
```

---

## ccswitch 配置服务

从 `~/.claude/settings.json` 实时读取 AI 模型配置，支持不停机热切换。

| 端点 | 方法 | 说明 |
|------|------|------|
| `http://localhost:5000/` | GET | 服务信息 + 端点列表 |
| `/api/health` | GET | 健康检查（含模型/阈值规则数/配置来源/运行时长） |
| `/api/config/reload` | POST | 从 settings.json 重新加载配置 |
| `/api/threshold-rules` | GET | 获取阈值规则列表（12 条初始规则） |
| `/api/threshold-rules/reload` | POST | 从 threshold_rules.json 重新加载规则到内存 |
| `/api/threshold/compute` | POST | ccswitch 批量阈值计算（SSE 模式） |

**核心能力**: 自动发现 settings.json→回退 settings.local.json / ccswitch 本地代理(127.0.0.1:15721) / 直连 API 双模式 / 模型名净化(去除 `[1M]` 后缀) / 14 个 env 提取 / 多级模型回退 / SSE 推送配置变更 / 文件监控(watchdog)自动热加载

---

## 代码规范

- **2 空格缩进**（后端 Java + 前端 Vue 统一，.editorconfig）
- Java 类名大驼峰 / 方法名小驼峰 / 常量全大写下划线
- Vue `<script setup>` 组合式 API / 单引号优先 / 分号结尾 / `const` `let` 禁止 `var`
- 统一返回 `Result<T>` 包装（`code` + `message` + `data`），静态工厂 `Result.success()` / `Result.error()`
- 数据库统一 MyBatis-Plus `LambdaQueryWrapper`（禁止字符串拼接 SQL）
- 密码 BCrypt 加密存储 (`spring-security-crypto 6.3.4`)
- JWT token 2 小时过期 (JJWT 0.13.0 模块化)，secret 支持 `${JWT_SECRET:default}` 环境变量兜底
- Entity 时间字段统一 `LocalDateTime` / 分贝字段 `BigDecimal` (DECIMAL(5,1))
- 异常统一 `BusinessException(code, message)` + `@RestControllerAdvice` 全局处理（10 种异常类型全覆盖）
- Controller 入参用 DTO + `@Valid` 校验（ThresholdRule DTO 已替换 Map 入参）
- 乐观锁 `version` 字段 + 唯一索引防重
- 日志脱敏工具类 `SensitiveDataUtil`（token/password 脱敏）
- TOCTOU 竞态防护（ConcurrentHashMap 串行化锁）

---

## 与研究报告的对应关系

本系统是研究报告的工程落地版本，核心算法忠实复现，实现方式适配生产环境：

| 研究报告（Python 实验） | 本系统（Java Web 生产） | 说明 |
|------|------|------|
| Pandas 数据预处理 (§3.2) | NoiseRecordServiceImpl 批量导入 + IQR 校验 | 相同逻辑，Java 实现 |
| 业务规则二维字典 (§3.3.1) | ThresholdServiceImpl.getCurrentThreshold() | 规则字典结构一致，新增三级兜底（规则→默认→55） |
| 统计自适应滑动窗口 (§3.3.2) | ThresholdServiceImpl.getAdaptiveThreshold() | μ±kσ 公式一致，窗口/k 值可配置 |
| 混合阈值模型 (§3.3.3) | ThresholdServiceImpl.autoJudgeWithHybrid() | 3 触发条件一致，新增 judged_by_model 标记 |
| Matplotlib 可视化 (§3.5) | ECharts 6 交互式图表 (StatisticsPage) | 功能对等：时间序列/柱状图/性能对比，新增热力图+雷达图 |
| Orange LOF/孤立森林 (§3.4.2) | Orange 工作流配置文件 `data/orange_workflow_config.json` | Orange 作为对比基线保留，生产系统用 Java 实现 |
| Python 核心代码 (§3.4.1) | Java Service 层 (ThresholdServiceImpl) | 算法逻辑一致，Java 版本增加幂等/乐观锁/异常处理 |
| 实验验证 5 折交叉 (§4.1) | StatisticsService 模型性能实时计算 (§4.2) | DB 实时聚合替代离线实验，指标公式一致 |

> **未实现的研究报告内容**：§4.6.2 深度学习(CNN/LSTM)和 §5.2 模型智能化升级属于未来研究方向（研究报告自身定位为"改进方向"和"研究展望"），不在本系统范围内。P2-2 AI 噪声分类已用 10 条启发式规则实现模拟版本。

---

## Q-CR 质量检测历程

| 轮次 | 日期 | 得分 | 修复数 | 关键变化 |
|:---:|------|:---:|:---:|------|
| 第 1 轮 | 2026-06-11 | 83.10 | — | 基线检测，发现 21 项风险（JWT 硬编码/前端逻辑错误/CSV 注入等） |
| 第 2 轮 | 2026-06-11 | 86.80 | 6 | P0×3: JWT 环境变量化 + isAbnormal 类型修复 + CSV 注入防护 |
| 第 3 轮 | 2026-06-11 | 87.55 | 2 | P2×2: GlobalExceptionHandler 日志增强 + 路由守卫 /login 跳转 |
| 第 4 轮 | 2026-06-11 | 89.60 | 3 | P1×3: 模型性能实时计算 + 数据集 CSV + Orange 工作流配置 |
| 第 5 轮 | 2026-06-11 | 90.10 | 2 | P1: pageSize上限校验 + P2: docker-compose.yml 4服务编排 |
| 第 6 轮 | 2026-06-11 | **92.15** | 7 | P1×2: JWT env兜底+异常处理器10类全覆盖; P2×5: DTO替换Map+TOCTOU闭合+全路由懒加载+日志脱敏+45单元测试 |

**54 Iron Laws 合规率**: 96.3% (52/54) | **需求覆盖率**: 93.8% (16 项功能需求中 15 项 ≥90%)

> 📂 完整检测记录见 `docs/对话记录/Q-CR-Cycle-Round1~6-2026-06-11.md`

---

## 提示词指令集 (Prompt 工程)

本项目使用 AI 辅助开发，核心提示词演化记录在 `ai-records/` 目录：

| 文件 | 说明 |
|------|------|
| `噪声实体生成-演化.md` | NoiseRecord Entity + DTO 生成提示词 |
| `阈值Service生成-演化.md` | ThresholdServiceImpl (动态阈值引擎) 生成提示词 |
| `Vue阈值配置页生成-演化.md` | ThresholdConfigPage.vue 生成提示词 |

Skill 目录 `~/.claude/skills/Q-CR/SKILL.md` 包含 Q-CR Omega v1.1 检测引擎完整配置。

---

## 相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 研究报告 | `03-选题库-学生标定卡/校园噪音分贝预警（规则+异常检测类）研究报告.md` | 142 条数据 + 3 种动态阈值模型实验 |
| 需求规格 | `docs/PRD.md` | 13 功能 (P0×7 + P1×5 + P2×1) |
| 概要设计 | `docs/TECH_DESIGN.md` | 架构图 + 模块表 + 路由 + 流程图 + 页面原型 |
| 数据库设计 | `docs/DATABASE_DESIGN.md` | ER 图 + 6 表 DDL + 索引/外键/乐观锁 |
| API 设计 | `docs/API_DESIGN.md` | 52 接口 (11 模块) + 业务异常码表 |
| 部署文档 | `docs/DEPLOY.md` | 8 节 (架构/环境/部署/验证/安全/FAQ/ccswitch/回滚) |
| Docker | `docker-compose.yml` + `Dockerfile` | 4 服务编排 (MySQL+Backend+Frontend+ccswitch) |
| 数据集 | `data/campus_noise_dataset.csv` | 142 条 × 4 字段 |
| Orange 工作流 | `data/orange_workflow_config.json` | 9 Widget + 10 连接 + 5 模型性能基线 |
| 对话记录 | `docs/对话记录/` | R-00~R-08 审查报告 + Q-CR Round 1~6 检测报告 |

---

## 团队

- **qxw** — 全栈开发 + AI 提示词工程 + Q-CR 质量检测

---

## 许可证

本项目为课程设计项目。数据集和研究方法详见研究报告。
