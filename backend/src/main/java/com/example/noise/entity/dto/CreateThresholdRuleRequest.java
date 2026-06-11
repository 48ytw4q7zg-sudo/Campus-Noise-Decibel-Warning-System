package com.example.noise.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建阈值规则请求 DTO — P1-4
 */
@Data
public class CreateThresholdRuleRequest {

  @NotBlank(message = "功能区不能为空")
  @Size(max = 10, message = "功能区名称最长10字符")
  private String location;

  @NotBlank(message = "时段不能为空")
  @Size(max = 20, message = "时段标签最长20字符")
  private String timeSegment;

  @NotNull(message = "阈值不能为空")
  @Min(value = 0, message = "阈值不能小于0")
  @Max(value = 120, message = "阈值不能大于120")
  private Integer thresholdValue;

  @Size(max = 200, message = "描述最长200字符")
  private String description;
}
