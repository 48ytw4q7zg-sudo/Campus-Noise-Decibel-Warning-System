package com.example.noise.controller;

import com.example.noise.common.BusinessException;
import com.example.noise.common.Result;
import com.example.noise.entity.AreaConfig;
import com.example.noise.service.AreaConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AreaController {

  private final AreaConfigService areaConfigService;

  public AreaController(AreaConfigService areaConfigService) {
    this.areaConfigService = areaConfigService;
  }

  /** 功能区配置列表（仅管理员） */
  @GetMapping("/api/areas")
  public Result<List<AreaConfig>> listAll(HttpServletRequest request) {
    checkAdmin(request);
    List<AreaConfig> data = areaConfigService.listAll();
    return Result.success(data);
  }

  /** 修改功能区配置（仅管理员，乐观锁） */
  @PutMapping("/api/areas/{id}")
  public Result<Void> updateConfig(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body,
                                    HttpServletRequest request) {
    checkAdmin(request);
    Integer noiseSensitivity = body.get("noiseSensitivity") != null
        ? ((Number) body.get("noiseSensitivity")).intValue() : null;
    Integer defaultThreshold = body.get("defaultThreshold") != null
        ? ((Number) body.get("defaultThreshold")).intValue() : null;
    String description = (String) body.get("description");
    Integer status = body.get("status") != null
        ? ((Number) body.get("status")).intValue() : null;
    Integer version = body.get("version") != null
        ? ((Number) body.get("version")).intValue() : null;

    areaConfigService.updateConfig(id, noiseSensitivity, defaultThreshold, description, status, version);
    return Result.success(null, "功能区配置更新成功");
  }

  /** 管理员权限校验 */
  private void checkAdmin(HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"管理员".equals(role)) {
      throw new BusinessException(403, "越权访问");
    }
  }
}
