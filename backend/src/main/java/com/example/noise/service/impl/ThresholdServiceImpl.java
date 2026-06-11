package com.example.noise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.noise.common.BusinessException;
import com.example.noise.entity.AreaConfig;
import com.example.noise.entity.NoiseRecord;
import com.example.noise.entity.ThresholdRule;
import com.example.noise.mapper.AreaConfigMapper;
import com.example.noise.mapper.NoiseRecordMapper;
import com.example.noise.mapper.ThresholdRuleMapper;
import com.example.noise.service.AlertLogService;
import com.example.noise.service.CcswitchService;
import com.example.noise.service.ThresholdService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 动态阈值判断 Service 实现 — P0-3 + P1-4 阈值规则 CRUD
 * 根据当前时间确定时段标签 → 查询阈值规则 → 与噪声记录比较 → 标记异常
 */
@Service
public class ThresholdServiceImpl implements ThresholdService {

  /** 全局默认阈值，单位 dB(A) */
  private static final int GLOBAL_DEFAULT_THRESHOLD = 55;

  /** 四大功能区名称 */
  private static final Set<String> VALID_LOCATIONS = new HashSet<>(Arrays.asList(
      "图书馆", "食堂", "操场", "宿舍"
  ));

  /** 分贝值上限 */
  private static final int MAX_DECIBEL = 120;

  private final ThresholdRuleMapper thresholdRuleMapper;
  private final AreaConfigMapper areaConfigMapper;
  private final NoiseRecordMapper noiseRecordMapper;
  private final AlertLogService alertLogService;
  private final CcswitchService ccswitchService;

  public ThresholdServiceImpl(ThresholdRuleMapper thresholdRuleMapper,
                              AreaConfigMapper areaConfigMapper,
                              NoiseRecordMapper noiseRecordMapper,
                              AlertLogService alertLogService,
                              CcswitchService ccswitchService) {
    this.thresholdRuleMapper = thresholdRuleMapper;
    this.areaConfigMapper = areaConfigMapper;
    this.noiseRecordMapper = noiseRecordMapper;
    this.alertLogService = alertLogService;
    this.ccswitchService = ccswitchService;
  }

  @Override
  public ThresholdRule getCurrentThreshold(String location) {
    String timeSegment = getCurrentTimeSegment();

    // 1. 查 threshold_rule 表（只查 status=1 的启用规则）
    LambdaQueryWrapper<ThresholdRule> ruleWrapper = new LambdaQueryWrapper<>();
    ruleWrapper.eq(ThresholdRule::getLocation, location)
               .eq(ThresholdRule::getTimeSegment, timeSegment)
               .eq(ThresholdRule::getStatus, 1);
    ThresholdRule rule = thresholdRuleMapper.selectOne(ruleWrapper);
    if (rule != null) {
      return rule;
    }

    // 2. 规则缺失 → 查 area_config.default_threshold 兜底
    LambdaQueryWrapper<AreaConfig> areaWrapper = new LambdaQueryWrapper<>();
    areaWrapper.eq(AreaConfig::getAreaName, location);
    AreaConfig area = areaConfigMapper.selectOne(areaWrapper);
    if (area != null && area.getDefaultThreshold() != null) {
      ThresholdRule fallback = new ThresholdRule();
      fallback.setLocation(location);
      fallback.setTimeSegment(timeSegment);
      fallback.setThresholdValue(area.getDefaultThreshold());
      fallback.setDescription("兜底：功能区默认阈值（threshold_rule 未配置）");
      return fallback;
    }

    // 3. area_config 也缺失 → 全局默认 55 dB(A)
    ThresholdRule globalFallback = new ThresholdRule();
    globalFallback.setLocation(location);
    globalFallback.setTimeSegment(timeSegment);
    globalFallback.setThresholdValue(GLOBAL_DEFAULT_THRESHOLD);
    globalFallback.setDescription("兜底：全局默认阈值 55 dB(A)（threshold_rule + area_config 均未配置）");
    return globalFallback;
  }

  @Override
  public Map<String, Object> checkRecord(Long noiseRecordId) {
    // 1. 查噪声记录
    NoiseRecord record = noiseRecordMapper.selectById(noiseRecordId);
    if (record == null) {
      throw new BusinessException(2001, "噪声记录不存在");
    }

    // 2. 已判断则拒绝重复判断
    if (record.getIsAbnormal() != null) {
      throw new BusinessException(2004, "该记录已判断");
    }

    // 3. 查当前功能区对应时段的阈值
    ThresholdRule rule = getCurrentThreshold(record.getLocation());
    int thresholdValue = rule.getThresholdValue();

    // 4. 比较：分贝值 > 阈值则标记异常
    boolean isAbnormal = record.getDecibel().compareTo(BigDecimal.valueOf(thresholdValue)) > 0;
    record.setIsAbnormal(isAbnormal ? 1 : 0);
    record.setJudgedByModel("RULE_BASED");
    noiseRecordMapper.updateById(record);

    // 5. 超阈值则创建告警
    boolean alertCreated = false;
    if (isAbnormal) {
      alertLogService.createAlert(
          record.getId(),
          record.getLocation(),
          record.getDecibel(),
          thresholdValue,
          "超阈值"
      );
      alertCreated = true;
    }

    // 6. 组装返回结果
    Map<String, Object> result = new HashMap<>();
    result.put("noiseRecordId", record.getId());
    result.put("decibel", record.getDecibel());
    result.put("thresholdValue", thresholdValue);
    result.put("isAbnormal", record.getIsAbnormal());
    result.put("judgedByModel", record.getJudgedByModel());
    result.put("alertCreated", alertCreated);
    return result;
  }

  // ==================== P1-4 阈值规则 CRUD ====================

  @Override
  public List<ThresholdRule> listRules(String location) {
    LambdaQueryWrapper<ThresholdRule> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ThresholdRule::getStatus, 1);
    if (location != null && !location.isBlank()) {
      wrapper.eq(ThresholdRule::getLocation, location);
    }
    wrapper.orderByAsc(ThresholdRule::getLocation)
           .orderByAsc(ThresholdRule::getTimeSegment);
    return thresholdRuleMapper.selectList(wrapper);
  }

  @Override
  public ThresholdRule createRule(String location, String timeSegment, Integer thresholdValue, String description) {
    // 1. 校验功能区名称
    if (location == null || !VALID_LOCATIONS.contains(location)) {
      throw new BusinessException(400, "功能区名称无效，必须为：图书馆/食堂/操场/宿舍");
    }

    // 2. 校验时段不能为空
    if (timeSegment == null || timeSegment.isBlank()) {
      throw new BusinessException(400, "时段不能为空");
    }

    // 3. 校验阈值范围 0-120 dB(A)
    if (thresholdValue == null || thresholdValue < 0 || thresholdValue > MAX_DECIBEL) {
      throw new BusinessException(400, "阈值分贝值必须在 0-120 之间");
    }

    // 4. 检查唯一索引：同一功能区同一时段规则不能重复
    LambdaQueryWrapper<ThresholdRule> existsWrapper = new LambdaQueryWrapper<>();
    existsWrapper.eq(ThresholdRule::getLocation, location)
                 .eq(ThresholdRule::getTimeSegment, timeSegment);
    Long count = thresholdRuleMapper.selectCount(existsWrapper);
    if (count > 0) {
      throw new BusinessException(3001, "该功能区对应时段的阈值规则已存在");
    }

    // 5. 插入
    ThresholdRule rule = new ThresholdRule();
    rule.setLocation(location);
    rule.setTimeSegment(timeSegment);
    rule.setThresholdValue(thresholdValue);
    rule.setDescription(description != null ? description : "");
    rule.setStatus(1);       // 默认启用
    rule.setVersion(0);      // 乐观锁初始版本
    thresholdRuleMapper.insert(rule);
    return rule;
  }

  @Override
  public void updateRule(Long id, Integer thresholdValue, String description, Integer status, Integer version) {
    // 1. 查存在
    ThresholdRule rule = thresholdRuleMapper.selectById(id);
    if (rule == null) {
      throw new BusinessException(2003, "阈值规则不存在");
    }

    // 2. 乐观锁版本校验
    if (version == null || !version.equals(rule.getVersion())) {
      throw new BusinessException(3002, "乐观锁冲突：规则已被其他操作修改，请刷新后重试");
    }

    // 3. 校验阈值范围（如果提供了）
    if (thresholdValue != null && (thresholdValue < 0 || thresholdValue > MAX_DECIBEL)) {
      throw new BusinessException(400, "阈值分贝值必须在 0-120 之间");
    }

    // 4. 校验状态（如果提供了）
    if (status != null && status != 0 && status != 1) {
      throw new BusinessException(400, "状态值只能为 0（禁用）或 1（启用）");
    }

    // 5. 更新字段（只更新提供的字段）
    if (thresholdValue != null) {
      rule.setThresholdValue(thresholdValue);
    }
    if (description != null) {
      rule.setDescription(description);
    }
    if (status != null) {
      rule.setStatus(status);
    }
    // 乐观锁版本号 +1
    rule.setVersion(rule.getVersion() + 1);
    thresholdRuleMapper.updateById(rule);
  }

  @Override
  public void deleteRule(Long id) {
    ThresholdRule rule = thresholdRuleMapper.selectById(id);
    if (rule == null) {
      throw new BusinessException(2003, "阈值规则不存在");
    }
    thresholdRuleMapper.deleteById(id);
  }

  @Override
  public Map<String, Object> reloadRules() {
    // 统计当前启用规则数量
    LambdaQueryWrapper<ThresholdRule> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ThresholdRule::getStatus, 1);
    long ruleCount = thresholdRuleMapper.selectCount(wrapper);

    String reloadTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    Map<String, Object> result = new HashMap<>();
    result.put("reloadTime", reloadTime);
    result.put("ruleCount", ruleCount);

    // 通过 CcswitchService 通知 ccswitch Flask 服务热更新阈值规则
    try {
      ccswitchService.reloadThresholdRules();
      result.put("ccswitchStatus", "已通知");
    } catch (BusinessException e) {
      // ccswitch 不可用，返回提示但不阻断主流程
      result.put("ccswitchStatus", "ccswitch 不可用（" + e.getMessage() + "）");
    }

    return result;
  }

  // ==================== P1-1 统计自适应阈值 ====================

  @Override
  public Map<String, Object> getAdaptiveThreshold(String location) {
    // 1. 校验功能区名称
    if (location == null || !VALID_LOCATIONS.contains(location)) {
      throw new BusinessException(400, "功能区名称无效，必须为：图书馆/食堂/操场/宿舍");
    }

    // 2. 查功能区配置，获取 windowSize 和 kValue
    LambdaQueryWrapper<AreaConfig> areaWrapper = new LambdaQueryWrapper<>();
    areaWrapper.eq(AreaConfig::getAreaName, location);
    AreaConfig area = areaConfigMapper.selectOne(areaWrapper);

    // 使用默认值兜底
    int windowSize;
    BigDecimal kValue;
    if (area != null && area.getWindowSize() != null && area.getKValue() != null) {
      windowSize = area.getWindowSize();
      kValue = area.getKValue();
    } else {
      // 默认值：图书馆/宿舍 windowSize=15 kValue=2.00；食堂/操场 windowSize=10 kValue=3.00
      switch (location) {
        case "图书馆":
        case "宿舍":
          windowSize = 15;
          kValue = new BigDecimal("2.00");
          break;
        case "食堂":
        case "操场":
        default:
          windowSize = 10;
          kValue = new BigDecimal("3.00");
          break;
      }
    }

    // 3. 读取滑动窗口内的噪声记录（只取已判定的，按时间倒序，限制 windowSize 条）
    List<NoiseRecord> recentRecords = noiseRecordMapper.selectRecentForAdaptive(location, windowSize);
    int recordCount = recentRecords.size();

    // 4. 窗口内数据不足 → 回退到业务规则阈值
    if (recordCount < windowSize) {
      ThresholdRule fallbackRule = getCurrentThreshold(location);
      Map<String, Object> result = new HashMap<>();
      result.put("location", location);
      result.put("windowSize", windowSize);
      result.put("kValue", kValue);
      result.put("mean", null);
      result.put("stdDev", null);
      result.put("upperLimit", fallbackRule.getThresholdValue());
      result.put("lowerLimit", fallbackRule.getThresholdValue());
      result.put("windowRecordCount", recordCount);
      result.put("fallback", true);
      result.put("fallbackReason", "窗口内数据不足（当前 " + recordCount + " 条，需要 " + windowSize + " 条），回退业务规则阈值");
      return result;
    }

    // 5. 计算均值 μ
    BigDecimal sum = BigDecimal.ZERO;
    for (NoiseRecord r : recentRecords) {
      sum = sum.add(r.getDecibel());
    }
    BigDecimal mean = sum.divide(BigDecimal.valueOf(recordCount), 2, RoundingMode.HALF_UP);

    // 6. 计算标准差 σ = sqrt( Σ(xi - μ)² / n )
    BigDecimal varianceSum = BigDecimal.ZERO;
    for (NoiseRecord r : recentRecords) {
      BigDecimal diff = r.getDecibel().subtract(mean);
      varianceSum = varianceSum.add(diff.pow(2));
    }
    BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(recordCount), 4, RoundingMode.HALF_UP);
    // 标准差保留 1 位小数
    BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()))
        .setScale(1, RoundingMode.HALF_UP);

    // 7. 阈值 = μ ± k × σ
    BigDecimal kTimesStd = kValue.multiply(stdDev).setScale(1, RoundingMode.HALF_UP);
    BigDecimal upperLimit = mean.add(kTimesStd).setScale(1, RoundingMode.HALF_UP);
    BigDecimal lowerLimit = mean.subtract(kTimesStd).setScale(1, RoundingMode.HALF_UP);

    // 下限不低于 0
    if (lowerLimit.compareTo(BigDecimal.ZERO) < 0) {
      lowerLimit = BigDecimal.ZERO;
    }

    Map<String, Object> result = new HashMap<>();
    result.put("location", location);
    result.put("windowSize", windowSize);
    result.put("kValue", kValue);
    result.put("mean", mean);
    result.put("stdDev", stdDev);
    result.put("upperLimit", upperLimit);
    result.put("lowerLimit", lowerLimit);
    result.put("windowRecordCount", recordCount);
    result.put("fallback", false);
    return result;
  }

  @Override
  public void updateAdaptiveConfig(List<Map<String, Object>> areaConfigs) {
    if (areaConfigs == null || areaConfigs.isEmpty()) {
      throw new BusinessException(400, "配置列表不能为空");
    }

    for (Map<String, Object> config : areaConfigs) {
      String location = (String) config.get("location");
      Object windowSizeObj = config.get("windowSize");
      Object kValueObj = config.get("kValue");

      // 校验功能区名称
      if (location == null || !VALID_LOCATIONS.contains(location)) {
        throw new BusinessException(400, "功能区名称无效: " + location);
      }

      // 校验 windowSize: 5-100 的整数
      if (windowSizeObj == null) {
        throw new BusinessException(400, location + " 的 windowSize 不能为空");
      }
      int windowSize;
      if (windowSizeObj instanceof Integer) {
        windowSize = (Integer) windowSizeObj;
      } else {
        windowSize = Integer.parseInt(windowSizeObj.toString());
      }
      if (windowSize < 5 || windowSize > 100) {
        throw new BusinessException(400, location + " 的 windowSize 必须在 5-100 之间");
      }

      // 校验 kValue: 1.0-5.0
      if (kValueObj == null) {
        throw new BusinessException(400, location + " 的 kValue 不能为空");
      }
      BigDecimal kValue;
      if (kValueObj instanceof BigDecimal) {
        kValue = (BigDecimal) kValueObj;
      } else if (kValueObj instanceof Double) {
        kValue = BigDecimal.valueOf((Double) kValueObj);
      } else {
        kValue = new BigDecimal(kValueObj.toString());
      }
      if (kValue.compareTo(new BigDecimal("1.0")) < 0 || kValue.compareTo(new BigDecimal("5.0")) > 0) {
        throw new BusinessException(400, location + " 的 kValue 必须在 1.0-5.0 之间");
      }

      // 查功能区配置
      LambdaQueryWrapper<AreaConfig> wrapper = new LambdaQueryWrapper<>();
      wrapper.eq(AreaConfig::getAreaName, location);
      AreaConfig area = areaConfigMapper.selectOne(wrapper);
      if (area == null) {
        throw new BusinessException(6001, "功能区配置不存在: " + location);
      }

      // 更新
      area.setWindowSize(windowSize);
      area.setKValue(kValue.setScale(2, RoundingMode.HALF_UP));
      areaConfigMapper.updateById(area);
    }
  }

  // ==================== P1-2 混合阈值模型 ====================

  @Override
  public Map<String, Object> getHybridStatus() {
    // 遍历 4 个功能区，统计全局混合模型状态
    int totalWindows = 0;
    int abnormalWindows = 0;
    boolean isTriggered = false;
    String triggerReason = "";

    // 检查当前是否处于特殊时段（考试周、午休、夜间静校）
    String currentSegment = getCurrentTimeSegment();
    LocalTime now = LocalTime.now();
    boolean isSpecialPeriod = "午休".equals(currentSegment) || "夜间静校".equals(currentSegment);

    for (String location : VALID_LOCATIONS) {
      // 对每个功能区计算最近 3 个窗口的异常率
      int windowSize = getEffectiveWindowSize(location);

      // 取最近 3 个窗口的数据（每个窗口 windowSize 条，共 3*windowSize 条）
      int totalSample = windowSize * 3;
      List<NoiseRecord> recentRecords = noiseRecordMapper.selectRecentForAdaptive(location, totalSample);
      totalWindows += 3;

      if (recentRecords.size() < windowSize) {
        // 数据不足，不算为异常窗口
        continue;
      }

      // 按 windowSize 分窗口
      int windowCount = Math.min(3, recentRecords.size() / windowSize);
      for (int w = 0; w < windowCount; w++) {
        int start = w * windowSize;
        int end = Math.min(start + windowSize, recentRecords.size());
        List<NoiseRecord> windowData = recentRecords.subList(start, end);

        // 计算窗口均值和标准差
        BigDecimal sum = BigDecimal.ZERO;
        for (NoiseRecord r : windowData) {
          sum = sum.add(r.getDecibel());
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(windowData.size()), 2, RoundingMode.HALF_UP);

        BigDecimal varSum = BigDecimal.ZERO;
        for (NoiseRecord r : windowData) {
          BigDecimal diff = r.getDecibel().subtract(mean);
          varSum = varSum.add(diff.pow(2));
        }
        BigDecimal variance = varSum.divide(BigDecimal.valueOf(windowData.size()), 4, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()))
            .setScale(1, RoundingMode.HALF_UP);

        BigDecimal kValue = getEffectiveKValue(location);
        BigDecimal upperLimit = mean.add(kValue.multiply(stdDev)).setScale(1, RoundingMode.HALF_UP);

        // 计算窗口异常率：分贝值 > 统计上限的记录占比
        long abnormalCount = windowData.stream()
            .filter(r -> r.getDecibel().compareTo(upperLimit) > 0)
            .count();
        double abnormalRate = (double) abnormalCount / windowData.size();

        // 条件 1：连续 3 个窗口异常率 > 10% → 触发
        if (abnormalRate > 0.10) {
          abnormalWindows++;
        }
      }
    }

    double abnormalRate3Windows = (double) abnormalWindows / Math.max(totalWindows, 1);

    // 检查 3 个触发条件
    boolean cond1 = abnormalWindows >= 3 && abnormalRate3Windows > 0.10;
    boolean cond2 = isSpecialPeriod;
    boolean cond3 = false; // 分贝骤升 ≥15dB 需要实时数据流判断，此处从报告取默认

    isTriggered = cond1 || cond2 || cond3;

    StringBuilder reasonBuilder = new StringBuilder();
    if (cond1) {
      reasonBuilder.append("连续3个窗口异常率>10%（异常窗口 ").append(abnormalWindows)
          .append("，可计算窗口 ").append(totalWindows).append("）；");
    }
    if (cond2) {
      reasonBuilder.append("处于特殊时段（").append(currentSegment).append("）；");
    }
    if (cond3) {
      reasonBuilder.append("检测到分贝骤升≥15dB；");
    }
    triggerReason = reasonBuilder.toString();

    Map<String, Object> result = new HashMap<>();
    result.put("currentMode", isTriggered ? "RULE_BASED" : "STAT_ADAPTIVE");
    result.put("abnormalRate3Windows", Math.round(abnormalRate3Windows * 1000.0) / 1000.0);
    result.put("isTriggered", isTriggered);
    result.put("triggerReason", triggerReason.isEmpty() ? "" : triggerReason);
    return result;
  }

  @Override
  public Map<String, Object> getHybridPerformance() {
    // 返回研究报告 §4 实验数据（固定值）
    Map<String, Object> result = new HashMap<>();
    result.put("accuracy", 92.6);
    result.put("precision", 90.8);
    result.put("recall", 91.7);
    result.put("f1Score", 91.2);
    result.put("falsePositiveRate", 4.0);

    Map<String, Integer> modeDistribution = new HashMap<>();
    modeDistribution.put("RULE_BASED", 30);
    modeDistribution.put("ADAPTIVE", 67);
    modeDistribution.put("HYBRID", 3);
    result.put("modeDistribution", modeDistribution);

    return result;
  }

  // ==================== P1-2 混合模型自动判断 ====================

  @Override
  public void autoJudgeWithHybrid(NoiseRecord record) {
    // 1. 已判断则跳过
    if (record.getIsAbnormal() != null) {
      return;
    }

    boolean useRuleBased = false;
    String judgedByModel = "STAT_ADAPTIVE";
    int thresholdValue;

    // 2. 判断是否触发业务规则回退条件
    String currentSegment = getCurrentTimeSegment();
    boolean isSpecialPeriod = "午休".equals(currentSegment) || "夜间静校".equals(currentSegment);

    // 条件 1：连续 3 个窗口异常率 > 10%
    boolean cond1 = check3WindowAbnormalRate(record.getLocation());

    if (cond1 || isSpecialPeriod) {
      useRuleBased = true;
      judgedByModel = "RULE_BASED";
    }

    if (useRuleBased) {
      // 使用业务规则阈值
      ThresholdRule rule = getCurrentThreshold(record.getLocation());
      thresholdValue = rule.getThresholdValue();
    } else {
      // 使用统计自适应阈值
      Map<String, Object> adaptive = getAdaptiveThreshold(record.getLocation());
      if (Boolean.TRUE.equals(adaptive.get("fallback"))) {
        // 自适应也回退 → 用业务规则
        ThresholdRule rule = getCurrentThreshold(record.getLocation());
        thresholdValue = rule.getThresholdValue();
        judgedByModel = "RULE_BASED";
      } else {
        BigDecimal upperLimit = (BigDecimal) adaptive.get("upperLimit");
        thresholdValue = upperLimit.intValue();
      }
    }

    // 3. 比较分贝值与阈值
    boolean isAbnormal = record.getDecibel().compareTo(BigDecimal.valueOf(thresholdValue)) > 0;
    record.setIsAbnormal(isAbnormal ? 1 : 0);
    record.setJudgedByModel(judgedByModel);
    noiseRecordMapper.updateById(record);

    // 4. 超阈值则创建告警
    if (isAbnormal) {
      String alertType = "超阈值";
      if (judgedByModel.equals("RULE_BASED")) {
        alertType = "超阈值";
      }
      alertLogService.createAlert(
          record.getId(),
          record.getLocation(),
          record.getDecibel(),
          thresholdValue,
          alertType
      );
    }
  }

  // ==================== P1-2 私有辅助方法 ====================

  /**
   * 获取功能区的有效窗口大小（配置缺失时用默认值）
   */
  private int getEffectiveWindowSize(String location) {
    LambdaQueryWrapper<AreaConfig> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(AreaConfig::getAreaName, location);
    AreaConfig area = areaConfigMapper.selectOne(wrapper);
    if (area != null && area.getWindowSize() != null) {
      return area.getWindowSize();
    }
    // 默认值
    return "图书馆".equals(location) || "宿舍".equals(location) ? 15 : 10;
  }

  /**
   * 获取功能区的有效 k 值（配置缺失时用默认值）
   */
  private BigDecimal getEffectiveKValue(String location) {
    LambdaQueryWrapper<AreaConfig> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(AreaConfig::getAreaName, location);
    AreaConfig area = areaConfigMapper.selectOne(wrapper);
    if (area != null && area.getKValue() != null) {
      return area.getKValue();
    }
    // 默认值
    return "图书馆".equals(location) || "宿舍".equals(location)
        ? new BigDecimal("2.00") : new BigDecimal("3.00");
  }

  /**
   * 检查某功能区最近 3 个滑动窗口的异常率是否均 > 10%
   *
   * @param location 功能区名称
   * @return true 如果满足触发条件 1
   */
  private boolean check3WindowAbnormalRate(String location) {
    int windowSize = getEffectiveWindowSize(location);
    BigDecimal kValue = getEffectiveKValue(location);

    // 取最近 3 个窗口的数据
    int totalSample = windowSize * 3;
    List<NoiseRecord> recentRecords = noiseRecordMapper.selectRecentForAdaptive(location, totalSample);

    int minRequired = windowSize; // 每个窗口至少需要 windowSize 条数据
    if (recentRecords.size() < minRequired * 2) {
      // 数据不足，不触发
      return false;
    }

    // 按 windowSize 分组
    int fullWindows = recentRecords.size() / windowSize;
    int checkWindows = Math.min(3, fullWindows);
    int consecutiveAbnormal = 0;
    int requiredConsecutive = checkWindows; // 需要所有可计算的窗口都异常

    for (int w = 0; w < checkWindows; w++) {
      int start = w * windowSize;
      int end = start + windowSize;
      List<NoiseRecord> windowData = recentRecords.subList(start, end);

      // 计算窗口统计阈值上限
      BigDecimal sum = BigDecimal.ZERO;
      for (NoiseRecord r : windowData) {
        sum = sum.add(r.getDecibel());
      }
      BigDecimal mean = sum.divide(BigDecimal.valueOf(windowData.size()), 2, RoundingMode.HALF_UP);

      BigDecimal varSum = BigDecimal.ZERO;
      for (NoiseRecord r : windowData) {
        BigDecimal diff = r.getDecibel().subtract(mean);
        varSum = varSum.add(diff.pow(2));
      }
      BigDecimal variance = varSum.divide(BigDecimal.valueOf(windowData.size()), 4, RoundingMode.HALF_UP);
      BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()))
          .setScale(1, RoundingMode.HALF_UP);

      BigDecimal upperLimit = mean.add(kValue.multiply(stdDev)).setScale(1, RoundingMode.HALF_UP);

      // 计算异常率
      long abnormalInWindow = windowData.stream()
          .filter(r -> r.getDecibel().compareTo(upperLimit) > 0)
          .count();
      double rate = (double) abnormalInWindow / windowData.size();

      if (rate > 0.10) {
        consecutiveAbnormal++;
      } else {
        break; // 不连续则中断
      }
    }

    return consecutiveAbnormal >= requiredConsecutive;
  }

  /**
   * 根据当前时间返回时段标签
   * 映射规则（来自研究报告 §3.3.1）：
   *   7:30–8:00 → 早读
   *   8:00–12:00 → 上课
   *   12:00–14:00 → 午休
   *   14:00–18:00 → 上课
   *   18:00–22:00 → 活动/晚自修
   *   22:00–7:30 → 夜间静校
   */
  private String getCurrentTimeSegment() {
    LocalTime now = LocalTime.now();
    LocalTime t0730 = LocalTime.of(7, 30);
    LocalTime t0800 = LocalTime.of(8, 0);
    LocalTime t1200 = LocalTime.of(12, 0);
    LocalTime t1400 = LocalTime.of(14, 0);
    LocalTime t1800 = LocalTime.of(18, 0);
    LocalTime t2200 = LocalTime.of(22, 0);

    if (!now.isBefore(t0730) && now.isBefore(t0800)) {
      return "早读";
    }
    if (!now.isBefore(t0800) && now.isBefore(t1200)) {
      return "上课";
    }
    if (!now.isBefore(t1200) && now.isBefore(t1400)) {
      return "午休";
    }
    if (!now.isBefore(t1400) && now.isBefore(t1800)) {
      return "上课";
    }
    if (!now.isBefore(t1800) && now.isBefore(t2200)) {
      return "活动/晚自修";
    }
    // 22:00–7:30
    return "夜间静校";
  }
}
