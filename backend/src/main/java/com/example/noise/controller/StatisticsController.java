package com.example.noise.controller;

import com.example.noise.common.Result;
import com.example.noise.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计控制器 — P1-3 统计/可视化
 */
@RestController
public class StatisticsController {

  private final StatisticsService statisticsService;

  public StatisticsController(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  /** 时间序列数据：指定功能区的分贝曲线 + 异常点 */
  @GetMapping("/api/statistics/timeseries")
  public Result<Map<String, Object>> getTimeseries(
      @RequestParam(required = false) String location,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo) {
    Map<String, Object> data = statisticsService.getTimeseries(location, dateFrom, dateTo);
    return Result.success(data);
  }

  /** 各功能区统计汇总 */
  @GetMapping("/api/statistics/areas")
  public Result<Map<String, Object>> getAreaStats(
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo) {
    Map<String, Object> data = statisticsService.getAreaStats(dateFrom, dateTo);
    return Result.success(data);
  }

  /** 模型性能对比数据 */
  @GetMapping("/api/statistics/models")
  public Result<Map<String, Object>> getModelPerformance() {
    Map<String, Object> data = statisticsService.getModelPerformance();
    return Result.success(data);
  }

  /** P2 多维度分析：xDim × yDim 聚合 */
  @GetMapping("/api/statistics/multi-dim")
  public Result<List<Map<String, Object>>> getMultiDimAnalysis(
      @RequestParam String xDim,
      @RequestParam String yDim,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo) {
    List<Map<String, Object>> data = statisticsService.getMultiDimAnalysis(xDim, yDim, dateFrom, dateTo);
    return Result.success(data);
  }

  /** P2 热力图数据：功能区 × 时段 → 平均分贝矩阵 */
  @GetMapping("/api/statistics/heatmap")
  public Result<Map<String, Object>> getHeatmap(
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo) {
    Map<String, Object> data = statisticsService.getHeatmap(dateFrom, dateTo);
    return Result.success(data);
  }

  /** P2 雷达图数据：各功能区多维度指标 */
  @GetMapping("/api/statistics/radar")
  public Result<Map<String, Object>> getRadar(
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo) {
    Map<String, Object> data = statisticsService.getRadar(dateFrom, dateTo);
    return Result.success(data);
  }
}
