package com.example.noise.service;

import com.example.noise.entity.NoiseRecord;
import com.example.noise.entity.ThresholdRule;

import java.util.List;
import java.util.Map;

/**
 * 动态阈值判断 Service 接口 — P0-3 + P1-4 阈值规则 CRUD
 */
public interface ThresholdService {

  /**
   * 根据当前时间确定时段标签 → 查threshold_rule表(只查status=1) →
   * 没找到查area_config.default_threshold → 都没有返回全局默认55dB(A)
   *
   * @param location 功能区名称
   * @return 当前时段适用的阈值规则（含兜底）
   */
  ThresholdRule getCurrentThreshold(String location);

  /**
   * 对指定噪声记录执行阈值判断：查记录→查当前阈值→比较→标记is_abnormal+judgedByModel
   * 如果is_abnormal已非NULL抛BusinessException(2004, "该记录已判断")
   * 如果超阈值则调用AlertLogService创建告警
   *
   * @param noiseRecordId 噪声记录ID
   * @return {noiseRecordId, decibel, thresholdValue, isAbnormal, judgedByModel, alertCreated}
   */
  Map<String, Object> checkRecord(Long noiseRecordId);

  // ==================== P1-4 阈值规则管理 ====================

  /**
   * 查询阈值规则列表，可按功能区名称筛选
   * 只返回 status=1 的启用规则
   *
   * @param location 功能区名称（可选，为null时返回全部）
   * @return 阈值规则列表
   */
  List<ThresholdRule> listRules(String location);

  /**
   * 新增阈值规则
   * 校验 location∈4大功能区 + thresholdValue 0-120 → 检查唯一索引(location+timeSegment) → 重复抛BusinessException(3001) → insert
   *
   * @param location       功能区名称（图书馆/食堂/操场/宿舍）
   * @param timeSegment    时段标签
   * @param thresholdValue 阈值分贝值（0-120）
   * @param description    规则描述
   * @return 新增的规则实体
   */
  ThresholdRule createRule(String location, String timeSegment, Integer thresholdValue, String description);

  /**
   * 修改阈值规则（乐观锁）
   * selectById查存在(抛2003) → 乐观锁version校验(抛3002) → 更新thresholdValue/description/status → updateById
   *
   * @param id             规则ID
   * @param thresholdValue 阈值分贝值（可选）
   * @param description    规则描述（可选）
   * @param status         状态 1=启用 0=禁用（可选）
   * @param version        乐观锁版本号
   */
  void updateRule(Long id, Integer thresholdValue, String description, Integer status, Integer version);

  /**
   * 删除阈值规则
   * selectById查存在(抛2003) → deleteById
   *
   * @param id 规则ID
   */
  void deleteRule(Long id);

  /**
   * 通知 cccswitch 热更新阈值规则
   * P1 阶段：直接返回 {reloadTime, ruleCount}，P2 再对接真实 cccswitch HTTP POST
   *
   * @return {reloadTime, ruleCount}
   */
  Map<String, Object> reloadRules();

  // ==================== P1-1 统计自适应阈值 ====================

  /**
   * 查询某功能区当前统计自适应阈值
   * 滑动窗口计算均值μ和标准差σ → 阈值=μ±k×σ
   * 窗口内数据不足时回退到业务规则阈值
   *
   * @param location 功能区名称
   * @return {location, windowSize, kValue, mean, stdDev, upperLimit, lowerLimit, windowRecordCount}
   */
  Map<String, Object> getAdaptiveThreshold(String location);

  /**
   * 配置自适应参数（管理员）
   * 批量更新各功能区的 windowSize 和 kValue
   *
   * @param areaConfigs [{location, windowSize, kValue}, ...]
   */
  void updateAdaptiveConfig(List<Map<String, Object>> areaConfigs);

  // ==================== P1-2 混合阈值模型 ====================

  /**
   * 查询混合模型运行状态
   * 遍历4个功能区，检查3个触发条件
   *
   * @return {currentMode, abnormalRate3Windows, isTriggered, triggerReason}
   */
  Map<String, Object> getHybridStatus();

  /**
   * 查询混合模型性能指标
   * 返回研究报告 §4 的实验数据（固定值）
   *
   * @return {accuracy, precision, recall, f1Score, falsePositiveRate, modeDistribution}
   */
  Map<String, Object> getHybridPerformance();

  // ==================== P1-2 混合模型自动判断 ====================

  /**
   * 使用混合阈值模型对新入库的噪声记录自动判断（P1-2）
   * P1阶段：优先用统计自适应阈值，触发条件满足时用业务规则阈值
   * 计算 isAbnormal + judgedByModel + 超阈值则创建告警
   *
   * @param record 噪声记录（已入库，isAbnormal 为 null）
   */
  void autoJudgeWithHybrid(NoiseRecord record);
}
