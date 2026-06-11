package com.example.noise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.noise.common.BusinessException;
import com.example.noise.entity.AreaConfig;
import com.example.noise.entity.Report;
import com.example.noise.entity.ThresholdRule;
import com.example.noise.mapper.AreaConfigMapper;
import com.example.noise.mapper.NoiseRecordMapper;
import com.example.noise.mapper.ReportMapper;
import com.example.noise.mapper.ThresholdRuleMapper;
import com.example.noise.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 报告生成 Service 实现 — P2-3 定时报告生成
 * 手动生成报告 + 每日 6:00 自动生成前一日报告
 */
@Service
public class ReportServiceImpl implements ReportService {

  private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

  private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /** 四大功能区列表 */
  private static final List<String> AREAS = List.of("图书馆", "食堂", "操场", "宿舍");

  private final ReportMapper reportMapper;
  private final NoiseRecordMapper noiseRecordMapper;
  private final ThresholdRuleMapper thresholdRuleMapper;
  private final AreaConfigMapper areaConfigMapper;

  public ReportServiceImpl(ReportMapper reportMapper,
                           NoiseRecordMapper noiseRecordMapper,
                           ThresholdRuleMapper thresholdRuleMapper,
                           AreaConfigMapper areaConfigMapper) {
    this.reportMapper = reportMapper;
    this.noiseRecordMapper = noiseRecordMapper;
    this.thresholdRuleMapper = thresholdRuleMapper;
    this.areaConfigMapper = areaConfigMapper;
  }

  @Override
  public IPage<Report> queryPage(int pageNum, int pageSize) {
    LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
    wrapper.orderByDesc(Report::getCreateTime);
    return reportMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
  }

  @Override
  public Report getDetail(Long id) {
    Report report = reportMapper.selectById(id);
    if (report == null) {
      throw new BusinessException(8001, "报告不存在");
    }
    return report;
  }

  @Override
  public Report generateReport(String reportPeriod, String periodStart, String periodEnd) {
    // 格式兼容：前端可能传 ISO 8601 (yyyy-MM-ddTHH:mm:ss) 或 MySQL (yyyy-MM-dd HH:mm:ss)
    String startParam = periodStart != null ? periodStart.replace('T', ' ') : null;
    String endParam = periodEnd != null ? periodEnd.replace('T', ' ') : null;

    // 幂等检查：同周期 + 同起止时间不可重复生成
    LambdaQueryWrapper<Report> checkWrapper = new LambdaQueryWrapper<>();
    checkWrapper.eq(Report::getReportPeriod, reportPeriod)
                 .eq(Report::getPeriodStart, LocalDateTime.parse(startParam, DTF))
                 .eq(Report::getPeriodEnd, LocalDateTime.parse(endParam, DTF));
    Long existingCount = reportMapper.selectCount(checkWrapper);
    if (existingCount > 0) {
      throw new BusinessException(8002, "同周期已有报告，不可重复生成");
    }

    // 聚合噪声数据：全局汇总 + 按功能区分组
    Map<String, Object> globalSummary = noiseRecordMapper.selectGlobalSummary(startParam, endParam);
    List<Map<String, Object>> areaStatsList = noiseRecordMapper.selectAreaStats(startParam, endParam);

    // 聚合告警数据
    Long totalAlerts = noiseRecordMapper.selectGlobalAlertCount(startParam, endParam);
    List<Map<String, Object>> alertByArea = noiseRecordMapper.selectAlertCountByArea(startParam, endParam);

    // 阈值配置快照
    String thresholdSnapshot = buildThresholdSnapshot();

    // ========== 生成 Markdown 报告内容 ==========
    StringBuilder content = new StringBuilder();

    content.append("# 校园噪音监测报告\n\n");

    // 1. 报告概要
    content.append("## 报告概要\n\n");
    content.append("- **报告周期**: ").append(reportPeriod).append("\n");
    content.append("- **统计时段**: ").append(periodStart).append(" ~ ").append(periodEnd).append("\n");
    content.append("- **生成时间**: ").append(LocalDateTime.now().format(DTF)).append("\n\n");

    // 全局汇总（null guard：数据库可能无数据）
    long totalRecords = globalSummary != null
        ? ((Number) globalSummary.getOrDefault("totalRecords", 0)).longValue() : 0L;
    BigDecimal avgDecibel = globalSummary != null && globalSummary.get("avgDecibel") != null
        ? new BigDecimal(globalSummary.get("avgDecibel").toString()) : BigDecimal.ZERO;

    content.append("| 指标 | 数值 |\n");
    content.append("|------|------|\n");
    content.append("| 总记录数 | ").append(totalRecords).append(" |\n");
    content.append("| 平均分贝值 | ").append(String.format("%.1f", avgDecibel)).append(" dB(A) |\n");
    content.append("| 总告警数 | ").append(totalAlerts != null ? totalAlerts : 0).append(" |\n\n");

    // 2. 各功能区统计表
    content.append("## 各功能区统计\n\n");
    content.append("| 功能区 | 记录数 | 平均分贝 | 异常数 | 告警数 |\n");
    content.append("|--------|--------|----------|--------|--------|\n");

    for (String area : AREAS) {
      long areaTotal = 0;
      double areaAvg = 0;
      long areaAbnormal = 0;
      long areaAlert = 0;

      // 从噪声统计中取
      for (Map<String, Object> stat : areaStatsList) {
        if (area.equals(stat.get("location"))) {
          areaTotal = ((Number) stat.getOrDefault("totalRecords", 0)).longValue();
          Object dbObj = stat.get("avgDecibel");
          areaAvg = dbObj != null ? new BigDecimal(dbObj.toString()).doubleValue() : 0.0;
          areaAbnormal = ((Number) stat.getOrDefault("abnormalCount", 0)).longValue();
          break;
        }
      }

      // 从告警统计中取
      for (Map<String, Object> alertStat : alertByArea) {
        if (area.equals(alertStat.get("location"))) {
          areaAlert = ((Number) alertStat.getOrDefault("alertCount", 0)).longValue();
          break;
        }
      }

      content.append("| ").append(area)
             .append(" | ").append(areaTotal)
             .append(" | ").append(String.format("%.1f", areaAvg))
             .append(" | ").append(areaAbnormal)
             .append(" | ").append(areaAlert)
             .append(" |\n");
    }

    content.append("\n");

    // 3. 告警汇总
    content.append("## 告警汇总\n\n");
    content.append("- **周期内告警总数**: ").append(totalAlerts != null ? totalAlerts : 0).append("\n\n");
    if (alertByArea != null && !alertByArea.isEmpty()) {
      content.append("| 功能区 | 告警次数 |\n");
      content.append("|--------|----------|\n");
      for (Map<String, Object> al : alertByArea) {
        content.append("| ").append(al.get("location"))
               .append(" | ").append(al.get("alertCount"))
               .append(" |\n");
      }
      content.append("\n");
    } else {
      content.append("该周期内无告警记录。\n\n");
    }

    // 4. 阈值配置快照
    content.append("## 阈值配置快照\n\n");
    content.append(thresholdSnapshot);

    // 存入 report 表
    Report report = new Report();
    report.setReportPeriod(reportPeriod);
    report.setPeriodStart(LocalDateTime.parse(startParam, DTF));
    report.setPeriodEnd(LocalDateTime.parse(endParam, DTF));
    report.setContent(content.toString());
    report.setStatus("已生成");
    reportMapper.insert(report);

    return report;
  }

  @Override
  public void scheduleConfig(String reportPeriod, String generateTime) {
    // P2 占位：记录配置入参即可，暂无动态 scheduler 管理
    log.info("报告定时配置已记录: reportPeriod={}, generateTime={}", reportPeriod, generateTime);
  }

  @Override
  @Scheduled(cron = "0 0 6 * * ?")
  public void autoGenerateDailyReport() {
    try {
      // 生成前一天 (00:00:00 ~ 23:59:59) 的 DAILY 报告
      LocalDate yesterday = LocalDate.now().minusDays(1);
      String startTime = yesterday.atStartOfDay().format(DTF);
      String endTime = yesterday.atTime(LocalTime.MAX).withNano(0).format(DTF);

      log.info("开始定时生成每日报告: {} ~ {}", startTime, endTime);

      // 幂等：如果已存在则跳过
      LambdaQueryWrapper<Report> checkWrapper = new LambdaQueryWrapper<>();
      checkWrapper.eq(Report::getReportPeriod, "DAILY")
                   .eq(Report::getPeriodStart, LocalDateTime.parse(startTime, DTF))
                   .eq(Report::getPeriodEnd, LocalDateTime.parse(endTime, DTF));
      if (reportMapper.selectCount(checkWrapper) > 0) {
        log.info("每日报告已存在，跳过: {}", yesterday.format(DATE_FMT));
        return;
      }

      Report report = generateReport("DAILY", startTime, endTime);
      log.info("每日报告生成成功: id={}, 日期={}", report.getId(), yesterday.format(DATE_FMT));
    } catch (BusinessException e) {
      // 幂等冲突（已有报告）不报错，仅记录日志
      log.warn("每日报告生成跳过（{}）: code={}", e.getMessage(), e.getCode());
    } catch (Exception e) {
      log.error("每日报告生成失败", e);
    }
  }

  // ==================== 私有方法 ====================

  /**
   * 构建阈值配置快照（Markdown 格式）
   * 包含：所有启用的阈值规则 + 各功能区默认阈值
   */
  private String buildThresholdSnapshot() {
    StringBuilder sb = new StringBuilder();

    // 启用阈值规则
    LambdaQueryWrapper<ThresholdRule> ruleWrapper = new LambdaQueryWrapper<>();
    ruleWrapper.eq(ThresholdRule::getStatus, 1)
               .orderByAsc(ThresholdRule::getLocation)
               .orderByAsc(ThresholdRule::getTimeSegment);
    List<ThresholdRule> rules = thresholdRuleMapper.selectList(ruleWrapper);

    sb.append("### 当前阈值规则（启用）\n\n");
    if (rules.isEmpty()) {
      sb.append("暂无启用的阈值规则。\n\n");
    } else {
      sb.append("| 功能区 | 时段 | 阈值(dB) | 规则描述 |\n");
      sb.append("|--------|------|----------|----------|\n");
      for (ThresholdRule rule : rules) {
        sb.append("| ").append(rule.getLocation())
          .append(" | ").append(nullToEmpty(rule.getTimeSegment()))
          .append(" | ").append(rule.getThresholdValue())
          .append(" | ").append(nullToEmpty(rule.getDescription()))
          .append(" |\n");
      }
      sb.append("\n");
    }

    // 各功能区默认阈值
    List<AreaConfig> areas = areaConfigMapper.selectList(new LambdaQueryWrapper<>());
    sb.append("### 功能区默认阈值\n\n");
    if (areas.isEmpty()) {
      sb.append("暂无功能区配置。\n\n");
    } else {
      sb.append("| 功能区 | 敏感度 | 默认阈值(dB) | 状态 |\n");
      sb.append("|--------|--------|-------------|------|\n");
      for (AreaConfig area : areas) {
        String sensLabel = switch (area.getNoiseSensitivity()) {
          case 1 -> "高";
          case 2 -> "中";
          case 3 -> "低";
          default -> "未知";
        };
        String statusLabel = area.getStatus() != null && area.getStatus() == 1 ? "启用" : "禁用";
        sb.append("| ").append(area.getAreaName())
          .append(" | ").append(sensLabel)
          .append(" | ").append(area.getDefaultThreshold() != null ? area.getDefaultThreshold() : "-")
          .append(" | ").append(statusLabel)
          .append(" |\n");
      }
      sb.append("\n");
    }

    return sb.toString();
  }

  /** null → 空字符串 */
  private String nullToEmpty(String s) {
    return s != null ? s : "";
  }
}
