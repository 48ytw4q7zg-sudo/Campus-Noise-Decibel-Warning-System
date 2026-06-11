package com.example.noise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 阈值规则实体 — 对应 threshold_rule 表
 * P0-3 业务规则动态阈值判断
 */
@Data
@TableName("threshold_rule")
public class ThresholdRule {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 功能区名称 */
  @TableField("location")
  private String location;

  /** 时段：A时段/B时段/C时段/D时段 */
  @TableField("time_segment")
  private String timeSegment;

  /** 阈值分贝值 */
  @TableField("threshold_value")
  private Integer thresholdValue;

  /** 规则描述 */
  @TableField("description")
  private String description;

  /** 状态：1=启用 0=禁用 */
  @TableField("status")
  private Integer status;

  /** 乐观锁版本号 */
  @TableField("version")
  private Integer version;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("update_time")
  private LocalDateTime updateTime;
}
