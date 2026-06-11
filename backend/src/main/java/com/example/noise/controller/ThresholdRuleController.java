package com.example.noise.controller;

import com.example.noise.common.BusinessException;
import com.example.noise.common.Result;
import com.example.noise.entity.ThresholdRule;
import com.example.noise.service.ThresholdService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 阈值规则 CRUD 控制器 — P1-4
 * 管理员可增/改/删规则，普通用户可查询规则列表
 */
@RestController
public class ThresholdRuleController {

  private final ThresholdService thresholdService;

  public ThresholdRuleController(ThresholdService thresholdService) {
    this.thresholdService = thresholdService;
  }

  /**
   * 查询阈值规则列表（普通用户/管理员均可）
   * GET /api/thresholds/rules?location=图书馆
   */
  @GetMapping("/api/thresholds/rules")
  public Result<List<ThresholdRule>> listRules(@RequestParam(required = false) String location) {
    List<ThresholdRule> data = thresholdService.listRules(location);
    return Result.success(data);
  }

  /**
   * 新增阈值规则（仅管理员）
   * POST /api/thresholds/rules
   * Body: { location, timeSegment, thresholdValue, description }
   */
  @PostMapping("/api/thresholds/rules")
  public Result<ThresholdRule> createRule(@RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
    checkAdmin(request);
    String location = (String) body.get("location");
    String timeSegment = (String) body.get("timeSegment");
    Integer thresholdValue = body.get("thresholdValue") != null
        ? ((Number) body.get("thresholdValue")).intValue() : null;
    String description = (String) body.get("description");

    ThresholdRule rule = thresholdService.createRule(location, timeSegment, thresholdValue, description);
    return Result.success(rule, "阈值规则新增成功");
  }

  /**
   * 修改阈值规则（仅管理员，乐观锁）
   * PUT /api/thresholds/rules/{id}
   * Body: { thresholdValue, description, status, version }
   */
  @PutMapping("/api/thresholds/rules/{id}")
  public Result<Void> updateRule(@PathVariable Long id,
                                  @RequestBody Map<String, Object> body,
                                  HttpServletRequest request) {
    checkAdmin(request);
    Integer thresholdValue = body.get("thresholdValue") != null
        ? ((Number) body.get("thresholdValue")).intValue() : null;
    String description = (String) body.get("description");
    Integer status = body.get("status") != null
        ? ((Number) body.get("status")).intValue() : null;
    Integer version = body.get("version") != null
        ? ((Number) body.get("version")).intValue() : null;

    thresholdService.updateRule(id, thresholdValue, description, status, version);
    return Result.success(null, "阈值规则更新成功");
  }

  /**
   * 删除阈值规则（仅管理员）
   * DELETE /api/thresholds/rules/{id}
   */
  @DeleteMapping("/api/thresholds/rules/{id}")
  public Result<Void> deleteRule(@PathVariable Long id,
                                  HttpServletRequest request) {
    checkAdmin(request);
    thresholdService.deleteRule(id);
    return Result.success(null, "阈值规则删除成功");
  }

  /**
   * 通知 cccswitch 热更新阈值规则（仅管理员）
   * POST /api/thresholds/rules/reload
   */
  @PostMapping("/api/thresholds/rules/reload")
  public Result<Map<String, Object>> reloadRules(HttpServletRequest request) {
    checkAdmin(request);
    Map<String, Object> data = thresholdService.reloadRules();
    return Result.success(data, "热更新通知已完成");
  }

  /** 管理员权限校验 */
  private void checkAdmin(HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"管理员".equals(role)) {
      throw new BusinessException(403, "越权访问");
    }
  }
}
