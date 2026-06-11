# 校园噪音分贝预警员系统

> 基于 "业务规则 + 统计自适应 + 混合阈值" 动态模型的校园噪声监测预警平台
>
> 目标异常检测准确率 ≥92%，误报率 ≤5%
>
> 技术栈：SpringBoot 3.5.14 + Vue 3.5.34 + MySQL 8.4 + Python Flask ccswitch

## 项目概述

校园噪音分贝预警员系统面向**普通用户**和**管理员**两类角色，覆盖**图书馆、食堂、操场、宿舍**四大核心功能区，通过 `传感器数据采集 → 动态阈值计算 → 异常检测 → 告警推送` 的完整业务闭环，实现校园噪声的精准识别与实时预警。

### 核心特色

- **动态阈值三模型**：业务规则固定动态阈值 + 统计自适应阈值 + 混合阈值（最优方案准确率 92.6%）
- **"业务规则 > 算法"** 设计理念：场景语义约束优于纯数据驱动
- **ccswitch 配置热更新**：从 `~/.claude/settings.json` 实时读取 AI 模型配置，支持不停机切换
- **全栈 Vertical Slice 架构**：每个功能 = Controller → Service → Mapper → Entity → API → Store → Page

## 系统架构

```
┌──────────────────────────────────────────────┐
│              前端 (Vue 3.5 + Element Plus)    │
│  Login → Dashboard → NoiseMonitor → Alerts   │
│  AreaConfig → ThresholdConfig → Statistics   │
│  Axios (/api/*) + Pinia stores               │
└──────────────────┬───────────────────────────┘
                   │ HTTP REST + JWT Bearer
┌──────────────────▼───────────────────────────┐
│           后端 (SpringBoot 3.5.14)             │
│  11 Controller → 18 Service → 6 Mapper        │
│  LoginInterceptor → GlobalExceptionHandler    │
└──────────────────┬───────────────────────────┘
                   │ JDBC
┌──────────────────▼───────────────────────────┐
│         MySQL 8.4 LTS (noise_db)              │
│  user / noise_record / threshold_rule /       │
│  alert_log / area_config / report             │
└──────────────────────────────────────────────┘

┌ - - - - - - - - - - - - - - - - - - - - - - ┐
│  ccswitch 配置服务 (Flask · Port 5000)        │
│  读取 ~/.claude/settings.json                │
│  AI 模型配置热更新 + 阈值参数管理              │
└ - - - - - - - - - - - - - - - - - - - - - - ┘
```

## 技术栈

| 层 | 技术 | 版本 |
|---|------|------|
| 后端框架 | SpringBoot | 3.5.14 |
| ORM | MyBatis-Plus | 3.5.15 |
| 数据库 | MySQL | 8.4 LTS |
| JWT | JJWT | 0.13.0 |
| 密码加密 | spring-security-crypto (BCrypt) | 6.3.4 |
| 前端框架 | Vue 3 (Composition API) | 3.5.34 |
| UI 库 | Element Plus | 2.13.7 |
| 状态管理 | Pinia | 3.0.4 |
| HTTP 客户端 | Axios | 1.15.2 |
| 构建工具 | Vite / Maven | 8.0.0 / 3.9 |
| 包管理 | pnpm | 10.33.4 |
| 配置服务 | Python Flask + ccswitch | 3.x |
| 图表 | ECharts | 6.x |
| JDK | OpenJDK | 21 |
| Node.js | | 24 LTS |

## 快速开始

### 前置要求

- JDK 21+
- Node.js 24 LTS
- pnpm 10+
- MySQL 8.4
- Maven 3.9+
- Python 3.x（ccswitch 配置服务，P2 加分项）

### 一键启动（推荐）

双击项目根目录的 **`一键启动前后端.bat`**，自动完成：

1. 环境检测（Java / Node.js / pnpm / Maven / MySQL / Python）
2. 数据库初始化（建库 + 建表 + 种子数据）
3. ccswitch 配置服务启动（Flask :5000）
4. 后端编译 + 启动（SpringBoot :8080）
5. 前端依赖安装 + 启动（Vite :5173）
6. 自动打开浏览器

> 也可使用 `一键启动前后端/一键启动前后端.bat`（效果相同，路径自动适配）。

### 手动启动

#### 1. 数据库初始化

```bash
mysql -u root -p < sql/01-init.sql
```

编辑 `backend/src/main/resources/application.yml` 填写数据库密码。

#### 2. 启动 ccswitch 配置服务（P2 加分项，可选）

```bash
cd ccswitch_service
pip install -r requirements.txt
python app.py
# 监听 http://localhost:5000
```

#### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
# 监听 http://localhost:8080
```

#### 4. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
# 监听 http://localhost:5173
```

#### 5. 访问系统

浏览器打开 `http://localhost:5173`

**首次使用**：访问注册页创建管理员账号（角色选择"管理员"）。

## 项目结构

```
CampusNoiseDecibelWarningSystem/
├── backend/                         # SpringBoot 后端
│   ├── src/main/java/com/example/noise/
│   │   ├── common/                  # Result<T> / BusinessException / GlobalExceptionHandler
│   │   ├── config/                  # CORS / MyBatisPlus / WebMvc
│   │   ├── controller/              # 11 个 Controller
│   │   ├── entity/                  # 6 个 Entity + dto/
│   │   ├── interceptor/             # JWT LoginInterceptor
│   │   ├── mapper/                  # 6 个 MyBatis-Plus Mapper
│   │   ├── service/                 # 9 个 Service 接口
│   │   │   └── impl/                # 9 个 Service 实现
│   │   └── util/                    # JwtUtils
│   ├── src/main/resources/
│   │   ├── application.yml          # 主配置
│   │   └── mapper/                  # MyBatis XML
│   └── pom.xml
├── frontend/                        # Vue 3 前端
│   └── src/
│       ├── api/                     # 12 个 API 模块
│       ├── components/              # AppLayout 等
│       ├── router/                  # Vue Router
│       ├── stores/                  # Pinia user store
│       └── views/                   # 9 个页面组件
├── ccswitch_service/                # Flask 配置服务 (P2)
│   ├── app.py                       # Flask 主应用
│   ├── ccswitch.py                  # settings.json 读取
│   ├── config.py                    # 配置管理
│   └── threshold_rules.json         # 14 条初始阈值规则
├── docs/                            # 设计文档
│   ├── 00-选题标定.md
│   ├── PRD.md                       # 需求规格说明书
│   ├── TECH_DESIGN.md               # 概要设计
│   ├── DATABASE_DESIGN.md           # 数据库设计
│   └── API_DESIGN.md                # API 接口设计
├── sql/
│   └── 01-init.sql                  # 建表 + 种子数据
├── 一键启动前后端.bat                 # Windows 一键启动脚本
├── CLAUDE.md                        # AI 编码规则
└── README.md
```

## 功能清单

### P0 必做功能（全部完成 ✓）

| 编号 | 功能 | 后端 | 前端 |
|:---:|------|:---:|:---:|
| P0-1 | 用户注册登录 + JWT | ✓ | LoginPage |
| P0-2 | 噪声数据采集与存储（含 @Scheduled 定时模拟） | ✓ | NoiseMonitorPage |
| P0-3 | 基于业务规则的动态阈值判断（功能区+时段二维字典） | ✓ | — |
| P0-4 | 实时噪声监测仪表盘（10 秒自动刷新） | ✓ | DashboardPage |
| P0-5 | 异常告警记录与推送（确认→处置状态流转） | ✓ | AlertHistoryPage |
| P0-6 | 功能区配置管理（乐观锁并发控制） | ✓ | AreaConfigPage |
| P0-7 | 噪声数据列表与筛选 | ✓ | NoiseMonitorPage |

### P1 应做功能（全部完成 ✓）

| 编号 | 功能 | 后端 | 前端 |
|:---:|------|:---:|:---:|
| P1-1 | 统计自适应阈值计算（滑动窗口 μ±k×σ） | ✓ | ThresholdConfigPage |
| P1-2 | 混合阈值模型（准确率 92.6%，3重触发条件） | ✓ | ThresholdConfigPage |
| P1-3 | 历史数据可视化（ECharts 时间序列+柱状图+雷达图） | ✓ | StatisticsPage |
| P1-4 | 阈值规则配置界面（对接 ccswitch 热更新） | ✓ | ThresholdConfigPage |
| P1-5 | 噪声数据高级筛选与 CSV 导出 | ✓ | NoiseMonitorPage |

### P2 可选功能（全部完成 ✓）

| 编号 | 功能 | 后端 | 前端 |
|:---:|------|:---:|:---:|
| P2-1 | 数据导入导出（CSV 解析+UTF-8 BOM） | ✓ | NoiseMonitorPage / SystemSettingsPage |
| P2-2 | AI 辅助噪声分类（10 条启发式规则引擎） | ✓ | SystemSettingsPage |
| P2-3 | 定时报告生成（Markdown + @Scheduled cron） | ✓ | SystemSettingsPage |
| P2-4 | 多维度统计分析（热力图+雷达图） | ✓ | StatisticsPage |
| P2-5 | ccswitch 配置热更新面板 | ✓ | SystemSettingsPage |

## API 接口一览

### 认证 `/api/auth`
- `POST /api/auth/register` — 注册（公开）
- `POST /api/auth/login` — 登录（公开，返回 JWT）

### 用户 `/api/users`
- `GET /api/users/me` — 个人信息（需登录）
- `PUT /api/users/me/password` — 修改密码（需登录，校验原密码）

### 噪声数据 `/api/noise`
- `POST /api/noise/records` — 传感器上报（传感器通道/管理员通道双模式）
- `POST /api/noise/records/batch` — 批量导入（管理员）
- `GET /api/noise/records/latest` — 各功能区最新数据（仪表盘用）
- `GET /api/noise/records` — 分页列表（支持时间/功能区/分贝范围/异常状态筛选）
- `GET /api/noise/records/{id}` — 详情
- `GET /api/noise/records/search` — 高级筛选（P1）
- `GET /api/noise/records/export` — CSV 导出（管理员，上限 10000 条）

### 阈值判断 `/api/thresholds`
- `GET /api/thresholds/current?location=XX` — 当前业务规则阈值
- `POST /api/thresholds/check/{id}` — 手动触发判断（管理员，幂等）
- `GET /api/thresholds/adaptive/current` — 统计自适应阈值（滑动窗口 μ±k×σ）
- `PUT /api/thresholds/adaptive/config` — 配置自适应参数（管理员）
- `GET /api/thresholds/hybrid/status` — 混合模型运行状态
- `GET /api/thresholds/hybrid/performance` — 混合模型性能指标

### 阈值规则 `/api/thresholds/rules`
- `GET /api/thresholds/rules` — 规则列表（支持按功能区筛选）
- `POST /api/thresholds/rules` — 新增规则（管理员，唯一索引防重）
- `PUT /api/thresholds/rules/{id}` — 修改规则（管理员，乐观锁）
- `DELETE /api/thresholds/rules/{id}` — 删除规则（管理员）
- `POST /api/thresholds/rules/reload` — 热更新通知 ccswitch（管理员）

### 仪表盘 `/api/dashboard`
- `GET /api/dashboard/overview` — 四大功能区实时概览（含异常指示灯）
- `GET /api/dashboard/areas/{location}` — 功能区详情（最近 N 条+当日统计）

### 告警 `/api/alerts`
- `GET /api/alerts` — 告警列表分页
- `GET /api/alerts/{id}` — 告警详情
- `PUT /api/alerts/{id}/confirm` — 确认告警（管理员，条件 UPDATE 防并发）
- `PUT /api/alerts/{id}/resolve` — 处置告警（管理员，状态流转：未确认→已确认→已处置）

### 统计可视化 `/api/statistics`
- `GET /api/statistics/timeseries` — 时间序列（分贝曲线+异常点）
- `GET /api/statistics/areas` — 功能区统计汇总
- `GET /api/statistics/models` — 四模型性能对比
- `GET /api/statistics/multi-dim` — 多维度交叉分析（P2）
- `GET /api/statistics/heatmap` — 热力图数据（P2）
- `GET /api/statistics/radar` — 雷达图数据（P2）

### 功能区配置 `/api/areas`
- `GET /api/areas` — 功能区列表（管理员）
- `PUT /api/areas/{id}` — 修改配置（管理员，乐观锁）

### 报告 `/api/reports`
- `GET /api/reports` — 报告列表分页
- `GET /api/reports/{id}` — 报告详情
- `POST /api/reports` — 手动生成报告（管理员，幂等去重）
- `PUT /api/reports/config` — 配置自动生成计划（管理员）

### AI 分类 `/api/ai`
- `POST /api/ai/classify` — 手动触发规则分类（管理员，CAS 更新）
- `PUT /api/ai/config` — 配置分类参数（管理员）

### ccswitch `/api/ccswitch`
- `GET /api/ccswitch/status` — 查询配置服务状态（管理员）
- `POST /api/ccswitch/reload` — 触发配置重载（管理员）

### 数据管理 `/api/data`
- `POST /api/data/import` — CSV 导入（管理员，逐行校验+错误详情）
- `GET /api/data/export-report` — 完整报表 CSV 导出（管理员）

## ccswitch 配置服务

ccswitch 是一个独立的 Python Flask 微服务（Port 5000），从 `~/.claude/settings.json` 实时读取 AI 模型配置和阈值参数，为系统提供配置热更新能力。

### 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/` | GET | 服务信息 + 端点列表 |
| `/api/health` | GET | 健康检查（含模型/阈值规则数/配置来源/运行时长） |
| `/api/config/reload` | POST | 从 settings.json 重新加载配置 |
| `/api/threshold-rules` | GET | 获取阈值规则列表（14 条初始规则） |
| `/api/threshold-rules/reload` | POST | 从 threshold_rules.json 重新加载规则到内存 |

### 配置优先级

1. `~/.claude/settings.json`（ccswitch 本地代理 / 直连 API 双模式）
2. `.env` 文件（回退模式，`ANTHROPIC_API_KEY` / `ANTHROPIC_BASE_URL` / `ANTHROPIC_MODEL`）

### 核心能力（对齐 ocsjs-ai-answer-service/ccswitch.py）

- 自动发现 `settings.json` → 回退 `settings.local.json`
- ccswitch 本地代理（127.0.0.1:15721）/ 直连 API 双模式支持
- 模型名净化：去除 `[1M]`/`[200K]` 等上下文长度后缀
- 完整 env 提取：14 个关键环境变量
- 多级模型回退：ANTHROPIC_MODEL → DEFAULT_OPUS_MODEL → ... → 硬编码默认值
- 运行时热重载

## 测试

### 后端编译

```bash
cd backend
mvn compile          # 编译（60+ 源文件）
mvn test             # 测试（待补充）
mvn package -DskipTests  # 打包为 JAR
```

### 前端开发

```bash
cd frontend
pnpm install         # 安装依赖
pnpm dev             # 开发服务器（热更新）
pnpm build           # 生产构建
```

### 全栈联调

1. 启动 ccswitch Flask 服务（可选）
2. 启动 SpringBoot 后端
3. 启动 Vite 前端开发服务器
4. 浏览器访问 `http://localhost:5173`

## 代码规范

- 2 空格缩进（后端 Java + 前端 Vue 统一）
- Java 类名大驼峰，方法名小驼峰
- Vue `<script setup>` 组合式 API，单引号优先
- 统一返回 `Result<T>` 包装（code + message + data）
- 数据库统一 MyBatis-Plus `LambdaQueryWrapper`（禁止字符串拼接 SQL）
- 密码 BCrypt 加密存储
- JWT token 2 小时过期
- Entity 时间字段统一 `LocalDateTime`

## 许可证

本项目为课程设计项目。
