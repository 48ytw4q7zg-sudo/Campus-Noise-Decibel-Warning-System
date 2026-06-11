package com.example.noise.entity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量自适应参数配置请求 DTO — P1-1
 */
@Data
public class UpdateAdaptiveConfigRequest {

  @NotEmpty(message = "配置列表不能为空")
  @Valid
  private List<AreaAdaptiveConfig> areaConfigs;

  @Data
  public static class AreaAdaptiveConfig {
    @NotBlank(message = "功能区不能为空")
    private String location;

    @NotNull(message = "窗口大小不能为空")
    @Min(value = 5, message = "窗口大小最小为5")
    @Max(value = 100, message = "窗口大小最大为100")
    private Integer windowSize;

    @NotNull(message = "k值不能为空")
    @DecimalMin(value = "1.0", message = "k值最小为1.0")
    @DecimalMax(value = "5.0", message = "k值最大为5.0")
    private BigDecimal kValue;
  }
}
