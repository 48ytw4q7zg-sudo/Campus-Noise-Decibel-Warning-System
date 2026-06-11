package com.example.noise.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.noise.entity.AlertLog;

import java.math.BigDecimal;

/**
 * 告警记录 Service 接口 — P0-5
 */
public interface AlertLogService {

  /**
   * 创建告警记录
   *
   * @param noiseRecordId 关联的噪声记录ID
   * @param location 功能区名称
   * @param decibel 实际分贝值
   * @param thresholdValue 触发阈值
   * @param alertType 告警类型（ABOVE_THRESHOLD / SUSTAINED / SPIKE）
   */
  void createAlert(Long noiseRecordId, String location, BigDecimal decibel,
                   Integer thresholdValue, String alertType);

  /**
   * 分页查询告警列表，支持按功能区/时间范围筛选，默认按创建时间倒序
   *
   * @param page MyBatis-Plus 分页对象
   * @param location 功能区筛选（可选）
   * @param dateFrom 起始时间（可选）
   * @param dateTo 结束时间（可选）
   * @return 分页结果
   */
  IPage<AlertLog> queryPage(IPage<AlertLog> page,
                            String location, String dateFrom, String dateTo);

  /**
   * 查询告警详情，含关联的噪声记录信息
   *
   * @param id 告警ID
   * @return 告警实体
   */
  AlertLog getDetail(Long id);

  /**
   * 确认告警（管理员操作），仅 UNCONFIRMED 状态可确认
   *
   * @param id 告警ID
   * @param version 客户端传入的版本号（乐观锁）
   */
  void confirmAlert(Long id, Integer version);

  /**
   * 处置告警（管理员操作），仅 CONFIRMED 状态可处置，支持填写备注
   *
   * @param id 告警ID
   * @param version 客户端传入的版本号（乐观锁）
   * @param remark 处置备注
   */
  void resolveAlert(Long id, Integer version, String remark);
}
