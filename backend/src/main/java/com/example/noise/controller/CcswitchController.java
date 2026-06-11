package com.example.noise.controller;

import com.example.noise.common.BusinessException;
import com.example.noise.common.Result;
import com.example.noise.service.CcswitchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

  /** 查询 ccswitch 配置服务状态（仅管理员） */
  @GetMapping("/api/ccswitch/status")
  public Result<Map<String, Object>> getStatus(HttpServletRequest request) {
    checkAdmin(request);
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

  /** 管理员权限校验 */
  private void checkAdmin(HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"管理员".equals(role)) {
      throw new BusinessException(403, "越权访问");
    }
  }
}
