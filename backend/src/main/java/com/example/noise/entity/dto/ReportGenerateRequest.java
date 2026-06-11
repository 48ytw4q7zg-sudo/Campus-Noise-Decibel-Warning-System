package com.example.noise.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 报告生成请求 DTO — P2-3 手动生成报告入参
 */
@Data
public class ReportGenerateRequest {

  /** 报表周期：DAILY / WEEKLY / MONTHLY */
  @NotBlank(message = "报表周期不能为空")
  private String reportPeriod;

  /** 统计起始时间，格式 yyyy-MM-dd HH:mm:ss */
  @NotBlank(message = "起始时间不能为空")
  private String periodStart;

  /** 统计结束时间，格式 yyyy-MM-dd HH:mm:ss */
  @NotBlank(message = "结束时间不能为空")
  private String periodEnd;
}
