package com.example.noise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 功能区配置实体 — 对应 area_config 表
 * P0-6 功能区配置管理
 */
@Data
@TableName("area_config")
public class AreaConfig {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 功能区名称，唯一索引 uq_area_name */
  @TableField("area_name")
  private String areaName;

  /** 噪声敏感度：1=高 2=中 3=低 */
  @TableField("noise_sensitivity")
  private Integer noiseSensitivity;

  /** 默认阈值 分贝 */
  @TableField("default_threshold")
  private Integer defaultThreshold;

  /** 功能区描述 */
  @TableField("description")
  private String description;

  /** 状态：1=启用 0=禁用 */
  @TableField("status")
  private Integer status;

  /** 滑动窗口大小（P1-1 统计自适应阈值） */
  @TableField("window_size")
  private Integer windowSize;

  /** K 值倍数 DECIMAL(3,2)（P1-1） */
  @TableField("k_value")
  private BigDecimal kValue;

  /** 乐观锁版本号 */
  @TableField("version")
  private Integer version;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("update_time")
  private LocalDateTime updateTime;
}
