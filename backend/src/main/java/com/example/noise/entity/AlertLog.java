package com.example.noise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警记录实体 — 对应 alert_log 表
 * P0-5 异常告警记录与推送
 */
@Data
@TableName("alert_log")
public class AlertLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 关联的噪声记录 ID */
  @TableField("noise_record_id")
  private Long noiseRecordId;

  /** 功能区名称 */
  @TableField("location")
  private String location;

  /** 实际分贝值 */
  @TableField("decibel")
  private BigDecimal decibel;

  /** 触发阈值 */
  @TableField("threshold_value")
  private Integer thresholdValue;

  /** 告警类型：超阈值 / 骤升 / 夜间异常 */
  @TableField("alert_type")
  private String alertType;

  /** 确认状态：未确认 / 已确认 / 已处置 */
  @TableField("confirm_status")
  private String confirmStatus;

  /** 确认人 ID（可空） */
  @TableField("confirmed_by")
  private Long confirmedBy;

  /** 备注 */
  @TableField("remark")
  private String remark;

  /** 乐观锁版本号 */
  @TableField("version")
  private Integer version;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("update_time")
  private LocalDateTime updateTime;
}
