package com.example.noise.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新阈值规则请求 DTO — P1-4
 */
@Data
public class UpdateThresholdRuleRequest {

  @Min(value = 0, message = "阈值不能小于0")
  @Max(value = 120, message = "阈值不能大于120")
  private Integer thresholdValue;

  @Size(max = 200, message = "描述最长200字符")
  private String description;

  @Min(value = 0, message = "状态值无效")
  @Max(value = 1, message = "状态值无效")
  private Integer status;

  @NotNull(message = "版本号不能为空")
  @Min(value = 0, message = "版本号无效")
  private Integer version;
}
