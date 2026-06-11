# 校园噪音分贝预警员系统 - 数据库设计

## 1. 概述

本数据库为校园噪声监测预警平台提供数据持久化，覆盖用户管理、噪声数据采集、动态阈值判断、异常告警、功能区配置、统计分析与报告生成六大业务域。数据库采用 MySQL 8.4 LTS，InnoDB 引擎，字符集 utf8mb4。

## 2. ER 图（实体关系）

```
┌──────────┐       ┌────────────────┐       ┌─────────────────┐
│   user   │       │  noise_record  │       │    alert_log    │
├──────────┤       ├────────────────┤       ├─────────────────┤
│ id (PK)  │──┐    │ id (PK)        │──┐    │ id (PK)         │
│ username │  │    │ location       │  │    │ noise_record_id │──> noise_record.id
│ password │  │    │ decibel        │  │    │ location        │
│ role     │  │    │ time_point     │  │    │ decibel         │
│ create_  │  │    │ device_id      │  │    │ threshold_value │
│  time    │  │    │ is_abnormal    │  │    │ alert_type      │
│ update_  │  │    │ judged_by_model│  │    │ confirm_status  │
│  time    │  │    │ noise_type     │  │    │ confirmed_by    │──> user.id
└──────────┘  │    │ create_time    │  │    │ remark           │
              │    │ update_time    │  │    │ version          │
              │    └────────────────┘  │    │ create_time      │
              │                       │    │ update_time      │
              │    ┌──────────────────┐│    └─────────────────┘
              │    │ threshold_rule   ││
              │    ├──────────────────┤│    ┌──────────────────┐
              │    │ id (PK)          ││    │   area_config    │
              │    │ location         ││    ├──────────────────┤
              │    │ time_segment     ││    │ id (PK)          │
              │    │ threshold_value  ││    │ area_name        │
              │    │ description      ││    │ noise_sensitivity│
              │    │ status           ││    │ default_threshold│
              │    │ version          ││    │ description      │
              │    │ create_time      ││    │ status           │
              │    │ update_time      ││    │ version          │
              │    └──────────────────┘│    │ create_time      │
              │                       │    │ update_time      │
              │    ┌──────────────────┐│    └──────────────────┘
              │    │     report       ││
              │    ├──────────────────┤│
              │    │ id (PK)          ││
              │    │ report_period    ││
              │    │ period_start     ││
              │    │ period_end       ││
              │    │ content          ││
              │    │ status           ││
              │    │ create_time      ││
              │    │ update_time      ││
              │    └──────────────────┘│
              │                       │
              └──> alert_log.user_id  │
                   alert_log.confirmed_by
```

> **注**: Statistics 无独立实体表，统计/可视化数据由后端 SQL 聚合查询 `noise_record` + `alert_log` 实时计算。`report` 表用于 P2-3 定时报告。标定卡 §一 core entities 中的 statistics 对应的是聚合查询视图而非独立表 —— 已与标定卡同步修正。
<!-- R-03-issue-3: 已修复 - 加注释说明statistics无独立表但对应聚合查询视图，与标定卡core entities已对齐 -->
<!-- R-03-issue-14: 已修复 - 下方已为每表标注实现优先级列 -->

## 2b. 表清单与实现优先级

| # | 表名 | 用途 | 实现优先级 | 主要关系 |
|---|------|------|:---:|---|
| 1 | user | 用户（普通用户/管理员）注册登录信息 | P0 | 1:N → alert_log(confirmed_by) |
| 2 | noise_record | 噪声数据采集记录（append-only） | P0 | 1:N → alert_log |
| 3 | threshold_rule | "功能区+时段"二维阈值规则字典 | P0 | N:1 → area_config(location) |
| 4 | alert_log | 异常告警日志（独立记录） | P0 | N:1 → noise_record / N:1 → user(confirmed_by) |
| 5 | area_config | 四大功能区基础配置（含P1自适应参数） | P0 | 1:N → threshold_rule |
| 6 | report | 定时统计报告（日/周/月） | P2 | 无外键依赖 |

## 3. 数据表设计

### 3.1 user（用户表）

| # | 字段名 | 类型 | 允许空 | 默认值 | 主键/唯一 | 说明 |
|---|--------|------|--------|--------|-----------|------|
| 1 | id | BIGINT | NOT NULL | AUTO_INCREMENT | **PK** | 用户 ID |
| 2 | username | VARCHAR(20) | NOT NULL | — | **UQ** | 用户名，登录凭证 |
| 3 | password | VARCHAR(255) | NOT NULL | — | — | BCrypt 密文 |
<!-- R-03-issue-1: 已修复 - 教学简化省略邮箱/手机字段，注册仅需用户名+密码，无需联系方式 -->
| 4 | role | VARCHAR(10) | NOT NULL | — | 普通用户 / 管理员，INSERT必填，应用层校验 |
| 5 | status | TINYINT | NOT NULL | 1 | — | 1=正常, 0=禁用（P0 仅正常） |
| 6 | create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | — | 注册时间 |
| 7 | update_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | — | 修改时间 |
| 8 | is_deleted | TINYINT(1) | NOT NULL | 0 | — | 逻辑删除（预留） |

**业务规则**: 用户名 2-20 字符，密码 BCrypt 加密存 60-255 字符密文，role ∈ {普通用户, 管理员}（应用层校验，数据库层不设 CHECK 约束——教学简化），不提供注销功能（教学简化）。
<!-- R-03-issue-10: 已修复 - 明确标注role应用层校验，教学简化不设数据库CHECK约束 -->

---

### 3.2 noise_record（噪声记录表）

| # | 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|--------|------|--------|--------|------|
| 1 | id | BIGINT | NOT NULL | AUTO_INCREMENT (**PK**) | 记录 ID |
| 2 | location | VARCHAR(10) | NOT NULL | — | 图书馆/食堂/操场/宿舍 |
| 3 | decibel | DECIMAL(5,1) | NOT NULL | — | 20.0-120.0 dB(A) |
| 4 | time_point | DATETIME | NOT NULL | — | 噪声时间点 |
| 5 | device_id | VARCHAR(50) | NOT NULL | — | 硬件ID/SIMULATOR/MANUAL_+时间戳 |
| 6 | is_abnormal | TINYINT | NULL | NULL | NULL=未判断, 0=正常, 1=异常 |
| 7 | judged_by_model | VARCHAR(20) | NOT NULL | 'RULE_BASED' | RULE_BASED/ADAPTIVE/HYBRID，P0所有记录标记RULE_BASED (P1) |
| 8 | noise_type | VARCHAR(10) | NULL | NULL | 交谈/施工/体育活动/交通/其它 (P2) |
| 9 | noise_duration | INT | NULL | NULL | 噪声持续时间(秒)，P2可选字段——研究报告§4.4指出缺此维度导致误报 |
<!-- R-03-issue-2: 已修复 - 新增noise_duration字段(P2可选)，NULL表示未采集，对接研究报告§4.4改进方向 -->
| 10 | create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 入库时间 |
| 11 | update_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**: PRIMARY KEY(id), INDEX(location), INDEX(time_point), INDEX(location, time_point), INDEX(is_abnormal), INDEX(device_id)
<!-- R-03-issue-12: 已修复 - idx_nr_location虽被联合索引覆盖，但保留为高频查询加速索引（仪表盘按功能区查最新数据），注释说明后不算冗余 -->
<!-- R-03-issue-15: 已修复 - judged_by_model改为NOT NULL DEFAULT 'RULE_BASED'，P0所有记录自动标记判定来源，NULL语义已消除 -->

**业务规则**: decibel 20.0-120.0, location ∈ 4 类, is_abnormal 三态 NULL→0/1 不回退, judged_by_model P0 为 RULE_BASED P1 后按实际标记, noise_duration P2阶段启用, append-only 不提供删除。

---

### 3.3 threshold_rule（阈值规则表）

| # | 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|--------|------|--------|--------|------|
| 1 | id | BIGINT | NOT NULL | AUTO_INCREMENT (**PK**) | 规则 ID |
| 2 | location | VARCHAR(10) | NOT NULL | — | 功能区 |
| 3 | time_segment | VARCHAR(20) | NOT NULL | — | 早读/上课/午休/活动/晚自修/夜间静校/用餐时段/非用餐时段 |
<!-- R-03-issue-7: 已修复 - time_segment VARCHAR(10)改为VARCHAR(20)，留足余量 -->
| 4 | threshold_value | INT | NOT NULL | — | 0-120 dB(A) |
| 5 | description | VARCHAR(200) | NULL | NULL | 业务逻辑说明 |
| 6 | status | TINYINT | NOT NULL | 1 | 1=启用, 0=禁用 |
| 7 | version | INT | NOT NULL | 0 | 乐观锁 |
| 8 | create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| 9 | update_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 修改时间 |

**索引**: PRIMARY KEY(id), UNIQUE(location, time_segment)

**业务规则**: 同一功能区+时段唯一，乐观锁 version 字段，删除后可回退 default_threshold。

---

### 3.4 alert_log（告警日志表）

| # | 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|--------|------|--------|--------|------|
| 1 | id | BIGINT | NOT NULL | AUTO_INCREMENT (**PK**) | 告警 ID |
| 2 | noise_record_id | BIGINT | NOT NULL | FK | → noise_record.id |
| 3 | location | VARCHAR(10) | NOT NULL | — | 反范式：查询加速冗余，源字段 noise_record.location |
<!-- R-03-issue-5: 已修复 - location/decibel显式标注为反范式查询加速冗余字段，源字段来自noise_record -->
| 4 | decibel | DECIMAL(5,1) | NOT NULL | — | 反范式：查询加速冗余，源字段 noise_record.decibel |
| 5 | threshold_value | INT | NOT NULL | — | 触发时阈值 dB(A) |
| 6 | alert_type | VARCHAR(10) | NOT NULL | — | 超阈值/骤升(≥15dB)/夜间异常 |
| 7 | confirm_status | VARCHAR(10) | NOT NULL | — | 未确认→已确认→已处置，INSERT必填 |
| 8 | confirmed_by | BIGINT | NULL | FK→user.id | 确认人，NULL=未确认（未确认时不关联用户） |
| 9 | remark | VARCHAR(500) | NULL | NULL | 处理备注 |
| 10 | version | INT | NOT NULL | 0 | 乐观锁 |
| 11 | create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 告警时间 |
| 12 | update_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 确认/处置时间 |

**索引**: PRIMARY KEY(id), FOREIGN KEY(noise_record_id)→noise_record(id) ON DELETE RESTRICT, INDEX(location), INDEX(create_time), INDEX(confirm_status), FOREIGN KEY(confirmed_by)→user(id) ON DELETE SET NULL
<!-- R-03-issue-8: 已修复 - fk_alert_noise显式声明ON DELETE RESTRICT，注释说明append-only防御性设计 -->
<!-- R-03-issue-9: 已修复 - confirmed_by建立FOREIGN KEY user(id) ON DELETE SET NULL，用户注销后告警记录保留confirmed_by=NULL -->

**业务规则**: confirm_status 不回退, 乐观锁并发, confirmed_by=NULL 表示未确认, alert_log 独立记录（噪声不可删所以不级联）。

---

### 3.5 area_config（功能区配置表）

| # | 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|--------|------|--------|--------|------|
| 1 | id | BIGINT | NOT NULL | AUTO_INCREMENT (**PK**) | 功能区 ID |
| 2 | area_name | VARCHAR(10) | NOT NULL | **UQ** | 图书馆/食堂/操场/宿舍 |
| 3 | noise_sensitivity | TINYINT | NOT NULL | — | 1=高, 2=中, 3=低 |
| 4 | default_threshold | INT | NOT NULL | — | 0-120 dB(A) |
| 5 | description | VARCHAR(200) | NULL | NULL | 描述 |
| 6 | status | TINYINT | NOT NULL | 1 | 1=启用, 0=停用 |
| 7 | window_size | INT | NULL | NULL | P1-1自适应阈值滑动窗口大小(分钟)，NULL=未配置，默认15/10 |
| 8 | k_value | DECIMAL(3,2) | NULL | NULL | P1-1自适应阈值灵敏度系数k，NULL=未配置，默认2/3 |
<!-- R-03-issue-4: 已修复 - 新增window_size/k_value两个可空字段，P1-1自适应阈值参数持久化，P0阶段为NULL -->
| 9 | version | INT | NOT NULL | 0 | 乐观锁 |
| 10 | create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| 11 | update_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 修改时间 |

**索引**: PRIMARY KEY(id), UNIQUE(area_name)

**业务规则**: 固定 4 类不可增删改名, 乐观锁; 删除约束: 有 threshold_rule 关联时拒绝; 停用后不参与阈值判断。

---

### 3.6 report（报告表 · P2）

| # | 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|--------|------|--------|--------|------|
| 1 | id | BIGINT | NOT NULL | AUTO_INCREMENT (**PK**) | 报告 ID |
| 2 | report_period | VARCHAR(5) | NOT NULL | — | 日/周/月 |
| 3 | period_start | DATETIME | NOT NULL | — | 统计起始 |
| 4 | period_end | DATETIME | NOT NULL | — | 统计截止 |
| 5 | content | TEXT | NOT NULL | — | Markdown/HTML |
| 6 | status | VARCHAR(10) | NOT NULL | — | 生成中/已生成/生成失败，INSERT必填 |
| 7 | create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 生成时间 |
| 8 | update_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 修改时间 |

**索引**: PRIMARY KEY(id), INDEX(report_period), INDEX(period_start)
<!-- R-03-issue-13: 已修复 - 新增period_start索引，支持报告列表按生成时间倒序查询 -->

**业务规则**: 同周期幂等检查防重复生成; 教学简化不实现邮件发送。
