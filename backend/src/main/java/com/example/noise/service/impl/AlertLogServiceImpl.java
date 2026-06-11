package com.example.noise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.noise.common.BusinessException;
import com.example.noise.entity.AlertLog;
import com.example.noise.entity.NoiseRecord;
import com.example.noise.mapper.AlertLogMapper;
import com.example.noise.mapper.NoiseRecordMapper;
import com.example.noise.service.AlertLogService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 告警记录 Service 实现 — P0-5
 * 创建告警 / 分页查询 / 详情 / 确认 / 处置
 */
@Service
public class AlertLogServiceImpl implements AlertLogService {

  private final AlertLogMapper alertLogMapper;
  private final NoiseRecordMapper noiseRecordMapper;

  public AlertLogServiceImpl(AlertLogMapper alertLogMapper,
                             NoiseRecordMapper noiseRecordMapper) {
    this.alertLogMapper = alertLogMapper;
    this.noiseRecordMapper = noiseRecordMapper;
  }

  @Override
  public void createAlert(Long noiseRecordId, String location, BigDecimal decibel,
                          Integer thresholdValue, String alertType) {
    AlertLog alert = new AlertLog();
    alert.setNoiseRecordId(noiseRecordId);
    alert.setLocation(location);
    alert.setDecibel(decibel);
    alert.setThresholdValue(thresholdValue);
    alert.setAlertType(alertType);
    alert.setConfirmStatus("未确认");
    alertLogMapper.insert(alert);
  }

  @Override
  public IPage<AlertLog> queryPage(IPage<AlertLog> page,
                                   String location, String dateFrom, String dateTo) {
    LambdaQueryWrapper<AlertLog> wrapper = new LambdaQueryWrapper<>();

    // 可选筛选：功能区
    if (location != null && !location.isBlank()) {
      wrapper.eq(AlertLog::getLocation, location);
    }
    // 可选筛选：时间范围
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    if (dateFrom != null && !dateFrom.isBlank()) {
      wrapper.ge(AlertLog::getCreateTime, LocalDateTime.parse(dateFrom, fmt));
    }
    if (dateTo != null && !dateTo.isBlank()) {
      wrapper.le(AlertLog::getCreateTime, LocalDateTime.parse(dateTo, fmt));
    }

    // 默认按创建时间倒序
    wrapper.orderByDesc(AlertLog::getCreateTime);

    return alertLogMapper.selectPage(page, wrapper);
  }

  @Override
  public AlertLog getDetail(Long id) {
    AlertLog alert = alertLogMapper.selectById(id);
    if (alert == null) {
      throw new BusinessException(4001, "告警记录不存在");
    }
    return alert;
  }

  @Override
  public void confirmAlert(Long id, Integer version) {
    AlertLog alert = alertLogMapper.selectById(id);
    if (alert == null) {
      throw new BusinessException(4001, "告警记录不存在");
    }
    if (!"未确认".equals(alert.getConfirmStatus())) {
      throw new BusinessException(4002, "该告警已确认，无需重复操作");
    }

    // 乐观锁检查
    if (!alert.getVersion().equals(version)) {
      throw new BusinessException(4003, "数据已被他人修改，请刷新后重试");
    }

    alert.setConfirmStatus("已确认");
    alert.setVersion(alert.getVersion() + 1);
    alertLogMapper.updateById(alert);
  }

  @Override
  public void resolveAlert(Long id, Integer version, String remark) {
    AlertLog alert = alertLogMapper.selectById(id);
    if (alert == null) {
      throw new BusinessException(4001, "告警记录不存在");
    }
    if (!"已确认".equals(alert.getConfirmStatus())) {
      throw new BusinessException(4002, "该告警未被确认，无法处置（请先确认）");
    }

    // 乐观锁检查
    if (!alert.getVersion().equals(version)) {
      throw new BusinessException(4003, "数据已被他人修改，请刷新后重试");
    }

    alert.setConfirmStatus("已处置");
    alert.setRemark(remark);
    alert.setVersion(alert.getVersion() + 1);
    alertLogMapper.updateById(alert);
  }
}
