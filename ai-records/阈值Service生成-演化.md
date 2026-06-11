# 阈值Service生成 提示词演化

## v1 — 初始版本

```
写一个 ThresholdService，根据功能区和当前时段判断噪声是否超过阈值，超过就标记异常并记录告警。
```

## 问题与不足

v1 产出只有单一的业务规则判断逻辑，没有按 CLAUDE.md 要求使用 `LambdaQueryWrapper`，直接用字符串拼接查询 `threshold_rule` 表。缺少三层降级策略（规则表 → 功能区默认阈值 → 全局默认 55dB）。没有构造函数注入依赖（用了 `@Autowired` 字段注入）。没有参数校验（location 为 null 时空指针）。混合模型（P1-2）和自适应阈值（P1-1）完全未涉及。

## v2 — 改进版本

```
基于 docs/DATABASE_DESIGN.md 中 threshold_rule + area_config 表 + docs/API_DESIGN.md 中阈值接口，生成 ThresholdService + ThresholdServiceImpl。

功能要求：
1. getCurrentThreshold(location): 查询 threshold_rule 表（LambdaQueryWrapper），按 location + 当前时段 + status=1 匹配
2. 三层降级：规则表 → area_config.default_threshold → 全局常量 55 dB(A)
3. judgeNoiseRecord(record): 调用 getCurrentThreshold + 比较分贝值 + 标记 isAbnormal + 写入 alert_log
4. 自适应阈值：滑动窗口计算 μ ± k×σ，k 默认 2.0，窗口默认 100 条
5. 混合模型：常规用自适应，异常率>10% 或分贝骤升≥15dB 时切换到业务规则

技术约束：构造函数注入（禁止 @Autowired 字段注入）、LambdaQueryWrapper 禁止字符串拼接、LocalDateTime 判断时段、无效 location 抛 BusinessException
```

## 改进效果

v2 覆盖了 P0-3 + P1-1 + P1-2 全部三种模型，但自适应阈值的滑动窗口实现每次全量查询 `noise_record` 表（性能问题），混合模型的切换判断逻辑散落在 `judgeNoiseRecord` 里导致方法超过 150 行。时段判断用了 `if-else` 链而非策略模式。

## v3 — 优化版本

```
基于 docs/DATABASE_DESIGN.md + docs/API_DESIGN.md + CLAUDE.md §二 后端规范，生成 ThresholdService 全套代码。

硬约束：
1. 分层：ThresholdService(接口) + ThresholdServiceImpl(实现) + ThresholdRuleMapper(已有)
2. 三层降级策略 + 中文注释标注每层来源
3. 时段判断：根据 LocalTime.now() 映射 5 时段标签（早自习/上午/下午/晚自习/夜间），抽出独立 private 方法 getCurrentTimeSegment()
4. 自适应阈值：滑动窗口查询须加 LIMIT（窗口大小可配置，默认 100），禁止全量查表后内存截断
5. 混合模型：切换判断逻辑抽出独立方法 shouldSwitchToRuleBased()，返回 boolean，单行 return 不超过 80 字符
6. 依赖注入：构造函数注入 + final 字段，全部依赖显式声明
7. 参数校验：location 非空且属于 4 大功能区之一，否则抛 BusinessException(3001)
8. 拆分：judgeNoiseRecord ≤ 60 行，超过则拆分子方法
9. CcswitchService：阈值规则刷新时同步通知 ccswitch 配置服务（P2 预留 MOCK 实现）

禁止：@Autowired 字段注入、全量查表无 LIMIT、硬编码魔法数字、方法超 80 行
```

## 最终效果

v3 实现后 `ThresholdServiceImpl.java` 方法平均 25 行，最大方法 58 行（低于 60 行上限）。滑动窗口查询带 `last("LIMIT " + windowSize)` 防止全量扫描。三层降级每层有中文注释标注来源。代码评审 R-05 中 Service 维度零 issue，性能测试中 10000 条噪声记录的阈值判断耗时从 v2 的 3200ms 降至 180ms。
