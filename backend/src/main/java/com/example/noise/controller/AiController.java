package com.example.noise.controller;

import com.example.noise.common.BusinessException;
import com.example.noise.common.Result;
import com.example.noise.service.AiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 分类控制器 — P2-2 AI辅助噪声分类
 * 对齐 API_DESIGN.md §3.10
 */
@RestController
public class AiController {

  private final AiService aiService;

  public AiController(AiService aiService) {
    this.aiService = aiService;
  }

  /**
   * 手动触发 AI 噪声分类（仅管理员）
   * POST /api/ai/classify
   */
  @PostMapping("/api/ai/classify")
  public Result<Map<String, Object>> classify(HttpServletRequest request) {
    checkAdmin(request);
    Map<String, Object> data = aiService.classify();
    return Result.success(data, "分类完成");
  }

  /**
   * 配置 AI 分类参数（仅管理员）
   * PUT /api/ai/config
   * Body: { "enabled": boolean, "minConfidence": double }
   */
  @PutMapping("/api/ai/config")
  public Result<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
    checkAdmin(request);
    Boolean enabled = body.containsKey("enabled") ? (Boolean) body.get("enabled") : null;
    Double minConfidence = body.containsKey("minConfidence")
        ? ((Number) body.get("minConfidence")).doubleValue() : null;
    aiService.updateConfig(enabled, minConfidence);
    return Result.success(aiService.getConfig(), "配置已更新");
  }

  /**
   * 查询当前 AI 分类配置（仅管理员）
   * GET /api/ai/config
   */
  @GetMapping("/api/ai/config")
  public Result<Map<String, Object>> getConfig(HttpServletRequest request) {
    checkAdmin(request);
    return Result.success(aiService.getConfig());
  }

  /** 管理员权限校验 */
  private void checkAdmin(HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"管理员".equals(role)) {
      throw new BusinessException(403, "越权访问");
    }
  }
}
