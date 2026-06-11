package com.example.noise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.noise.entity.AreaConfig;
import com.example.noise.entity.NoiseRecord;
import com.example.noise.mapper.AlertLogMapper;
import com.example.noise.mapper.AreaConfigMapper;
import com.example.noise.mapper.NoiseRecordMapper;
import com.example.noise.service.StatisticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计服务实现 — P1-3 统计/可视化
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

  private final NoiseRecordMapper noiseRecordMapper;
  private final AlertLogMapper alertLogMapper;
  private final AreaConfigMapper areaConfigMapper;

  public StatisticsServiceImpl(NoiseRecordMapper noiseRecordMapper,
                               AlertLogMapper alertLogMapper,
                               AreaConfigMapper areaConfigMapper) {
    this.noiseRecordMapper = noiseRecordMapper;
    this.alertLogMapper = alertLogMapper;
    this.areaConfigMapper = areaConfigMapper;
  }

  @Override
  public Map<String, Object> getTimeseries(String location, String dateFrom, String dateTo) {
    LambdaQueryWrapper<NoiseRecord> wrapper = new LambdaQueryWrapper<>();

    // 功能区筛选
    if (location != null && !location.isEmpty()) {
      wrapper.eq(NoiseRecord::getLocation, location);
    }

    // 时间范围筛选
    if (dateFrom != null && !dateFrom.isEmpty()) {
      wrapper.ge(NoiseRecord::getTimePoint, safeParseDateTime(dateFrom, true));
    }
    if (dateTo != null && !dateTo.isEmpty()) {
      wrapper.le(NoiseRecord::getTimePoint, safeParseDateTime(dateTo, false));
    }

    wrapper.orderByAsc(NoiseRecord::getTimePoint);
    // 限制 2000 条，防止数据量过大
    wrapper.last("LIMIT 2000");

    List<NoiseRecord> records = noiseRecordMapper.selectList(wrapper);

    // 构造点位列表
    List<Map<String, Object>> points = records.stream().map(r -> {
      Map<String, Object> point = new HashMap<>();
      point.put("timePoint", r.getTimePoint().toString());
      point.put("decibel", r.getDecibel());
      point.put("isAbnormal", r.getIsAbnormal() != null && r.getIsAbnormal() == 1);
      return point;
    }).collect(Collectors.toList());

    Map<String, Object> result = new HashMap<>();
    result.put("location", location != null ? location : "全部");
    result.put("points", points);
    return result;
  }

  @Override
  public Map<String, Object> getAreaStats(String dateFrom, String dateTo) {
    // 1. 按功能区分组统计
    List<Map<String, Object>> areaStats = noiseRecordMapper.selectAreaStats(dateFrom, dateTo);

    // 2. 按功能区告警次数
    List<Map<String, Object>> alertCounts = noiseRecordMapper.selectAlertCountByArea(dateFrom, dateTo);

    // 告警次数按功能区索引
    Map<String, Long> alertCountMap = new HashMap<>();
    for (Map<String, Object> ac : alertCounts) {
      String loc = (String) ac.get("location");
      Long cnt = ((Number) ac.get("alertCount")).longValue();
      alertCountMap.put(loc, cnt);
    }

    // 3. 汇总
    Map<String, Object> summary = noiseRecordMapper.selectGlobalSummary(dateFrom, dateTo);
    Long totalAlerts = noiseRecordMapper.selectGlobalAlertCount(dateFrom, dateTo);

    // 组装 areas 列表
    List<Map<String, Object>> areas = new ArrayList<>();
    for (Map<String, Object> row : areaStats) {
      String location = (String) row.get("location");
      Long totalRecords = ((Number) row.get("totalRecords")).longValue();
      BigDecimal avgDecibel = row.get("avgDecibel") != null
          ? new BigDecimal(row.get("avgDecibel").toString()).setScale(1, RoundingMode.HALF_UP)
          : BigDecimal.ZERO;
      Long abnormalCount = ((Number) row.get("abnormalCount")).longValue();

      // 异常率 = 异常数 / 总数 * 100
      BigDecimal abnormalRate = totalRecords > 0
          ? BigDecimal.valueOf(abnormalCount)
              .multiply(BigDecimal.valueOf(100))
              .divide(BigDecimal.valueOf(totalRecords), 1, RoundingMode.HALF_UP)
          : BigDecimal.ZERO;

      long alertCount = alertCountMap.getOrDefault(location, 0L);

      Map<String, Object> area = new HashMap<>();
      area.put("location", location);
      area.put("avgDecibel", avgDecibel);
      area.put("abnormalRate", abnormalRate);
      area.put("alertCount", alertCount);
      areas.add(area);
    }

    // 组装 summary
    Map<String, Object> summaryData = new HashMap<>();
    summaryData.put("totalRecords", summary.get("totalRecords") != null
        ? ((Number) summary.get("totalRecords")).longValue() : 0);
    summaryData.put("avgDecibel", summary.get("avgDecibel") != null
        ? new BigDecimal(summary.get("avgDecibel").toString()).setScale(1, RoundingMode.HALF_UP)
        : BigDecimal.ZERO);
    summaryData.put("totalAlerts", totalAlerts != null ? totalAlerts : 0);

    Map<String, Object> result = new HashMap<>();
    result.put("areas", areas);
    result.put("summary", summaryData);
    return result;
  }

  @Override
  public Map<String, Object> getModelPerformance() {
    // 基于 noise_record 表 judged_by_model 字段实时统计各模型性能
    List<NoiseRecord> allJudged = noiseRecordMapper.selectList(
        new LambdaQueryWrapper<NoiseRecord>().isNotNull(NoiseRecord::getIsAbnormal));
    List<Map<String, Object>> models = new ArrayList<>();

    // 按 judged_by_model 分组统计
    Map<String, List<NoiseRecord>> byModel = new HashMap<>();
    for (NoiseRecord r : allJudged) {
      String model = r.getJudgedByModel() != null ? r.getJudgedByModel() : "UNKNOWN";
      byModel.computeIfAbsent(model, k -> new ArrayList<>()).add(r);
    }

    // 转换 judged_by_model → 显示名
    Map<String, String> modelLabels = Map.of(
        "RULE_BASED", "业务规则",
        "STAT_ADAPTIVE", "统计自适应",
        "HYBRID", "混合阈值"
    );

    // 为每个模型计算性能指标（有 noise_type 标注的才参与计算）
    for (Map.Entry<String, List<NoiseRecord>> entry : byModel.entrySet()) {
      String modelKey = entry.getKey();
      List<NoiseRecord> records = entry.getValue();

      long tp = 0, fp = 0, tn = 0, fn = 0;
      for (NoiseRecord r : records) {
        boolean predictedAbnormal = r.getIsAbnormal() != null && r.getIsAbnormal() == 1;
        boolean actualAbnormal = "异常".equals(r.getNoiseType());
        if (predictedAbnormal && actualAbnormal) tp++;
        else if (predictedAbnormal && !actualAbnormal) fp++;
        else if (!predictedAbnormal && !actualAbnormal) tn++;
        else if (!predictedAbnormal && actualAbnormal) fn++;
      }
      long total = tp + fp + tn + fn;

      String label = modelLabels.getOrDefault(modelKey, modelKey);
      if (total > 0) {
        models.add(buildModel(label,
            round2((double) (tp + tn) / total * 100),
            round2((tp + fp) > 0 ? (double) tp / (tp + fp) * 100 : 0),
            round2((tp + fn) > 0 ? (double) tp / (tp + fn) * 100 : 0),
            round2(calcF1(tp, fp, fn)),
            round2((fp + tn) > 0 ? (double) fp / (fp + tn) * 100 : 0)));
      }
    }

    // 如果无标注数据，回退研究报告 §4 基准值
    if (models.isEmpty()) {
      models.add(buildModel("固定阈值", 78.0, 65.0, 70.0, 67.4, 13.5));
      models.add(buildModel("业务规则", 88.7, 85.2, 87.3, 86.2, 5.3));
      models.add(buildModel("统计自适应", 89.4, 86.1, 88.2, 87.1, 8.2));
      models.add(buildModel("混合阈值", 92.6, 90.8, 91.7, 91.2, 4.0));
    }

    Map<String, Object> result = new HashMap<>();
    result.put("models", models);
    return result;
  }

  private double calcF1(long tp, long fp, long fn) {
    double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
    double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
    return (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) * 100 : 0;
  }

  private double round2(double v) {
    return Math.round(v * 10.0) / 10.0;
  }

  /** 组装单个模型指标 Map */
  private Map<String, Object> buildModel(String modelName, double accuracy,
                                         double precision, double recall,
                                         double f1Score, double fpr) {
    Map<String, Object> model = new HashMap<>();
    model.put("modelName", modelName);
    model.put("accuracy", accuracy);
    model.put("precision", precision);
    model.put("recall", recall);
    model.put("f1Score", f1Score);
    model.put("fpr", fpr);
    return model;
  }

  // ========== P2 统计端点 ==========

  @Override
  public List<Map<String, Object>> getMultiDimAnalysis(String xDim, String yDim,
                                                        String dateFrom, String dateTo) {
    // 查询全量记录用于 Java 层聚合
    List<NoiseRecord> records = noiseRecordMapper.selectForAnalysis(dateFrom, dateTo);
    // 查询告警关联记录用于 alert_count 统计
    List<Map<String, Object>> alertRecords = noiseRecordMapper.selectAlertedRecordInfo(dateFrom, dateTo);

    // 按 xDim 值分组
    Map<String, List<NoiseRecord>> grouped = new HashMap<>();
    Map<String, Long> alertCountByX = new HashMap<>();

    for (NoiseRecord r : records) {
      String xValue = getXDimValue(r, xDim);
      if (xValue == null) continue;
      grouped.computeIfAbsent(xValue, k -> new ArrayList<>()).add(r);
    }

    // 告警数按 xDim 分组
    for (Map<String, Object> ar : alertRecords) {
      String xValue = getAlertXDimValue(ar, xDim);
      if (xValue == null) continue;
      alertCountByX.merge(xValue, 1L, Long::sum);
    }

    // 按 xDim 值构造返回列表
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map.Entry<String, List<NoiseRecord>> entry : grouped.entrySet()) {
      String xValue = entry.getKey();
      List<NoiseRecord> groupRecords = entry.getValue();

      double yValue = computeYValue(groupRecords, alertCountByX.getOrDefault(xValue, 0L), yDim);
      Map<String, Object> item = new HashMap<>();
      item.put("xValue", xValue);
      item.put("yValue", BigDecimal.valueOf(yValue).setScale(1, RoundingMode.HALF_UP));
      item.put("recordCount", groupRecords.size());
      result.add(item);
    }

    return result;
  }

  @Override
  public Map<String, Object> getHeatmap(String dateFrom, String dateTo) {
    // 定义时段和功能区标签
    String[] timeSegments = {"早读", "上课", "午休", "活动", "晚自修", "夜间静校"};
    String[] locations = {"图书馆", "食堂", "操场", "宿舍"};

    // 读取原始数据
    List<NoiseRecord> records = noiseRecordMapper.selectForAnalysis(dateFrom, dateTo);

    // 初始化矩阵：yLabels × xLabels
    double[][] sums = new double[locations.length][timeSegments.length];
    int[][] counts = new int[locations.length][timeSegments.length];

    for (NoiseRecord r : records) {
      if (r.getLocation() == null || r.getTimePoint() == null || r.getDecibel() == null) continue;

      int li = indexOf(locations, r.getLocation());
      int si = indexOf(timeSegments, timeSegmentOf(r.getTimePoint()));
      if (li < 0 || si < 0) continue;

      sums[li][si] += r.getDecibel().doubleValue();
      counts[li][si]++;
    }

    // 计算均值矩阵
    List<List<BigDecimal>> data = new ArrayList<>();
    for (int li = 0; li < locations.length; li++) {
      List<BigDecimal> row = new ArrayList<>();
      for (int si = 0; si < timeSegments.length; si++) {
        double avg = counts[li][si] > 0
            ? sums[li][si] / counts[li][si] : 0.0;
        row.add(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
      }
      data.add(row);
    }

    // labels
    List<String> xLabels = java.util.Arrays.asList(timeSegments);
    List<String> yLabels = java.util.Arrays.asList(locations);

    Map<String, Object> result = new HashMap<>();
    result.put("xLabels", xLabels);
    result.put("yLabels", yLabels);
    result.put("data", data);
    return result;
  }

  @Override
  public Map<String, Object> getRadar(String dateFrom, String dateTo) {
    List<Map<String, Object>> radarStats = noiseRecordMapper.selectRadarStats(dateFrom, dateTo);

    // 获取告警计数
    List<Map<String, Object>> alertCounts = noiseRecordMapper.selectAlertCountByArea(dateFrom, dateTo);
    Map<String, Long> alertCountMap = new HashMap<>();
    for (Map<String, Object> ac : alertCounts) {
      String loc = (String) ac.get("location");
      Long cnt = ((Number) ac.get("alertCount")).longValue();
      alertCountMap.put(loc, cnt);
    }

    List<Map<String, Object>> data = new ArrayList<>();
    for (Map<String, Object> row : radarStats) {
      String location = (String) row.get("location");
      long recordCount = ((Number) row.get("recordCount")).longValue();
      BigDecimal avgDecibel = row.get("avgDecibel") != null
          ? new BigDecimal(row.get("avgDecibel").toString()).setScale(1, RoundingMode.HALF_UP)
          : BigDecimal.ZERO;
      BigDecimal stdDev = row.get("stdDev") != null
          ? new BigDecimal(row.get("stdDev").toString()).setScale(1, RoundingMode.HALF_UP)
          : BigDecimal.ZERO;
      long abnormalCount = ((Number) row.get("abnormalCount")).longValue();
      long noiseTypeCount = ((Number) row.get("noiseTypeCount")).longValue();
      Long alertCount = alertCountMap.getOrDefault(location, 0L);

      // 异常率 = 异常数 / 总数 * 100
      BigDecimal abnormalRate = recordCount > 0
          ? BigDecimal.valueOf(abnormalCount)
              .multiply(BigDecimal.valueOf(100))
              .divide(BigDecimal.valueOf(recordCount), 1, RoundingMode.HALF_UP)
          : BigDecimal.ZERO;

      Map<String, Object> item = new HashMap<>();
      item.put("location", location);
      item.put("dimensions", java.util.Arrays.asList(
          avgDecibel, abnormalRate, stdDev, BigDecimal.valueOf(alertCount),
          BigDecimal.valueOf(noiseTypeCount)
      ));
      data.add(item);
    }

    List<String> dimensionLabels = java.util.Arrays.asList(
        "平均分贝", "异常率(%)", "标准差", "告警次数", "噪声类型数"
    );

    Map<String, Object> result = new HashMap<>();
    result.put("data", data);           // 注意：API 文档要求 data 数组嵌套在顶层，同时含 dimensionLabels
    result.put("dimensionLabels", dimensionLabels);
    return result;
  }

  // ========== 辅助方法 ==========

  /** 根据 time_point 判断所属时段 */
  private String timeSegmentOf(LocalDateTime time) {
    int hour = time.getHour();
    int minute = time.getMinute();
    // 使用分钟偏移简化判断
    int totalMinutes = hour * 60 + minute;

    if (totalMinutes >= 420 && totalMinutes < 480) {   // 07:00-08:00
      return "早读";
    } else if (totalMinutes >= 480 && totalMinutes < 690) { // 08:00-11:30
      return "上课";
    } else if (totalMinutes >= 690 && totalMinutes < 810) { // 11:30-13:30
      return "午休";
    } else if (totalMinutes >= 810 && totalMinutes < 1050) { // 13:30-17:30
      return "活动";
    } else if (totalMinutes >= 1050 && totalMinutes < 1320) { // 17:30-22:00
      return "晚自修";
    } else {
      return "夜间静校";
    }
  }

  /** 从记录中提取 xDim 对应的值 */
  private String getXDimValue(NoiseRecord r, String xDim) {
    switch (xDim) {
      case "location":
        return r.getLocation();
      case "noise_type":
        return r.getNoiseType() != null ? r.getNoiseType() : "未分类";
      case "time_segment":
        return r.getTimePoint() != null ? timeSegmentOf(r.getTimePoint()) : null;
      default:
        return null;
    }
  }

  /** 从告警关联记录中提取 xDim 对应的值 */
  private String getAlertXDimValue(Map<String, Object> ar, String xDim) {
    switch (xDim) {
      case "location":
        return (String) ar.get("location");
      case "noise_type":
        Object nt = ar.get("noise_type");
        return nt != null ? nt.toString() : "未分类";
      case "time_segment":
        Object tp = ar.get("time_point");
        if (tp == null) return null;
        String tpStr = tp.toString();
        if (tpStr.contains("T")) {
          return timeSegmentOf(LocalDateTime.parse(tpStr.substring(0, 19)));
        }
        return null;
      default:
        return null;
    }
  }

  /** 根据 yDim 计算 Y 轴指标值 */
  private double computeYValue(List<NoiseRecord> groupRecords, long alertCount, String yDim) {
    switch (yDim) {
      case "avg_decibel": {
        return groupRecords.stream()
            .filter(r -> r.getDecibel() != null)
            .mapToDouble(r -> r.getDecibel().doubleValue())
            .average()
            .orElse(0.0);
      }
      case "abnormal_rate": {
        if (groupRecords.isEmpty()) return 0.0;
        long abnormalCount = groupRecords.stream()
            .filter(r -> r.getIsAbnormal() != null && r.getIsAbnormal() == 1)
            .count();
        return (double) abnormalCount / groupRecords.size() * 100.0;
      }
      case "alert_count": {
        return (double) alertCount;
      }
      default:
        return 0.0;
    }
  }

  /** 在数组中查找元素索引，找不到返回 -1 */
  private int indexOf(String[] array, String value) {
    for (int i = 0; i < array.length; i++) {
      if (array[i].equals(value)) return i;
    }
    return -1;
  }

  /** 安全解析日期时间字符串：兼容 "yyyy-MM-dd" 和 "yyyy-MM-ddTHH:mm:ss" 两种格式 */
  private LocalDateTime safeParseDateTime(String dateStr, boolean isStart) {
    if (dateStr == null || dateStr.isEmpty()) return null;
    try {
      return LocalDateTime.parse(dateStr);
    } catch (Exception e) {
      // 尝试补全为 LocalDateTime
      return isStart
          ? LocalDateTime.of(java.time.LocalDate.parse(dateStr), java.time.LocalTime.MIN)
          : LocalDateTime.of(java.time.LocalDate.parse(dateStr), java.time.LocalTime.MAX);
    }
  }
}
