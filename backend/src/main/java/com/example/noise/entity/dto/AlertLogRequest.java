package com.example.noise.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 告警创建请求 DTO — P0-5 告警确认/创建
 */
@Data
public class AlertLogRequest {

  /** 关联的噪声记录 ID */
  @NotNull(message = "噪声记录 ID 不能为空")
  private Long noiseRecordId;
}
