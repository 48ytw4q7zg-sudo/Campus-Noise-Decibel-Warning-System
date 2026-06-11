package com.example.noise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.noise.entity.ThresholdRule;

/**
 * 阈值规则 Mapper — 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法
 */
public interface ThresholdRuleMapper extends BaseMapper<ThresholdRule> {
  // P0 阶段不需要自定义方法，BaseMapper 已提供 selectById/selectOne/insert/updateById 等
}
