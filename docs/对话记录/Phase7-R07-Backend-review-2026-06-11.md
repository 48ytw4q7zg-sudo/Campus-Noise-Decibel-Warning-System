# Phase 7 R-07 全栈代码审查 — Backend

## 审查概要

审查范围：11 Controller + 9 Service Impl + 3 Common + 2 Config + 1 Interceptor + 1 Util
审查日期：2026-06-11

| 维度 | 状态 | 高/中/低 问题数 |
|------|:--:|:--:|
| 命名规范 | 通过 | 0/1/0 |
| 分层架构 | 通过 | 0/0/1 |
| API 合规 | 通过 | 0/1/0 |
| MyBatis-Plus | 通过 | 0/1/0 |
| 错误处理 | 通过 | 0/0/1 |
| 安全 | 通过 | 0/1/0 |
| 跨模块 | 通过 | 0/1/0 |
| 性能 | 通过 | 0/1/1 |

---

## 维度1 命名规范

**中**: `NoiseRecordServiceImpl.java:36` — `AREAS` 与 `VALID_LOCATIONS` 内容重复（同为4个功能区），两处维护易不一致。[建议] 删除 `AREAS`，统一引用 `VALID_LOCATIONS`，或用 `List.copyOf(VALID_LOCATIONS)` 保持同步。

## 维度2 分层架构

**低**: `NoiseController.java` — 导入/导出逻辑混合在 Controller 中做 CSV 解析和错误校验，按 §二·一 分层职责应提取到 Service。[建议] 抽取 `CsvImportValidator` 或 `DataImportService`。

## 维度3 API 合规

**中**: `StatisticsController.java` — `getRadar` 返回的 Map 键名与 `API_DESIGN.md §3.8` 声明的 `data` / `dimensionLabels` 一致，但 `getHeatmap` 返回的三层嵌套 `data: [[...], [...]]` 在前端解包时需额外确认（Vector 到 JSON 自动转 `List<List<BigDecimal>>` 正确）。

## 维度4 MyBatis-Plus 用法

**中**: `NoiseRecordMapper.java` — `selectRecentForAdaptive`、`selectAreaStats`、`selectAlertCountByArea` 等 7 个 `@Select` 注解方法全部在 Mapper 接口中（无 XML）。[确认] 是否在 `application.yml` 中配置 `mapper-locations` 路径？`classpath:mapper/*.xml` 现有目录为空，这些 `@Select` 不需要 XML — 但配置项留着无效路径，建议清理或补注释说明。

## 维度5 错误处理

**低**: `CcswitchServiceImpl.java:43` — `extractBody` 返回 `Collections.emptyMap()` 时消费者（Controller）收到 `data = {}`，code 仍为 200。[建议] ccswitch 不可用时抛 `BusinessException(7001)` 而非返回空 Map 后静默 — 但当前实现已在 `getStatus()` > `reloadConfig()` 等调用处抛异常，`extractBody` 仅用于内部正常路径，无实际问题。

## 维度6 安全

**中**: `application.yml:8-9` — 数据库密码 `root:root` 明文写在配置文件中。[必须] 部署前改为环境变量或 `application-local.yml`（已加入 .gitignore）。JWT secret 使用固定字符串 `campus-noise-warning-system-jwt-secret-key-2026`（25 字符），[建议] 升级为 ≥256 bit 的随机密钥并放环境变量。

## 维度7 跨模块问题

**中**: `ThresholdServiceImpl` ↔ `NoiseRecordServiceImpl` 存在双向依赖风险。`NoiseRecordServiceImpl` 已注入 `ThresholdService`（上轮修复），但 `ThresholdServiceImpl.checkRecord` 也调用 `NoiseRecordMapper`（单向，安全）。[建议] 在 CLAUDE.md §二·一 中标注 "Service 之间可依赖但避免循环" 约束。

## 维度8 性能

**中**: `getHybridStatus()` — 对 4 个功能区 × 3 个窗口各执行一次 `selectRecentForAdaptive`（共 12 次 DB 查询）。[建议] 合并为单次查询：`SELECT ... WITHIN GROUP ORDER BY time_point DESC LIMIT ?` 一次性取各功能区最近 N 条后在 Java 层分组计算。

**低**: `generateSimulatedData()` — `@Scheduled(fixedRate = 300000)` 无 distributed lock。[P2 雅量] 引入 ShedLock 或仅在单实例部署场景运行。

---

## 总结（严重度排序）

| # | 严重度 | 文件 | 问题 |
|:--:|:--:|------|------|
| R-07-BE-1 | 中 | application.yml | DB 密码明文 + JWT secret 强度不足 |
| R-07-BE-2 | 中 | ThresholdServiceImpl | getHybridStatus 12 次 DB 查询 |
| R-07-BE-3 | 中 | NoiseRecordMapper | @Select 注解方法 7 个与空 XML 目录并存 |
| R-07-BE-4 | 中 | NoiseRecordServiceImpl | AREAS/VALID_LOCATIONS 重复定义 |
| R-07-BE-5 | 低 | NoiseController | CSV 解析逻辑在 Controller |
| R-07-BE-6 | 低 | ThresholdServiceImpl | generateSimulatedData 无分布式锁 |

后端整体质量：架构清晰、分层严格、MyBatis-Plus 使用规范。主要风险集中在配置安全（密码明文）和性能热点（混合模型多查询）。
