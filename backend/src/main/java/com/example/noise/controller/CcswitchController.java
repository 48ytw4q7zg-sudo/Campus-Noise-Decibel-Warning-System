package com.example.noise.controller;

import com.example.noise.common.BusinessException;
import com.example.noise.common.Result;
import com.example.noise.service.CcswitchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ccswitch 配置服务控制器 — P2（对齐 API_DESIGN.md §3.11）
 * 管理员专属接口，查询 ccswitch 状态与触发配置重载。
 */
@RestController
public class CcswitchController {

  private final CcswitchService ccswitchService;

  public CcswitchController(CcswitchService ccswitchService) {
    this.ccswitchService = ccswitchService;
  }

  /** 查询 ccswitch 配置服务状态（所有已登录用户可查，仅管理员触发重载） */
  @GetMapping("/api/ccswitch/status")
  public Result<Map<String, Object>> getStatus() {
    Map<String, Object> status = ccswitchService.getStatus();
    return Result.success(status);
  }

  /** 触发 ccswitch 配置重载（仅管理员） */
  @PostMapping("/api/ccswitch/reload")
  public Result<Map<String, Object>> reload(HttpServletRequest request) {
    checkAdmin(request);
    Map<String, Object> result = ccswitchService.reloadConfig();
    return Result.success(result, "配置重载成功");
  }

  /** 调用 ccswitch 实时计算阈值（仅管理员） */
  @PostMapping("/api/ccswitch/threshold/compute")
  public Result<?> computeThreshold(HttpServletRequest request, @RequestBody Map<String, Object> body) {
    checkAdmin(request);
    Map<String, Object> result = ccswitchService.computeThreshold(body);
    return Result.success(result);
  }

  /** 调用 ccswitch 批量阈值计算（仅管理员） */
  @PostMapping("/api/ccswitch/threshold/batch-compute")
  public Result<?> batchComputeThreshold(HttpServletRequest request, @RequestBody Map<String, Object> body) {
    checkAdmin(request);
    Map<String, Object> result = ccswitchService.batchComputeThreshold(body);
    return Result.success(result);
  }

  /** 更新 ccswitch 区域自适应参数（仅管理员） */
  @PutMapping("/api/ccswitch/threshold/area-config")
  public Result<?> updateAreaConfig(HttpServletRequest request, @RequestBody Map<String, Object> body) {
    checkAdmin(request);
    Map<String, Object> result = ccswitchService.updateAreaConfig(body);
    return Result.success(result, "区域自适应参数已更新");
  }

  /** 查询 ccswitch 区域自适应参数（仅管理员） */
  @GetMapping("/api/ccswitch/threshold/area-config")
  public Result<?> getAreaConfig(HttpServletRequest request) {
    checkAdmin(request);
    Map<String, Object> result = ccswitchService.getAreaConfig();
    return Result.success(result);
  }

  /** 管理员权限校验 */
  private void checkAdmin(HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"管理员".equals(role)) {
      throw new BusinessException(403, "越权访问");
    }
  }
}
