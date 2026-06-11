package com.example.noise.controller;

import com.example.noise.common.BusinessException;
import com.example.noise.common.Result;
import com.example.noise.entity.dto.UpdateAdaptiveConfigRequest;
import com.example.noise.service.ThresholdService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ThresholdController {

  private final ThresholdService thresholdService;

  public ThresholdController(ThresholdService thresholdService) {
    this.thresholdService = thresholdService;
  }

  /** 查询某功能区当前时段的业务规则阈值 */
  @GetMapping("/api/thresholds/current")
  public Result<?> getCurrentThreshold(@RequestParam String location) {
    Object rule = thresholdService.getCurrentThreshold(location);
    return Result.success(rule);
  }

  /** 手动触发某条噪声记录的阈值判断（仅管理员） */
  @PostMapping("/api/thresholds/check/{id}")
  public Result<Map<String, Object>> checkRecord(@PathVariable Long id,
                                                  HttpServletRequest request) {
    checkAdmin(request);
    Map<String, Object> result = thresholdService.checkRecord(id);
    return Result.success(result, "判断完成");
  }

  /** 通知 ccswitch 热更新阈值规则（仅管理员） */
  @PostMapping("/api/thresholds/reload")
  public Result<Map<String, Object>> reloadRules(HttpServletRequest request) {
    checkAdmin(request);
    Map<String, Object> result = thresholdService.reloadRules();
    return Result.success(result, "阈值规则已重载");
  }

  // ==================== P1-1 统计自适应阈值 ====================

  /** 查询某功能区当前统计自适应阈值 */
  @GetMapping("/api/thresholds/adaptive/current")
  public Result<Map<String, Object>> getAdaptiveThreshold(@RequestParam String location) {
    Map<String, Object> data = thresholdService.getAdaptiveThreshold(location);
    return Result.success(data);
  }

  /** 批量配置各功能区的自适应参数（仅管理员） */
  @PutMapping("/api/thresholds/adaptive/config")
  public Result<?> updateAdaptiveConfig(@Valid @RequestBody UpdateAdaptiveConfigRequest body,
                                        HttpServletRequest request) {
    checkAdmin(request);
    List<Map<String, Object>> areaConfigs = body.getAreaConfigs().stream()
        .map(c -> {
          Map<String, Object> m = new HashMap<>();
          m.put("location", c.getLocation());
          m.put("windowSize", c.getWindowSize());
          m.put("kValue", c.getKValue());
          return m;
        })
        .collect(Collectors.toList());
    thresholdService.updateAdaptiveConfig(areaConfigs);
    return Result.success(null, "自适应参数配置成功");
  }

  // ==================== P1-2 混合阈值模型 ====================

  /** 查询混合模型运行状态 */
  @GetMapping("/api/thresholds/hybrid/status")
  public Result<Map<String, Object>> getHybridStatus() {
    Map<String, Object> data = thresholdService.getHybridStatus();
    return Result.success(data);
  }

  /** 查询混合模型性能指标 */
  @GetMapping("/api/thresholds/hybrid/performance")
  public Result<Map<String, Object>> getHybridPerformance() {
    Map<String, Object> data = thresholdService.getHybridPerformance();
    return Result.success(data);
  }

  /** 管理员权限校验 */
  private void checkAdmin(HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"管理员".equals(role)) {
      throw new BusinessException(403, "越权访问");
    }
  }
}
