package com.example.noise.service;

import java.util.List;
import java.util.Map;

/**
 * 统计服务接口 — P1-3 统计/可视化
 */
public interface StatisticsService {

  /**
   * 查询指定功能区时间范围内的噪声时间序列数据
   *
   * @param location 功能区名称
   * @param dateFrom 起始时间 ISO 8601（可选）
   * @param dateTo   截止时间 ISO 8601（可选）
   * @return {"location": "...", "points": [{"timePoint": "...", "decibel": 42.3, "isAbnormal": 0}]}
   */
  Map<String, Object> getTimeseries(String location, String dateFrom, String dateTo);

  /**
   * 各功能区统计汇总
   *
   * @param dateFrom 起始时间 ISO 8601（可选）
   * @param dateTo   截止时间 ISO 8601（可选）
   * @return {"areas": [...], "summary": {...}}
   */
  Map<String, Object> getAreaStats(String dateFrom, String dateTo);

  /**
   * 模型性能对比数据（P1 阶段返回固定实验数据）
   *
   * @return {"models": [...]}
   */
  Map<String, Object> getModelPerformance();

  /**
   * 多维度跨维分析（P2）
   *
   * @param xDim     X 轴维度：time_segment / location / noise_type
   * @param yDim     Y 轴指标：avg_decibel / abnormal_rate / alert_count
   * @param dateFrom 起始时间 ISO 8601（可选）
   * @param dateTo   截止时间 ISO 8601（可选）
   * @return [{"xValue": "...", "yValue": N, "recordCount": N}, ...]
   */
  List<Map<String, Object>> getMultiDimAnalysis(String xDim, String yDim,
                                                 String dateFrom, String dateTo);

  /**
   * 热力图数据（P2）：功能区 × 时段 → 平均分贝矩阵
   *
   * @param dateFrom 起始时间 ISO 8601（可选）
   * @param dateTo   截止时间 ISO 8601（可选）
   * @return {"xLabels": [...], "yLabels": [...], "data": [[...], ...]}
   */
  Map<String, Object> getHeatmap(String dateFrom, String dateTo);

  /**
   * 雷达图数据（P2）：各功能区多维度指标
   *
   * @param dateFrom 起始时间 ISO 8601（可选）
   * @param dateTo   截止时间 ISO 8601（可选）
   * @return [{"location": "...", "dimensions": [...]}, ...] + dimensionLabels
   */
  Map<String, Object> getRadar(String dateFrom, String dateTo);
}
