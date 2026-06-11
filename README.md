# 校园噪音分贝预警员系统

> 基于"业务规则+统计自适应+混合阈值"动态模型的校园噪声监测预警平台，覆盖图书馆、食堂、操场、宿舍四大功能区，目标异常检测准确率≥92%

## 一、项目简介
- **题目**:校园噪音分贝预警员系统
- **核心实体**:用户(user)、噪声记录(noise_record)、阈值规则(threshold_rule)、告警记录(alert_log)、功能区配置(area_config)、统计数据(statistics)
- **角色**:普通用户（可查看噪声数据+告警+统计图表）/ 管理员（可管理功能区配置+阈值规则+系统设置+用户管理）
- **当前 Phase**:Phase 0(项目初始化 · 由 /rules-updater 自动更新)

## 二、技术栈

### 后端
- JDK 21 + SpringBoot 3.5.14 + MyBatis-Plus 3.5.15(starter + jsqlparser 子模块,3.5.9+ 拆包后必须同时引入)
- MySQL 8.4 LTS(驱动 mysql-connector-j 8.4.0)
- JJWT 0.13.0(模块化引入)+ Lombok 1.18.46

### 前端
- Vue 3.5.34 + Vue Router 5.0.6 + Pinia 3.0.4
- Element Plus 2.13.7 + Axios 1.15.2 + Vite 8.0.0

### ccswitch 配置服务
- Python Flask + ccswitch 配置读取模块(动态阈值热更新)

## 三、项目结构
```
CampusNoiseDecibelWarningSystem/
├── backend/                # SpringBoot 3 后端 (Port 8080)
│   ├── pom.xml
│   └── src/main/java/com/example/noise/
│       ├── Application.java
│       ├── config/         # CORS / MyBatis-Plus / WebMvc 配置
│       ├── common/         # Result / BusinessException / GlobalExceptionHandler
│       ├── util/           # JwtUtils 等工具类
│       ├── interceptor/    # LoginInterceptor JWT 校验
│       ├── controller/     # REST 控制器 (Phase 4)
│       ├── service/        # 业务逻辑 (Phase 4)
│       ├── mapper/         # MyBatis-Plus Mapper (Phase 4)
│       └── entity/         # ORM 实体 + DTO (Phase 4)
├── frontend/               # Vue 3 前端 (Port 5173)
│   └── src/
│       ├── views/          # 页面组件 (*Page.vue)
│       ├── components/     # 可复用组件
│       ├── api/            # axios 实例 + 业务 API 模块
│       ├── stores/         # Pinia 状态管理
│       ├── router/         # Vue Router 路由
│       └── styles/         # 全局样式
├── cswitch_service/        # Python Flask ccswitch 配置服务 (Port 5000 · P2 加分项)
├── docs/                   # 文档 (PRD / TECH_DESIGN / DATABASE_DESIGN / API_DESIGN / DEPLOY)
├── sql/                    # 数据库脚本 (Phase 2)
├── ai-records/             # AI 对话片段归档
├── .claude/                # AI 编码命令 (教师维护)
├── CLAUDE.md               # AI 编码规则 (教师维护)
└── AGENTS.md               # 开发协作规则
```

## 四、数据库设计
- 表数量:6-7(Phase 2 /db-designer 生成后填,详见 [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md))
- 表名清单:user / noise_record / threshold_rule / alert_log / area_config / statistics

## 五、API 接口
- 接口数量:14-16(Phase 3 /api-designer 生成后填,详见 [docs/API_DESIGN.md](docs/API_DESIGN.md))
- URL 前缀:`/api/...`

## 六、快速开始

### 后端
```bash
cd backend
mvn clean compile
mvn spring-boot:run    # 启动后端 http://localhost:8080
```

### 前端
```bash
cd frontend
pnpm install           # 用 pnpm 不要用 npm/yarn(详见 CLAUDE.md §一·一·前端)
pnpm dev               # 启动前端 http://localhost:5173
```

### ccswitch 配置服务 (可选加分项)
```bash
cd cswitch_service
pip install -r requirements.txt
python app.py          # 启动配置服务 http://localhost:5000
```

> ⚠️ 启动前先按 08b §3 改 `backend/src/main/resources/application.yml` 数据库密码。
> 💡 还没装 pnpm? 跑 `npm install -g pnpm`(详见 08a §5 末尾 pnpm 安装步骤)

## 七、文档索引
- [PRD 需求规格](docs/PRD.md)
- [概要设计](docs/TECH_DESIGN.md)
- [数据库设计](docs/DATABASE_DESIGN.md)
- [API 设计](docs/API_DESIGN.md)
- [部署文档](docs/DEPLOY.md)
- [AI 对话记录](docs/对话记录/)
- [选题标定](docs/00-选题标定.md)

## 八、验收清单(05-验收方案 V4-2 对齐)
- [ ] 5 项硬地基:backend 编译通过 / frontend 跑通 / 数据库就位 / Gitee push / CLAUDE.md 完整
- [ ] commit ≥ 30 次,跨度 ≥ 12 天
- [ ] ai-records ≥ 21 个对话片段(覆盖 ≥ 3 个 v1→v2→v3 演化记录)
- [ ] docs/ 完整(PRD / TECH_DESIGN / DATABASE_DESIGN / API_DESIGN 全)
