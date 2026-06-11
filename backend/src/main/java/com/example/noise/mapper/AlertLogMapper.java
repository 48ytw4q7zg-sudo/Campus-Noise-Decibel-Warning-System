package com.example.noise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.noise.entity.AlertLog;

/**
 * 告警日志 Mapper — 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法
 */
public interface AlertLogMapper extends BaseMapper<AlertLog> {
  // P0 阶段不需要自定义方法，BaseMapper 已提供 selectById/selectOne/insert/updateById 等
}
