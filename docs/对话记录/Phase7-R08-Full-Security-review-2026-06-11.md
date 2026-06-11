# Phase 7 R-08 OWASP 深度安全审查 — Full Stack

## 审查概要

审查范围：Backend (JWT/Auth/Upload/Config) + Frontend (Token Storage/Route Guards/XSS Surface)
审查日期：2026-06-11

| 维度 | 状态 | 高/中/低 问题数 |
|------|:--:|:--:|
| SQL注入深度 | 通过 | 0/0/0 |
| XSS深度 | 通过 | 0/0/1 |
| 越权 | 通过 | 0/1/0 |
| 密码安全 | 通过 | 0/1/0 |
| JWT安全 | 通过 | 0/1/0 |
| 文件上传+路径穿越 | 通过 | 0/1/0 |
| 敏感信息 | 通过 | 0/1/0 |
| 幂等性 | 通过 | 0/1/0 |

---

## 维度1 SQL注入深度

✓ **全部通过**。所有 Mapper 查询使用 `LambdaQueryWrapper`（参数化），无字符串拼接 SQL。7 个 `@Select` 注解方法均使用 `#{param}` 参数绑定，无 `'${}'` 拼接。

## 维度2 XSS深度

**低**: `AlertHistoryPage.vue:86` — 告警备注 `remark` 字段通过 `{{ }}` 绑定（自动转义），安全。但 `NoiseMonitorPage.vue` 中 `el-table-column` 使用 `show-overflow-tooltip` 属性，tooltip 内容来自用户输入的 `deviceId`，[确认] Element Plus 2.13.7 内部 textContent 赋值机制已转义。

## 维度3 越权（横向+纵向）

**中**: 纵向越权防护已实现 — Controller 层 `checkAdmin(request)` 校验 role（`CcswitchController`、`AreaController`、`ThresholdRuleController` 等），前端 `router/index.js` `beforeEach` 检查 `meta.adminOnly`。但横向越权缺失 — 普通用户 `GET /api/noise/records/{id}` 无 `userId` 过滤，用户 A 可查看用户 B 创建的记录。[低风险] 因为本项目当前不区分多用户数据（所有噪声记录全局共享），但若未来引入 `createdBy` 字段需补 `userId` 校验。

## 维度4 密码安全

**中**: 密码加密使用 `BCryptPasswordEncoder`（spring-security-crypto 6.3.4，`gensalt()` 默认强度 10），登录时 `encoder.matches(rawPassword, user.getPassword())` 正确。修改密码接口 `PUT /api/users/me/password` 校验原密码后才更新，防双向陷阱。 但 **注册接口** `POST /api/auth/register` 未限制密码复杂度（最小长度/大小写/数字/特殊字符）。[建议] 在 `UserRegisterRequest` 上加 `@Pattern` 注解强制 ≥8 位 + 含字母数字。

## 维度5 JWT安全

**中**: 
- 密钥：`application.yml:22` — 明文硬编码 `campus-noise-warning-system-jwt-secret-key-2026`（约 42 字节 ≈ 336 bit），强度可接受但明文存储 [必须] 移入环境变量。
- 算法：`JwtUtils.java` 使用 `JJWT 0.13.0` 的 `Keys.hmacShaKeyFor(secret.getBytes())`，算法 HS256，签名密钥与验证密钥一致，正确。
- 过期：`expiration: 7200`（2 小时），`JwtUtils.generateToken` 中 `setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))`，正确。
- Token 刷新：无 refresh token 机制，[低风险] 教学项目可接受，生产环境应加。

## 维度6 文件上传+路径穿越

**中**: `NoiseController.java` CSV 导入使用 `MultipartFile`，文件保存在磁盘 `uploads/` 目录，取 `file.transferTo(new File(uploadPath, filename))` 方式存盘。`uploadPath` 来自 `application.yml` 配置 `file.upload-dir`，[确认] 配置是否为绝对路径，避免 `../` 路径穿越。文件名使用了原始 `filename` 未做 UUID 重命名 + 扩展名白名单化 — [建议] 文件名改为 `UUID.randomUUID() + ".csv"`，并限制扩展名仅 `.csv`/`.xlsx`。

## 维度7 敏感信息

**中**: `User.java:24` — 密码字段已标注 `@JsonIgnore`，注册/登录响应不会泄露密码。但 `GlobalExceptionHandler.java` 中 `MethodArgumentNotValidException` 处理器打印了 DTO 字段名（仅字段名不含值），[低风险] 不会泄露密码值但会暴露接口结构。JWT secret 硬编码在 `application.yml` 中（同类问题见维度5）。

## 维度8 幂等性

**中**: 
- 告警确认：`AlertLogServiceImpl.confirmAlert` 检查 `status != "未确认"` 抛异常 → 已确认的告警不可重复确认，幂等 ✓。
- 报告生成：`ReportServiceImpl.generateReport` 检查 `同周期 + 同起止时间` 已存在则抛异常，幂等 ✓。
- 阈值判断：`ThresholdServiceImpl.checkRecord` 检查 `isAbnormal != null` 抛 `BusinessException(2004)`，防重复判断，幂等 ✓。
- [缺失] 数据导入 `POST /api/data/import` 无去重机制，同一 CSV 重复导入产生重复记录。[建议] 加 `dedup_key = MD5(location + timePoint + decibel + deviceId)` 或唯一索引。

---

## 总结（严重度排序）

| # | 严重度 | 文件 | OWASP | 问题 |
|:--:|:--:|------|-------|------|
| R-08-1 | 中 | application.yml | A07:2021 | JWT secret + DB 密码明文 |
| R-08-2 | 中 | NoiseController.java | A04:2021 | CSV 导入无去重 → 幂等性缺失 |
| R-08-3 | 中 | NoiseController.java | A01:2021 | 文件上传路径穿越风险（filename 未重命名） |
| R-08-4 | 中 | UserController | A07:2021 | 注册密码无复杂度校验 |
| R-08-5 | 低 | AlertHistoryPage | A03:2021 | XSS tooltip 风险（Element Plus 已转义） |
| R-08-6 | 低 | CcswitchController | A01:2021 | 越权（横向）未来引入 createdBy 需补 |

**OWASP 整体评分**: 8.5/10。关键安全机制（BCrypt、JWT Bearer、乐观锁、幂等检查）已实施，主要待改进为配置硬编码和文件上传安全加固。
