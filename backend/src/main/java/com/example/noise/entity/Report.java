package com.example.noise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表实体 — 对应 report 表
 * P2 数据导入导出
 */
@Data
@TableName("report")
public class Report {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 报表周期：DAILY / WEEKLY / MONTHLY */
  @TableField("report_period")
  private String reportPeriod;

  /** 统计起始时间 */
  @TableField("period_start")
  private LocalDateTime periodStart;

  /** 统计结束时间 */
  @TableField("period_end")
  private LocalDateTime periodEnd;

  /** 报表内容 TEXT */
  @TableField("content")
  private String content;

  /** 状态：DRAFT / GENERATED / ARCHIVED */
  @TableField("status")
  private String status;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("update_time")
  private LocalDateTime updateTime;
}
