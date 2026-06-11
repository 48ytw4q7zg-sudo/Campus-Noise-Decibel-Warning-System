package com.example.noise.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.noise.common.BusinessException;
import com.example.noise.common.Result;
import com.example.noise.entity.Report;
import com.example.noise.entity.dto.ReportConfigRequest;
import com.example.noise.entity.dto.ReportGenerateRequest;
import com.example.noise.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 报告 Controller — P2-3 定时报告生成
 * 对齐 API_DESIGN.md §3.9 报告管理
 */
@RestController
public class ReportController {

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  /** 报告分页列表 */
  @GetMapping("/api/reports")
  public Result<Map<String, Object>> queryPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "20") Integer pageSize) {
    IPage<Report> result = reportService.queryPage(pageNum, pageSize);

    Map<String, Object> data = new HashMap<>();
    data.put("records", result.getRecords());
    data.put("total", result.getTotal());
    data.put("pageNum", result.getCurrent());
    data.put("pageSize", result.getSize());
    data.put("pages", result.getPages());
    return Result.success(data);
  }

  /** 报告详情 */
  @GetMapping("/api/reports/{id}")
  public Result<Report> getDetail(@PathVariable Long id) {
    Report report = reportService.getDetail(id);
    return Result.success(report);
  }

  /** 手动生成报告（管理员） */
  @PostMapping("/api/reports")
  public Result<Report> generateReport(@Valid @RequestBody ReportGenerateRequest req,
                                        HttpServletRequest request) {
    checkAdmin(request);
    Report report = reportService.generateReport(
        req.getReportPeriod(), req.getPeriodStart(), req.getPeriodEnd());
    return Result.success(report, "报告生成成功");
  }

  /** 配置报告计划（管理员） */
  @PutMapping("/api/reports/config")
  public Result<Map<String, Object>> scheduleConfig(@Valid @RequestBody ReportConfigRequest req,
                                                     HttpServletRequest request) {
    checkAdmin(request);
    reportService.scheduleConfig(req.getReportPeriod(), req.getGenerateTime());

    Map<String, Object> data = new HashMap<>();
    data.put("reportPeriod", req.getReportPeriod());
    data.put("generateTime", req.getGenerateTime());
    data.put("status", "已配置");
    return Result.success(data, "报告计划配置成功");
  }

  /** 管理员权限校验 */
  private void checkAdmin(HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"管理员".equals(role)) {
      throw new BusinessException(403, "越权访问");
    }
  }
}
