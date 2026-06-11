package com.example.noise.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时噪声仪表盘 VO — P0-4 仪表盘各功能区最新噪声数据
 */
@Data
public class NoiseLatestVO {

  /** 噪声记录 ID */
  private Long id;

  /** 功能区名称 */
  private String location;

  /** 最新分贝值 */
  private BigDecimal decibel;

  /** 采集时间点 */
  private LocalDateTime timePoint;

  /** 当前阈值 */
  private Integer thresholdValue;

  /** 是否异常：NULL=未判断, 0=正常, 1=异常 */
  private Integer isAbnormal;
}
