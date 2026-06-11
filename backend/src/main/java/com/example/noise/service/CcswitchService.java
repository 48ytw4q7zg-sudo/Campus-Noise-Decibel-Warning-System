package com.example.noise.service;

import java.util.Map;

/**
 * ccswitch 配置服务集成接口 — P2
 * 通过 HTTP 调用 Flask ccswitch 服务（默认 http://localhost:5000），
 * 提供健康检查、配置重载、阈值规则查询与重载能力。
 */
public interface CcswitchService {

  /** 查询 ccswitch 健康状态（GET /api/health） */
  Map<String, Object> getStatus();

  /** 触发 ccswitch 配置重载（POST /api/config/reload） */
  Map<String, Object> reloadConfig();

  /** 获取阈值规则列表（GET /api/threshold-rules） */
  Map<String, Object> getThresholdRules();

  /** 通知 ccswitch 重载阈值规则（POST /api/threshold-rules/reload） */
  Map<String, Object> reloadThresholdRules();

  /** POST /api/threshold/compute — 调用 ccswitch 实时计算阈值 */
  Map<String, Object> computeThreshold(Map<String, Object> request);

  /** POST /api/threshold/batch-compute — 批量阈值计算 */
  Map<String, Object> batchComputeThreshold(Map<String, Object> request);

  /** PUT /api/threshold/area-config — 更新区域自适应参数 */
  Map<String, Object> updateAreaConfig(Map<String, Object> config);

  /** GET /api/threshold/area-config — 查询区域自适应参数 */
  Map<String, Object> getAreaConfig();
}
