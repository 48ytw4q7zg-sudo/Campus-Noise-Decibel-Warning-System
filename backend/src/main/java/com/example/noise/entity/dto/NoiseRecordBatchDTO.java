package com.example.noise.entity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 噪声批量采集请求 DTO — P0-2 批量导入
 */
@Data
public class NoiseRecordBatchDTO {

  /** 批量记录列表，至少一条 */
  @NotEmpty(message = "批量记录不能为空")
  @Valid
  private List<NoiseRecordRequest> records;
}
