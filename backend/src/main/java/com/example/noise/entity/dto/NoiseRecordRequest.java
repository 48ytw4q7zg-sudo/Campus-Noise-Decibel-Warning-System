package com.example.noise.entity.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 噪声记录请求 DTO — P0-2 噪声数据采集入参
 */
@Data
public class NoiseRecordRequest {

  /** 功能区名称 */
  @NotBlank(message = "功能区不能为空")
  private String location;

  /** 分贝值，范围 20.0 ~ 120.0 */
  @NotNull(message = "分贝值不能为空")
  @DecimalMin(value = "20.0", message = "分贝值不能小于 20.0")
  @DecimalMax(value = "120.0", message = "分贝值不能大于 120.0")
  private Double decibel;

  /** 采集时间点（可空，为空取服务器当前时间） */
  private LocalDateTime timePoint;

  /** 采集设备 ID（可空） */
  private String deviceId;
}
