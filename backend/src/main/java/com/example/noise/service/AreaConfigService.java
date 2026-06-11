package com.example.noise.service;

import com.example.noise.entity.AreaConfig;

import java.util.List;

/**
 * 功能区配置 Service 接口 — P0-6
 */
public interface AreaConfigService {

  /**
   * 查询所有功能区配置列表
   *
   * @return 功能区配置列表
   */
  List<AreaConfig> listAll();

  /**
   * 修改功能区配置（管理员操作），带乐观锁校验
   *
   * @param id 功能区ID
   * @param noiseSensitivity 噪声敏感度（可选）
   * @param defaultThreshold 默认阈值（可选）
   * @param description 描述（可选）
   * @param status 状态（可选）
   * @param version 客户端传入的版本号（乐观锁）
   */
  void updateConfig(Long id, Integer noiseSensitivity, Integer defaultThreshold,
                    String description, Integer status, Integer version);
}
