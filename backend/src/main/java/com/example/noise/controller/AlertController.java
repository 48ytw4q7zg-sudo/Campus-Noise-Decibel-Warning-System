package com.example.noise.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.noise.common.BusinessException;
import com.example.noise.common.Result;
import com.example.noise.entity.AlertLog;
import com.example.noise.service.AlertLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AlertController {

  private final AlertLogService alertLogService;

  public AlertController(AlertLogService alertLogService) {
    this.alertLogService = alertLogService;
  }

  /** 告警列表分页查询（时间倒序，支持按功能区/日期筛选） */
  @GetMapping("/api/alerts")
  public Result<Map<String, Object>> queryPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "20") Integer pageSize,
                                                @RequestParam(required = false) String location,
                                                @RequestParam(required = false) String dateFrom,
                                                @RequestParam(required = false) String dateTo) {
    IPage<AlertLog> page = new Page<>(pageNum, pageSize);
    IPage<AlertLog> result = alertLogService.queryPage(page, location, dateFrom, dateTo);

    Map<String, Object> data = new HashMap<>();
    data.put("records", result.getRecords());
    data.put("total", result.getTotal());
    data.put("pageNum", result.getCurrent());
    data.put("pageSize", result.getSize());
    data.put("pages", result.getPages());
    return Result.success(data);
  }

  /** 告警详情（含关联噪声记录信息） */
  @GetMapping("/api/alerts/{id}")
  public Result<AlertLog> getDetail(@PathVariable Long id) {
    AlertLog alert = alertLogService.getDetail(id);
    return Result.success(alert);
  }

  /** 确认告警（仅管理员，乐观锁） */
  @PutMapping("/api/alerts/{id}/confirm")
  public Result<Void> confirmAlert(@PathVariable Long id,
                                    @RequestBody Map<String, Integer> body,
                                    HttpServletRequest request) {
    checkAdmin(request);
    Integer version = body.get("version");
    alertLogService.confirmAlert(id, version);
    return Result.success(null, "告警已确认");
  }

  /** 处置告警（仅管理员，乐观锁，含备注） */
  @PutMapping("/api/alerts/{id}/resolve")
  public Result<Void> resolveAlert(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body,
                                    HttpServletRequest request) {
    checkAdmin(request);
    Integer version = (Integer) body.get("version");
    String remark = (String) body.get("remark");
    alertLogService.resolveAlert(id, version, remark);
    return Result.success(null, "告警已处置");
  }

  /** 管理员权限校验 */
  private void checkAdmin(HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"管理员".equals(role)) {
      throw new BusinessException(403, "越权访问");
    }
  }
}
