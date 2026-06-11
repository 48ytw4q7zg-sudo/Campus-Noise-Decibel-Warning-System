package com.example.noise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 噪声记录实体 — 对应 noise_record 表
 * P0-2 噪声数据采集与存储
 */
@Data
@TableName("noise_record")
public class NoiseRecord {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 功能区名称 */
  @TableField("location")
  private String location;

  /** 分贝值 DECIMAL(5,1) */
  @TableField("decibel")
  private BigDecimal decibel;

  /** 采集时间点 */
  @TableField("time_point")
  private LocalDateTime timePoint;

  /** 采集设备 ID */
  @TableField("device_id")
  private String deviceId;

  /** 是否异常：NULL=未判断, 0=正常, 1=异常 */
  @TableField("is_abnormal")
  private Integer isAbnormal;

  /** 判断模型：RULE_BASED / STATISTICAL / HYBRID */
  @TableField("judged_by_model")
  private String judgedByModel;

  /** 噪声类型（P2 AI 分类） */
  @TableField("noise_type")
  private String noiseType;

  /** 噪声持续时长 秒（P2） */
  @TableField("noise_duration")
  private Integer noiseDuration;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("update_time")
  private LocalDateTime updateTime;
}
