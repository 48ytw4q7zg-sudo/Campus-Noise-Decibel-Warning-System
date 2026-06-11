package com.example.noise.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 报告计划配置请求 DTO — P2-3 定时报告配置入参
 */
@Data
public class ReportConfigRequest {

  /** 报表周期：DAILY / WEEKLY / MONTHLY */
  @NotBlank(message = "报表周期不能为空")
  private String reportPeriod;

  /** 计划生成时间，格式 HH:mm */
  @NotBlank(message = "生成时间不能为空")
  private String generateTime;
}
