# 校园噪音分贝预警员系统 - API 接口设计

## 1. 接口约定（跨接口共用规则）

| 项 | 决定 | 引用 |
|---|---|---|
| URL 前缀 | `/api`（所有接口路径以 `/api/` 开头） | RESTful 标准 + 跟前端 axios baseURL `/api` 对齐（CLAUDE.md §三·三） |
| 响应格式 | 统一 `Result<T>`（`{Integer code, String message, T data}`）+ 静态工厂 | **CLAUDE.md §一·三** + init-skeleton 生成的 `common/Result.java` |
| 鉴权 Header | `Authorization: Bearer <JWT token>`（登录后接口必含） | CLAUDE.md §一·二 + init-skeleton 生成的 `LoginInterceptor` |
| 分页参数 | query 参数 `pageNum`（从 1 开始）+ `pageSize`（默认 20，最大 100） | MyBatis-Plus `PaginationInnerInterceptor` |
| RESTful 命名 | 资源用复数名词（`/api/users`）· HTTP 动词（GET 列表/详情 · POST 创建 · PUT 更新 · DELETE 删除） | RESTful 标准 |
| 路径参数 | `/api/users/{id}`（**禁止**用 `/api/users?id=`） | RESTful 标准 |
| 请求体格式 | `application/json`（POST/PUT 用 body · GET/DELETE 不用 body · 查询用 query） | HTTP 标准 |
| 时间字段格式 | ISO 8601（如 `2026-05-10T08:30:00`）序列化 | Jackson 默认 + LocalDateTime |

## 2. 接口清单（按业务模块分组）

### 2.1 用户/认证模块（/api/auth · /api/users）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 注册 | POST /api/auth/register | ❌ | 公开 | P0 |
| 2 | 登录 | POST /api/auth/login | ❌ | 公开 | P0 |
| 3 | 查看个人信息 | GET /api/users/me | ✅ | 普通用户/管理员 | P0 |
| 4 | 修改密码 | PUT /api/users/me/password | ✅ | 普通用户/管理员 | P0 |

### 2.2 噪声数据模块（/api/noise）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 传感器上报/手动录入 | POST /api/noise/records | ❌(传感器)/✅(手动) | 传感器无需认证，管理员手动录入 | P0 |
| 2 | 批量导入 | POST /api/noise/records/batch | ✅ | 管理员 | P0 |
| 3 | 各功能区最新数据 | GET /api/noise/records/latest | ✅ | 普通用户/管理员 | P0 |
| 4 | 噪声记录分页列表 | GET /api/noise/records | ✅ | 普通用户/管理员 | P0 |
| 5 | 噪声记录详情 | GET /api/noise/records/{id} | ✅ | 普通用户/管理员 | P0 |
| 6 | 高级筛选 | GET /api/noise/records/search | ✅ | 普通用户/管理员 | P1 |
| 7 | 导出 CSV | GET /api/noise/records/export | ✅ | 管理员 | P1 |
| 8 | 导入文件解析 | POST /api/data/import | ✅ | 管理员 | P2 |
| 9 | 导出 Excel 报表 | GET /api/data/export-report | ✅ | 管理员 | P2 |

### 2.3 阈值判断模块（/api/thresholds）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 查询当前阈值 | GET /api/thresholds/current | ✅ | 普通用户/管理员 | P0 |
| 2 | 手动触发判断 | POST /api/thresholds/check/{noiseRecordId} | ✅ | 管理员 | P0 |
| 3 | 统计自适应阈值查询 | GET /api/thresholds/adaptive/current | ✅ | 普通用户/管理员 | P1 |
| 4 | 配置自适应参数 | PUT /api/thresholds/adaptive/config | ✅ | 管理员 | P1 |
| 5 | 查询混合模型状态 | GET /api/thresholds/hybrid/status | ✅ | 普通用户/管理员 | P1 |
| 6 | 查询模型性能 | GET /api/thresholds/hybrid/performance | ✅ | 普通用户/管理员 | P1 |

### 2.4 阈值规则配置模块（/api/thresholds/rules）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 阈值规则列表 | GET /api/thresholds/rules | ✅ | 普通用户/管理员 | P0 |
| 2 | 新增阈值规则 | POST /api/thresholds/rules | ✅ | 管理员 | P1 |
| 3 | 修改阈值规则 | PUT /api/thresholds/rules/{id} | ✅ | 管理员 | P1 |
| 4 | 删除阈值规则 | DELETE /api/thresholds/rules/{id} | ✅ | 管理员 | P1 |
| 5 | 重载阈值规则 | POST /api/thresholds/rules/reload | ✅ | 管理员 | P1 |

### 2.5 仪表盘模块（/api/dashboard）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 功能区实时概览 | GET /api/dashboard/overview | ✅ | 普通用户/管理员 | P0 |
| 2 | 功能区详情 | GET /api/dashboard/areas/{location} | ✅ | 普通用户/管理员 | P0 |

### 2.6 告警模块（/api/alerts）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 告警列表分页 | GET /api/alerts | ✅ | 普通用户/管理员 | P0 |
| 2 | 告警详情 | GET /api/alerts/{id} | ✅ | 普通用户/管理员 | P0 |
| 3 | 确认告警 | PUT /api/alerts/{id}/confirm | ✅ | 管理员 | P0 |
| 4 | 处置告警 | PUT /api/alerts/{id}/resolve | ✅ | 管理员 | P0 |

### 2.7 功能区配置模块（/api/areas）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 功能区列表 | GET /api/areas | ✅ | 管理员 | P0 |
| 2 | 修改功能区配置 | PUT /api/areas/{id} | ✅ | 管理员 | P0 |

### 2.8 统计/可视化模块（/api/statistics）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 时间序列数据 | GET /api/statistics/timeseries | ✅ | 普通用户/管理员 | P1 |
| 2 | 功能区统计汇总 | GET /api/statistics/areas | ✅ | 普通用户/管理员 | P1 |
| 3 | 模型性能对比 | GET /api/statistics/models | ✅ | 普通用户/管理员 | P1 |
| 4 | 多维度分析 | GET /api/statistics/multi-dim | ✅ | 普通用户/管理员 | P2 |
| 5 | 热力图数据 | GET /api/statistics/heatmap | ✅ | 普通用户/管理员 | P2 |
| 6 | 雷达图数据 | GET /api/statistics/radar | ✅ | 普通用户/管理员 | P2 |

### 2.9 报告模块（/api/reports · P2）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 报告列表分页 | GET /api/reports | ✅ | 普通用户/管理员 | P2 |
| 2 | 报告详情 | GET /api/reports/{id} | ✅ | 普通用户/管理员 | P2 |
| 3 | 手动生成报告 | POST /api/reports | ✅ | 管理员 | P2 |
| 4 | 配置报告计划 | PUT /api/reports/config | ✅ | 管理员 | P2 |

### 2.10 AI 分类模块（/api/ai · P2）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 手动触发分类 | POST /api/ai/classify | ✅ | 管理员 | P2 |
| 2 | 配置 AI 参数 | PUT /api/ai/config | ✅ | 管理员 | P2 |

### 2.11 ccswitch 模块（/api/ccswitch · P2）

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | 实现优先级 |
|---|---|:--:|---|:---:|
| 1 | 查询配置状态 | GET /api/ccswitch/status | ✅ | 管理员 | P2 |
| 2 | 触发配置重载 | POST /api/ccswitch/reload | ✅ | 管理员 | P2 |

## 3. 接口详情

### 3.1 用户/认证模块

#### POST /api/auth/register

- **功能**：用户注册（用户名+密码+角色）
- **是否需登录**：❌ 公开
- **请求参数**（body · application/json）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | ✅ | 用户名，2-20 字符，唯一 |
| password | String | ✅ | 密码，6-32 字符 |
| role | String | ✅ | 角色：`普通用户` / `管理员` |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | username/password/role 缺失或格式错误 |
| 1001 | 用户名已存在 | username 违反唯一约束 |
| 1002 | 角色取值错误 | role 不在 {普通用户, 管理员} 内 |
| 500 | 服务器内部错误 | 数据库异常 |

---

#### POST /api/auth/login

- **功能**：用户登录，返回 JWT token + 角色
- **是否需登录**：❌ 公开
- **请求参数**（body · application/json）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | ✅ | 用户名 |
| password | String | ✅ | 密码（明文，后端 BCrypt 校验） |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "role": "管理员"
  }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | username/password 缺失 |
| 1003 | 用户名或密码错误 | 用户不存在或 BCrypt 校验失败 |
| 1004 | 账号已被禁用 | user.status = 0 |
| 500 | 服务器内部错误 | 数据库异常 |

---

#### GET /api/users/me

- **功能**：查看当前登录用户个人信息
- **是否需登录**：✅（LoginInterceptor）
- **角色限制**：普通用户/管理员（只能查看自己的信息）
- **请求参数**：无（从 JWT 解析 userId）

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "role": "管理员",
    "status": 1,
    "createTime": "2026-06-11T14:00:00"
  }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录或 token 过期 | JWT 校验失败 |
| 2001 | 用户不存在 | JWT 中的 userId 对应的用户已被删除 |

---

#### PUT /api/users/me/password

- **功能**：修改当前用户密码（需输入原密码）
- **是否需登录**：✅
- **角色限制**：普通用户/管理员（只能改自己的密码）
- **行级权限**：从 JWT 解析 userId，仅操作自己的 user 记录
- **请求参数**（body · application/json）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| oldPassword | String | ✅ | 原密码 |
| newPassword | String | ✅ | 新密码，6-32 字符 |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | oldPassword/newPassword 缺失或格式错误 |
| 401 | 未登录或 token 过期 | JWT 校验失败 |
| 1005 | 原密码错误 | BCrypt 校验原密码不匹配 |

---

### 3.2 噪声数据模块

#### POST /api/noise/records

- **功能**：接收传感器上报噪声数据，或管理员手动录入
- **是否需登录**：传感器上报无需认证（内部接口），管理员手动录入需登录
- **角色限制**：传感器调用无限制，手动录入仅管理员
- **请求参数**（body · application/json）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| location | String | ✅ | 功能区：`图书馆`/`食堂`/`操场`/`宿舍` |
| decibel | Double | ✅ | 分贝值，20.0-120.0 dB(A) |
| timePoint | String | ❌ | 噪声时间点（ISO 8601），为空则取服务器当前时间 |
| deviceId | String | ❌ | 设备标识，管理员手动录入时自动填充 `MANUAL_`+时间戳 |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "数据记录成功",
  "data": { "id": 42 }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | location/decibel 缺失或超出范围 |
| 401 | 未登录 | 手动录入时 token 无效 |
| 403 | 越权访问 | 非管理员尝试手动录入 |
| 2002 | 功能区不存在 | location 不在四大功能区之列 |

---

#### POST /api/noise/records/batch

- **功能**：批量导入噪声数据
- **是否需登录**：✅
- **角色限制**：仅管理员
- **请求参数**（body · application/json）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| records | List\<NoiseRecordBatchDTO\> | ✅ | 批量噪声记录数组，单次 ≤ 5000 条 |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "批量导入完成",
  "data": { "successCount": 4800, "failCount": 0 }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | 数组为空或格式错误 |
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员调用 |
| 5000 | 批量数据超限 | records 超过 5000 条 |

---

#### GET /api/noise/records/latest

- **功能**：获取各功能区最新一条噪声数据（仪表盘用）
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用
- **请求参数**：无
- **分页约束**：不适用（固定返回 4 条，每功能区 1 条）
- **空集合**：某功能区无数据时该元素 `data: null`，其余功能区正常返回

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 142,
      "location": "图书馆",
      "decibel": 48.3,
      "timePoint": "2026-06-11T14:30:00",
      "thresholdValue": 40,
      "isAbnormal": 0
    },
    {
      "id": 143,
      "location": "食堂",
      "decibel": 63.8,
      "timePoint": "2026-06-11T14:30:00",
      "thresholdValue": 65,
      "isAbnormal": 0
    },
    {
      "id": 144,
      "location": "操场",
      "decibel": 68.9,
      "timePoint": "2026-06-11T14:30:00",
      "thresholdValue": 70,
      "isAbnormal": 0
    },
    {
      "id": 145,
      "location": "宿舍",
      "decibel": 51.2,
      "timePoint": "2026-06-11T14:30:00",
      "thresholdValue": 45,
      "isAbnormal": 0
    }
  ]
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录或 token 过期 | JWT 校验失败 |

---

#### GET /api/noise/records

- **功能**：噪声记录分页列表查询，支持按时间/功能区/分贝范围/异常状态筛选
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用
- **分页约束**：pageNum ≥ 1 / pageSize 1-100（默认 20），超限返回 400
- **空集合**：无数据时 `data.records: []`，不返回 `null`
- **排序参数**：sortBy 白名单 `time_point`/`decibel`/`create_time`，默认 `time_point DESC`

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| pageNum | Integer | ❌ | 1 | 页码（从 1 开始） |
| pageSize | Integer | ❌ | 20 | 每页条数（1-100） |
| location | String | ❌ | — | 功能区筛选 |
| dateFrom | String | ❌ | — | 起始时间（ISO 8601） |
| dateTo | String | ❌ | — | 截止时间（ISO 8601） |
| minDb | Double | ❌ | — | 分贝下限 |
| maxDb | Double | ❌ | — | 分贝上限 |
| isAbnormal | Integer | ❌ | — | 异常状态：null=待判断/0=正常/1=异常 |
| sortBy | String | ❌ | time_point | 排序字段（白名单） |
| sortOrder | String | ❌ | desc | asc/desc |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 142,
        "location": "图书馆",
        "decibel": 48.3,
        "timePoint": "2026-06-11T14:30:00",
        "deviceId": "SIMULATOR",
        "isAbnormal": 0,
        "judgedByModel": "RULE_BASED",
        "noiseType": null
      }
    ],
    "total": 142,
    "pageNum": 1,
    "pageSize": 20,
    "pages": 8
  }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | pageSize > 100 / dateFrom > dateTo / minDb > maxDb |
| 401 | 未登录或 token 过期 | JWT 校验失败 |

---

#### GET /api/noise/records/{id}

- **功能**：查看某条噪声记录详情（含阈值判断结果）
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用
- **请求参数**：路径参数 `id`

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 142,
    "location": "图书馆",
    "decibel": 48.3,
    "timePoint": "2026-06-11T14:30:00",
    "deviceId": "SIMULATOR",
    "isAbnormal": 0,
    "judgedByModel": "RULE_BASED",
    "noiseType": null,
    "noiseDuration": null,
    "thresholdValue": 40,
    "alertInfo": null
  }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录或 token 过期 | JWT 校验失败 |
| 2003 | 噪声记录不存在 | id 不存在 |

---

#### GET /api/noise/records/search

- **功能**：高级筛选（关键词搜索 + 噪声类型，P1 扩展）
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用
- **分页约束**：同 GET /api/noise/records
- **空集合**：`data.records: []`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | String | ❌ | 设备 ID 模糊搜索 |
| noiseType | String | ❌ | 噪声类型：`交谈`/`施工`/`体育活动`/`交通`/`其它` |
| （其余参数同 GET /api/noise/records） | | | |

- **成功响应**：同 GET /api/noise/records
- **异常响应**：同 GET /api/noise/records

---

#### GET /api/noise/records/export

- **功能**：导出筛选结果为 CSV 文件
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：仅管理员
- **请求参数**：同 GET /api/noise/records 筛选参数（不含 pageNum/pageSize）
- **导出上限**：10000 条，超限返回 5001
- **文件名**：`noise_export_YYYYMMDD_HHmmss.csv`（UTF-8 BOM）

- **成功响应**（code=200）：浏览器触发 CSV 下载
- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |
| 5001 | 导出数据量超限 | 筛选结果 > 10000 条 |

---

### 3.3 阈值判断模块

#### GET /api/thresholds/current

- **功能**：查询某功能区当前时段的业务规则阈值
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| location | String | ✅ | 功能区：`图书馆`/`食堂`/`操场`/`宿舍` |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "location": "图书馆",
    "timeSegment": "上课",
    "thresholdValue": 40,
    "defaultThreshold": 40,
    "source": "RULE_BASED"
  }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | location 为空 |
| 401 | 未登录 | token 无效 |
| 2002 | 功能区不存在 | location 不在四大功能区之列 |

---

#### POST /api/thresholds/check/{noiseRecordId}

- **功能**：手动触发某条噪声记录的阈值判断（管理员"重新判断"按钮）
- **是否需登录**：✅
- **角色限制**：仅管理员
- **幂等性**：is_abnormal 已非 NULL 则跳过，返回"已判断"提示

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "判断完成",
  "data": {
    "noiseRecordId": 142,
    "decibel": 48.3,
    "thresholdValue": 40,
    "isAbnormal": 0,
    "judgedByModel": "RULE_BASED",
    "alertCreated": false
  }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |
| 2003 | 噪声记录不存在 | noiseRecordId 不存在 |
| 2004 | 该记录已判断 | is_abnormal 非 NULL（幂等跳过） |

---

#### GET /api/thresholds/adaptive/current

- **功能**：查询某功能区当前统计自适应阈值（P1-1）
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| location | String | ✅ | 功能区 |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "location": "图书馆",
    "windowSize": 15,
    "kValue": 2.00,
    "mean": 48.3,
    "stdDev": 6.7,
    "upperLimit": 61.7,
    "lowerLimit": 34.9,
    "windowRecordCount": 6
  }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | location 为空 |
| 2009 | 窗口内数据不足 | 窗口内正常记录数 < windowSize（回退业务规则） |

---

#### PUT /api/thresholds/adaptive/config

- **功能**：配置各功能区自适应阈值参数（窗口大小 + k 值）
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：仅管理员
- **幂等性**：数据库主键覆盖更新（area_config.id）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| areaConfigs | List\<AreaAdaptiveConfigDTO\> | ✅ | 各功能区参数数组 |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "配置保存成功",
  "data": null
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | windowSize < 5 或 > 100 / kValue < 1 或 > 5 |
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |

---

#### GET /api/thresholds/hybrid/status

- **功能**：查询当前混合阈值模型运行状态
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "currentMode": "STAT_ADAPTIVE",
    "abnormalRate3Windows": 0.05,
    "isTriggered": false,
    "triggerReason": null
  }
}
```

---

#### GET /api/thresholds/hybrid/performance

- **功能**：查询混合阈值模型性能统计
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accuracy": 92.6,
    "precision": 90.8,
    "recall": 91.7,
    "f1Score": 91.2,
    "falsePositiveRate": 4.0,
    "modeDistribution": { "RULE_BASED": 30, "ADAPTIVE": 67, "HYBRID": 3 }
  }
}
```

---

### 3.4 阈值规则配置模块

#### GET /api/thresholds/rules

- **功能**：阈值规则列表（按功能区筛选），P0 只读展示
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用
- **空集合**：无规则时 `data: []`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| location | String | ❌ | 功能区筛选，为空则返回全部 |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "location": "图书馆",
      "timeSegment": "上课",
      "thresholdValue": 40,
      "description": "严格安静要求，参考GB50118-2010高要求标准",
      "status": 1
    }
  ]
}
```

---

#### POST /api/thresholds/rules

- **功能**：新增阈值规则
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：仅管理员
- **幂等性**：数据库唯一索引 `(location, time_segment)`，重复插入返回 3001

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| location | String | ✅ | 功能区 |
| timeSegment | String | ✅ | 时段标签 |
| thresholdValue | Integer | ✅ | 阈值 0-120 dB(A) |
| description | String | ❌ | 业务逻辑说明 |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "规则创建成功",
  "data": { "id": 13 }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | 字段缺失或阈值超出范围 |
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |
| 3001 | 该功能区此时段已有规则 | 违反唯一约束（请修改而非新增） |

---

#### PUT /api/thresholds/rules/{id}

- **功能**：修改阈值规则
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：仅管理员
- **行级权限**：无（管理员可修改任意规则）
- **幂等性**：乐观锁 version 字段，并发修改时后提交者返回 3002

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| thresholdValue | Integer | ❌ | 阈值 0-120 dB(A) |
| description | String | ❌ | 业务逻辑说明 |
| status | Integer | ❌ | 1=启用，0=禁用 |
| version | Integer | ✅ | 当前版本号（乐观锁） |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "规则更新成功",
  "data": null
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | version 缺失或阈值超出范围 |
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |
| 2003 | 规则不存在 | id 不存在 |
| 3002 | 数据已被修改 | 乐观锁版本号不匹配 |

---

#### DELETE /api/thresholds/rules/{id}

- **功能**：删除阈值规则（删除后使用默认阈值兜底）
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：仅管理员

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "规则已删除，该功能区此时段将使用默认阈值兜底",
  "data": null
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |
| 2003 | 规则不存在 | id 不存在 |

---

#### POST /api/thresholds/rules/reload

- **功能**：通知 ccswitch 配置服务热更新阈值规则
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：仅管理员
- **集成方式**：主系统 → HTTP POST Flask ccswitch (Port 5000) `/api/config/reload`

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "规则已重载",
  "data": { "reloadTime": "2026-06-11T15:00:00", "ruleCount": 12 }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |
| 7001 | ccswitch 服务不可用 | Flask 服务未启动或连接超时 |
| 7002 | 配置重载失败 | ccswitch 读取 settings.json 异常 |

---

### 3.5 仪表盘模块

#### GET /api/dashboard/overview

- **功能**：四大功能区实时状态概览（最新分贝 + 异常状态 + 当前阈值）
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用
- **空集合**：某功能区无数据时 `data: null`

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "location": "图书馆",
      "decibel": 48.3,
      "thresholdValue": 40,
      "isAbnormal": 0,
      "indicator": "normal",
      "lastUpdateTime": "2026-06-11T14:30:00"
    },
    {
      "location": "食堂",
      "decibel": 63.8,
      "thresholdValue": 65,
      "isAbnormal": 0,
      "indicator": "normal",
      "lastUpdateTime": "2026-06-11T14:30:00"
    },
    {
      "location": "操场",
      "decibel": 68.9,
      "thresholdValue": 70,
      "isAbnormal": 0,
      "indicator": "normal",
      "lastUpdateTime": "2026-06-11T14:30:00"
    },
    {
      "location": "宿舍",
      "decibel": 51.2,
      "thresholdValue": 45,
      "isAbnormal": 1,
      "indicator": "abnormal",
      "lastUpdateTime": "2026-06-11T14:30:00"
    }
  ]
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录或 token 过期 | JWT 校验失败 |

---

#### GET /api/dashboard/areas/{location}

- **功能**：某功能区详细信息（最近 N 条数据 + 统计摘要）
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| location | String | ✅ | 功能区（路径参数） |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "location": "图书馆",
    "latestDecibel": 48.3,
    "thresholdValue": 40,
    "isAbnormal": 0,
    "indicator": "normal",
    "recentRecords": [
      { "timePoint": "2026-06-11T14:30:00", "decibel": 48.3 },
      { "timePoint": "2026-06-11T14:25:00", "decibel": 42.1 }
    ],
    "todayStats": { "avg": 45.2, "max": 62.5, "min": 35.2, "abnormalCount": 2 }
  }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录 | token 无效 |
| 2002 | 功能区不存在 | location 不在四大功能区之列 |

---

### 3.6 告警模块

#### GET /api/alerts

- **功能**：告警列表分页查询（时间倒序，支持按功能区/日期筛选）
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可查看
- **分页约束**：pageNum ≥ 1 / pageSize 1-100（默认 20）
- **空集合**：无数据时 `data.records: []`
- **排序**：默认 `create_time DESC`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | ❌ | 页码 |
| pageSize | Integer | ❌ | 每页条数 |
| location | String | ❌ | 功能区筛选 |
| dateFrom | String | ❌ | 起始日期 |
| dateTo | String | ❌ | 截止日期 |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "noiseRecordId": 142,
        "location": "宿舍",
        "decibel": 51.2,
        "thresholdValue": 45,
        "alertType": "超阈值",
        "confirmStatus": "未确认",
        "confirmedBy": null,
        "remark": null,
        "createTime": "2026-06-11T14:30:00"
      }
    ],
    "total": 25,
    "pageNum": 1,
    "pageSize": 20,
    "pages": 2
  }
}
```

---

#### GET /api/alerts/{id}

- **功能**：告警详情（含关联噪声记录信息）
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "noiseRecordId": 142,
    "location": "宿舍",
    "decibel": 51.2,
    "thresholdValue": 45,
    "alertType": "超阈值",
    "confirmStatus": "未确认",
    "confirmedBy": null,
    "remark": null,
    "createTime": "2026-06-11T14:30:00",
    "noiseRecord": {
      "id": 142,
      "deviceId": "SIMULATOR",
      "timePoint": "2026-06-11T14:30:00",
      "judgedByModel": "RULE_BASED"
    }
  }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录 | token 无效 |
| 4001 | 告警不存在 | id 不存在 |

---

#### PUT /api/alerts/{id}/confirm

- **功能**：确认告警（管理员）
- **是否需登录**：✅
- **角色限制**：仅管理员
- **幂等性**：乐观锁 version + 条件 UPDATE `WHERE confirm_status='未确认'`，已确认则返回 4002

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| version | Integer | ✅ | 当前版本号（乐观锁） |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "告警已确认",
  "data": null
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |
| 4001 | 告警不存在 | id 不存在 |
| 4002 | 该告警已被处理 | confirm_status ≠ '未确认'（并发冲突） |
| 4003 | 数据已被修改 | 乐观锁版本号不匹配 |

---

#### PUT /api/alerts/{id}/resolve

- **功能**：处置告警（管理员），确认状态从"已确认"→"已处置"
- **是否需登录**：✅
- **角色限制**：仅管理员
- **幂等性**：条件 UPDATE `WHERE confirm_status='已确认'`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| version | Integer | ✅ | 当前版本号（乐观锁） |
| remark | String | ❌ | 处理备注（≤500 字符） |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "告警已处置",
  "data": null
}
```

- **异常响应**：同 PUT /api/alerts/{id}/confirm

---

### 3.7 功能区配置模块

#### GET /api/areas

- **功能**：四大功能区配置列表
- **是否需登录**：✅
- **角色限制**：仅管理员

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "areaName": "图书馆",
      "noiseSensitivity": 1,
      "defaultThreshold": 40,
      "description": "需严格安静，噪声敏感度极高",
      "status": 1,
      "windowSize": null,
      "kValue": null
    },
    { "id": 2, "areaName": "食堂", "noiseSensitivity": 2, "defaultThreshold": 65, "status": 1 },
    { "id": 3, "areaName": "操场", "noiseSensitivity": 3, "defaultThreshold": 70, "status": 1 },
    { "id": 4, "areaName": "宿舍", "noiseSensitivity": 1, "defaultThreshold": 45, "status": 1 }
  ]
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |

---

#### PUT /api/areas/{id}

- **功能**：修改功能区配置（默认阈值/敏感度/描述/状态）
- **是否需登录**：✅
- **角色限制**：仅管理员
- **行级权限**：无（管理员可修改任意功能区）
- **幂等性**：乐观锁 version 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| noiseSensitivity | Integer | ❌ | 1=高/2=中/3=低 |
| defaultThreshold | Integer | ❌ | 0-120 dB(A) |
| description | String | ❌ | 描述 |
| status | Integer | ❌ | 1=启用/0=停用 |
| version | Integer | ✅ | 当前版本号（乐观锁） |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "功能区配置更新成功",
  "data": null
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 400 | 参数校验失败 | defaultThreshold 超出 0-120 |
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |
| 6001 | 功能区不存在 | id 不存在 |
| 6002 | 数据已被修改 | 乐观锁版本号不匹配 |

---

### 3.8 统计/可视化模块（P1/P2）

#### GET /api/statistics/timeseries

- **功能**：时间序列数据（分贝曲线 + 阈值上下限 + 异常点）
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用
- **空集合**：无数据时 `data.points: []`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| location | String | ✅ | 功能区 |
| dateFrom | String | ❌ | 起始时间 |
| dateTo | String | ❌ | 截止时间 |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "location": "图书馆",
    "points": [
      {
        "timePoint": "2026-06-11T08:00:00",
        "decibel": 42.3,
        "upperThreshold": 61.7,
        "lowerThreshold": 34.9,
        "thresholdSource": "ADAPTIVE",
        "isAbnormal": false
      },
      {
        "timePoint": "2026-06-11T08:10:00",
        "decibel": 55.2,
        "upperThreshold": 40.0,
        "lowerThreshold": 40.0,
        "thresholdSource": "RULE_BASED",
        "isAbnormal": true,
        "alertId": 5
      }
    ]
  }
}
```

---

#### GET /api/statistics/areas

- **功能**：各功能区统计汇总（平均分贝/异常率/告警次数）
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| dateFrom | String | ❌ | 起始时间 |
| dateTo | String | ❌ | 截止时间 |

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "areas": [
      { "location": "图书馆", "avgDecibel": 48.3, "abnormalRate": 18.4, "alertCount": 7 },
      { "location": "食堂", "avgDecibel": 63.8, "abnormalRate": 27.8, "alertCount": 10 },
      { "location": "操场", "avgDecibel": 68.9, "abnormalRate": 32.4, "alertCount": 11 },
      { "location": "宿舍", "avgDecibel": 51.2, "abnormalRate": 20.6, "alertCount": 7 }
    ],
    "summary": { "totalRecords": 142, "avgDecibel": 57.6, "totalAlerts": 35 }
  }
}
```

---

#### GET /api/statistics/models

- **功能**：模型性能对比数据（固定阈值 vs 业务规则 vs 统计自适应 vs 混合）
- **实现优先级**：P1
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "models": [
      { "modelName": "固定阈值", "accuracy": 78.0, "precision": 65.0, "recall": 70.0, "f1Score": 67.4, "fpr": 13.5 },
      { "modelName": "业务规则", "accuracy": 88.7, "precision": 85.2, "recall": 87.3, "f1Score": 86.2, "fpr": 5.3 },
      { "modelName": "统计自适应", "accuracy": 89.4, "precision": 86.1, "recall": 88.2, "f1Score": 87.1, "fpr": 8.2 },
      { "modelName": "混合阈值", "accuracy": 92.6, "precision": 90.8, "recall": 91.7, "f1Score": 91.2, "fpr": 4.0 }
    ]
  }
}
```

---

#### 其余统计接口（P2-4 多维分析 / 热力图 / 雷达图 · 统一结构）

##### GET /api/statistics/multi-dim

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| xDim | String | ✅ | X 轴维度：`time_segment`/`location`/`noise_type` |
| yDim | String | ✅ | Y 轴指标：`avg_decibel`/`abnormal_rate`/`alert_count` |
| dateFrom | String | ❌ | 起始时间 |
| dateTo | String | ❌ | 截止时间 |

##### GET /api/statistics/heatmap

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| dateFrom | String | ❌ | 起始时间 |
| dateTo | String | ❌ | 截止时间 |

##### GET /api/statistics/radar

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| dateFrom | String | ❌ | 起始时间 |
| dateTo | String | ❌ | 截止时间 |

---

### 3.9 报告模块（P2）

#### GET /api/reports

- **功能**：报告列表分页
- **实现优先级**：P2
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用
- **分页约束**：pageNum ≥ 1 / pageSize 1-100
- **空集合**：`data.records: []`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | ❌ | 页码 |
| pageSize | Integer | ❌ | 每页条数 |

- **成功响应**（code=200）：含报告列表 + 分页信息

---

#### GET /api/reports/{id}

- **功能**：报告详情
- **实现优先级**：P2
- **是否需登录**：✅
- **角色限制**：普通用户/管理员均可调用

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 8001 | 报告不存在 | id 不存在 |

---

#### POST /api/reports

- **功能**：手动触发报告生成
- **实现优先级**：P2
- **是否需登录**：✅
- **角色限制**：仅管理员
- **幂等性**：同周期已有报告则跳过（检查 report_period + period_start 去重）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reportPeriod | String | ✅ | `日`/`周`/`月` |
| periodStart | String | ✅ | 统计起始时间（ISO 8601） |
| periodEnd | String | ✅ | 统计截止时间（ISO 8601） |

---

#### PUT /api/reports/config

- **功能**：配置报告自动生成计划
- **实现优先级**：P2
- **是否需登录**：✅
- **角色限制**：仅管理员

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reportPeriod | String | ✅ | 周期 |
| generateTime | String | ✅ | 生成时间（HH:mm） |

---

### 3.10 AI 分类模块（P2）

#### POST /api/ai/classify

- **功能**：手动触发 AI 噪声分类（扫描未分类记录）
- **实现优先级**：P2
- **是否需登录**：✅
- **角色限制**：仅管理员
- **幂等性**：CAS `WHERE noise_type IS NULL`，已分类记录跳过

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "分类完成",
  "data": { "classifiedCount": 35, "skippedCount": 107 }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 9001 | AI 分类服务不可用 | ccswitch 或 AI API 不可达 |

---

#### PUT /api/ai/config

- **功能**：配置 AI 分类参数
- **实现优先级**：P2
- **是否需登录**：✅
- **角色限制**：仅管理员

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| enabled | Boolean | ❌ | 是否启用自动分类 |
| minConfidence | Double | ❌ | 最低置信度 0-1，默认 0.7 |

---

### 3.11 ccswitch 模块（P2）

#### GET /api/ccswitch/status

- **功能**：查询 ccswitch 配置服务当前状态
- **实现优先级**：P2
- **是否需登录**：✅
- **角色限制**：仅管理员

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "connected": true,
    "model": "claude-opus-4-8",
    "baseUrl": "http://127.0.0.1:15721",
    "configSource": "ccswitch",
    "uptime": "2h 35m",
    "environmentVars": { "ANTHROPIC_MODEL": "claude-opus-4-8", "ANTHROPIC_BASE_URL": "..." }
  }
}
```

---

#### POST /api/ccswitch/reload

- **功能**：触发 ccswitch 配置重载
- **实现优先级**：P2
- **是否需登录**：✅
- **角色限制**：仅管理员
- **幂等性**：前端按钮禁用防抖（请求中不可重复点击）

- **成功响应**（code=200）：

```json
{
  "code": 200,
  "message": "配置重载成功",
  "data": { "model": "claude-opus-4-8", "reloadTime": "2026-06-11T15:00:00" }
}
```

- **异常响应**：

| code | message | 触发场景 |
|---|---|---|
| 401 | 未登录 | token 无效 |
| 403 | 越权访问 | 非管理员 |
| 7001 | ccswitch 服务不可用 | Flask 服务未运行或连接超时 |
| 7002 | 配置重载失败 | ccswitch 读取 settings.json 异常 |

---

## 4. 通用响应格式 + 异常码表

### 4.1 Result\<T\> 响应格式（详见 CLAUDE.md §一·三）

成功响应：
```json
{ "code": 200, "message": "操作成功", "data": <T> }
```

失败响应：
```json
{ "code": <错误码>, "message": "<错误说明>", "data": null }
```

### 4.2 全局异常码（@RestControllerAdvice）

| code | message 模板 | 触发场景 | 触发位置 |
|---|---|---|---|
| 400 | 参数校验失败: \<字段名\> | @Valid 校验失败 | Controller 入参 @Valid |
| 401 | 未登录或 token 过期 | LoginInterceptor JWT 校验失败 | LoginInterceptor.preHandle |
| 403 | 越权访问 | JWT 角色不匹配 | Controller/Service 层 |
| 404 | 资源不存在 | 路径不匹配 | Spring Boot 默认 |
| 500 | 服务器内部错误 | Exception 兜底 | GlobalExceptionHandler |

### 4.3 业务异常码（Service → BusinessException）

| code 范围 | 模块 | 示例 |
|---|---|---|
| 1001-1099 | 用户/认证 | 1001=用户名已存在 · 1003=用户名或密码错误 · 1004=账号已禁用 · 1005=原密码错误 |
| 2001-2099 | 噪声数据 | 2001=用户不存在 · 2002=功能区不存在 · 2003=噪声记录不存在 · 2004=该记录已判断 · 2009=窗口数据不足 |
| 3001-3099 | 阈值规则 | 3001=该功能区此时段已有规则 · 3002=数据已被修改（乐观锁冲突） |
| 4001-4099 | 告警 | 4001=告警不存在 · 4002=该告警已被处理 · 4003=数据已被修改（乐观锁冲突） |
| 5001-5099 | 数据导入导出 | 5001=导出数据量超限 · 5002=文件格式不支持 · 5003=表头不匹配 · 5004=文件过大 |
| 6001-6099 | 功能区配置 | 6001=功能区不存在 · 6002=数据已被修改（乐观锁冲突） · 6003=功能区有依赖数据不可删除/停用 |
| 7001-7099 | ccswitch | 7001=ccswitch 服务不可用 · 7002=配置重载失败 |
| 8001-8099 | 报告 | 8001=报告不存在 · 8002=同周期已有报告（幂等跳过） |
| 9001-9099 | AI 分类 | 9001=AI 分类服务不可用 · 9002=分类参数配置错误 |
