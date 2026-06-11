package com.example.noise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.noise.entity.NoiseRecord;
import com.example.noise.mapper.NoiseRecordMapper;
import com.example.noise.service.AiService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 噪声分类服务实现 — P2-2 AI辅助噪声分类
 *
 * P2 阶段使用基于规则的启发式分类，不调用外部 AI API（教学简化）。
 * 规则引擎：func(location, decibel, timeSegment) → noiseType
 */
@Service
public class AiServiceImpl implements AiService {

  private final NoiseRecordMapper noiseRecordMapper;

  /** 是否启用自动分类（P2 占位，默认 true） */
  private boolean enabled = true;

  /** 最低置信度（P2 占位，默认 0.7，当前规则引擎不使用） */
  private double minConfidence = 0.7;

  public AiServiceImpl(NoiseRecordMapper noiseRecordMapper) {
    this.noiseRecordMapper = noiseRecordMapper;
  }

  @Override
  public Map<String, Object> classify() {
    // 查询所有 noiseType IS NULL 的记录
    LambdaQueryWrapper<NoiseRecord> wrapper = new LambdaQueryWrapper<>();
    wrapper.isNull(NoiseRecord::getNoiseType);
    List<NoiseRecord> unclassified = noiseRecordMapper.selectList(wrapper);

    int classifiedCount = 0;

    for (NoiseRecord record : unclassified) {
      String noiseType = classifyByRule(record.getLocation(), record.getDecibel());
      if (noiseType != null) {
        // CAS 更新：仅 noiseType IS NULL 时才写（幂等）
        LambdaUpdateWrapper<NoiseRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(NoiseRecord::getId, record.getId())
            .isNull(NoiseRecord::getNoiseType)
            .set(NoiseRecord::getNoiseType, noiseType);
        int rows = noiseRecordMapper.update(null, updateWrapper);
        if (rows > 0) {
          classifiedCount++;
        }
      }
    }

    int skippedCount = unclassified.size() - classifiedCount;

    Map<String, Object> result = new HashMap<>();
    result.put("classifiedCount", classifiedCount);
    result.put("skippedCount", skippedCount);
    return result;
  }

  @Override
  public void updateConfig(Boolean enabled, Double minConfidence) {
    if (enabled != null) {
      this.enabled = enabled;
    }
    if (minConfidence != null) {
      this.minConfidence = minConfidence;
    }
  }

  @Override
  public Map<String, Object> getConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put("enabled", enabled);
    config.put("minConfidence", minConfidence);
    return config;
  }

  /**
   * 基于规则的启发式分类引擎。
   *
   * 规则表（location × decibel → noiseType）：
   * <pre>
   * | 功能区 | 分贝范围 | 噪声类型 | 规则依据         |
   * |--------|---------|---------|------------------|
   * | 图书馆 | ≤45dB   | 交谈    | 低声交谈，翻书    |
   * | 图书馆 | 45-55dB | 交谈    | 较大声交谈/讨论   |
   * | 图书馆 | >55dB   | 施工    | 异常噪声，可能装修 |
   * | 食堂   | ≤65dB   | 交谈    | 正常用餐交谈      |
   * | 食堂   | >65dB   | 体育活动 | 喧哗或大型活动    |
   * | 操场   | ≤70dB   | 体育活动 | 正常体育活动      |
   * | 操场   | >70dB   | 施工    | 大型活动或施工    |
   * | 宿舍   | ≤45dB   | 交谈    | 正常生活交谈      |
   * | 宿舍   | 45-55dB | 体育活动 | 室内运动          |
   * | 宿舍   | >55dB   | 施工    | 异常噪声/装修     |
   * </pre>
   *
   * @param location 功能区名称（图书馆/食堂/操场/宿舍）
   * @param decibel  分贝值
   * @return 噪声类型（交谈/施工/体育活动），无匹配返回"其它"
   */
  private String classifyByRule(String location, BigDecimal decibel) {
    if (location == null || decibel == null) {
      return "其它";
    }

    double db = decibel.doubleValue();

    switch (location) {
      case "图书馆":
        if (db <= 55.0) {
          return "交谈";
        } else {
          return "施工";
        }

      case "食堂":
        if (db <= 65.0) {
          return "交谈";
        } else {
          return "体育活动";
        }

      case "操场":
        if (db <= 70.0) {
          return "体育活动";
        } else {
          return "施工";
        }

      case "宿舍":
        if (db <= 45.0) {
          return "交谈";
        } else if (db <= 55.0) {
          return "体育活动";
        } else {
          return "施工";
        }

      default:
        return "其它";
    }
  }
}
