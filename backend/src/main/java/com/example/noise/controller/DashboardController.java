package com.example.noise.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.noise.common.Result;
import com.example.noise.entity.NoiseRecord;
import com.example.noise.entity.dto.NoiseLatestVO;
import com.example.noise.service.NoiseRecordService;
import com.example.noise.service.ThresholdService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DashboardController {

  private final NoiseRecordService noiseRecordService;
  private final ThresholdService thresholdService;

  public DashboardController(NoiseRecordService noiseRecordService,
                              ThresholdService thresholdService) {
    this.noiseRecordService = noiseRecordService;
    this.thresholdService = thresholdService;
  }

  /** 四大功能区实时状态概览（最新分贝 + 异常状态 + 当前阈值 + 指示灯） */
  @GetMapping("/api/dashboard/overview")
  public Result<List<Map<String, Object>>> overview() {
    List<NoiseLatestVO> latestList = noiseRecordService.getLatestPerArea();
    List<Map<String, Object>> result = new ArrayList<>();

    for (NoiseLatestVO item : latestList) {
      if (item == null) {
        continue; // 该功能区无数据，跳过
      }

      Map<String, Object> card = new HashMap<>();
      card.put("location", item.getLocation());
      card.put("decibel", item.getDecibel());
      card.put("thresholdValue", item.getThresholdValue());
      card.put("isAbnormal", item.getIsAbnormal());
      card.put("lastUpdateTime", item.getTimePoint() != null
          ? item.getTimePoint().toString() : null);

      // 根据 isAbnormal 计算指示灯状态
      String indicator;
      if (item.getIsAbnormal() == null) {
        indicator = "unknown";
      } else if (item.getIsAbnormal() == 1) {
        indicator = "abnormal";
      } else {
        indicator = "normal";
      }
      card.put("indicator", indicator);

      result.add(card);
    }

    return Result.success(result);
  }

  /** 某功能区详情（摘要 + 最近N条记录 + 当日统计） */
  @GetMapping("/api/dashboard/areas/{location}")
  public Result<Map<String, Object>> areaDetail(@PathVariable String location) {
    Map<String, Object> data = new HashMap<>();
    data.put("location", location);

    // 从最新列表中获取该功能区的摘要信息
    List<NoiseLatestVO> latestList = noiseRecordService.getLatestPerArea();
    for (NoiseLatestVO item : latestList) {
      if (item != null && location.equals(item.getLocation())) {
        data.put("latestDecibel", item.getDecibel());
        data.put("thresholdValue", item.getThresholdValue());
        data.put("isAbnormal", item.getIsAbnormal());

        String indicator;
        if (item.getIsAbnormal() == null) {
          indicator = "unknown";
        } else if (item.getIsAbnormal() == 1) {
          indicator = "abnormal";
        } else {
          indicator = "normal";
        }
        data.put("indicator", indicator);
        break;
      }
    }

    // 查询最近 10 条记录
    IPage<NoiseRecord> recentPage = new Page<>(1, 10);
    IPage<NoiseRecord> recentResult = noiseRecordService.queryPage(
        recentPage, location, null, null, null, null, null, "time_point", "desc");

    List<Map<String, Object>> recentRecords = new ArrayList<>();
    for (NoiseRecord record : recentResult.getRecords()) {
      Map<String, Object> rec = new HashMap<>();
      rec.put("timePoint", record.getTimePoint() != null ? record.getTimePoint().toString() : null);
      rec.put("decibel", record.getDecibel());
      recentRecords.add(rec);
    }
    data.put("recentRecords", recentRecords);

    // 当日统计
    String todayStart = LocalDate.now().atStartOfDay().toString();
    IPage<NoiseRecord> todayPage = new Page<>(1, 10000);
    IPage<NoiseRecord> todayResult = noiseRecordService.queryPage(
        todayPage, location, todayStart, null, null, null, null, "time_point", "asc");

    Map<String, Object> todayStats = computeTodayStats(todayResult.getRecords());
    data.put("todayStats", todayStats);

    return Result.success(data);
  }

  /** 计算当日统计摘要 */
  private Map<String, Object> computeTodayStats(List<NoiseRecord> records) {
    Map<String, Object> stats = new HashMap<>();
    if (records == null || records.isEmpty()) {
      stats.put("avg", 0);
      stats.put("max", 0);
      stats.put("min", 0);
      stats.put("abnormalCount", 0);
      return stats;
    }

    double sum = 0;
    double max = Double.MIN_VALUE;
    double min = Double.MAX_VALUE;
    int abnormalCount = 0;

    for (NoiseRecord r : records) {
      double db = r.getDecibel() != null ? r.getDecibel().doubleValue() : 0;
      sum += db;
      if (db > max) max = db;
      if (db < min) min = db;
      if (r.getIsAbnormal() != null && r.getIsAbnormal() == 1) {
        abnormalCount++;
      }
    }

    stats.put("avg", Math.round(sum / records.size() * 10.0) / 10.0);
    stats.put("max", max);
    stats.put("min", min);
    stats.put("abnormalCount", abnormalCount);
    return stats;
  }
}
