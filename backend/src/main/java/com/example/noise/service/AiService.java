package com.example.noise.service;

import java.util.Map;

/**
 * AI 噪声分类服务接口 — P2-2 AI辅助噪声分类
 */
public interface AiService {

  /**
   * 手动触发分类：扫描所有 noiseType IS NULL 的记录进行规则分类
   *
   * @return {"classifiedCount": N, "skippedCount": N}
   */
  Map<String, Object> classify();

  /**
   * 配置分类参数（P2 占位：enabled 控制是否允许触发，minConfidence 为未来模型预留）
   *
   * @param enabled       是否启用自动分类
   * @param minConfidence 最低置信度 0-1
   */
  void updateConfig(Boolean enabled, Double minConfidence);

  /**
   * 获取当前分类配置
   *
   * @return {"enabled": boolean, "minConfidence": double}
   */
  Map<String, Object> getConfig();
}
