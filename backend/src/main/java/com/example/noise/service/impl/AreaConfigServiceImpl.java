package com.example.noise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.noise.common.BusinessException;
import com.example.noise.entity.AreaConfig;
import com.example.noise.mapper.AreaConfigMapper;
import com.example.noise.service.AreaConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 功能区配置 Service 实现 — P0-6
 * 查询所有功能区 / 修改配置（含乐观锁）
 */
@Service
public class AreaConfigServiceImpl implements AreaConfigService {

  private final AreaConfigMapper areaConfigMapper;

  public AreaConfigServiceImpl(AreaConfigMapper areaConfigMapper) {
    this.areaConfigMapper = areaConfigMapper;
  }

  @Override
  public List<AreaConfig> listAll() {
    return areaConfigMapper.selectList(new LambdaQueryWrapper<>());
  }

  @Override
  public void updateConfig(Long id, Integer noiseSensitivity, Integer defaultThreshold,
                           String description, Integer status, Integer version) {
    AreaConfig area = areaConfigMapper.selectById(id);
    if (area == null) {
      throw new BusinessException(6001, "功能区配置不存在");
    }

    // 乐观锁检查：当前版本与客户端传入版本不一致则拒绝
    if (!area.getVersion().equals(version)) {
      throw new BusinessException(6002, "配置已被他人修改，请刷新后重试");
    }

    // 仅更新非 null 字段
    if (noiseSensitivity != null) {
      area.setNoiseSensitivity(noiseSensitivity);
    }
    if (defaultThreshold != null) {
      area.setDefaultThreshold(defaultThreshold);
    }
    if (description != null) {
      area.setDescription(description);
    }
    if (status != null) {
      area.setStatus(status);
    }
    area.setVersion(area.getVersion() + 1);

    areaConfigMapper.updateById(area);
  }
}
