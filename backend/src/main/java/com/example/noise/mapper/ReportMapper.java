package com.example.noise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.noise.entity.Report;

/**
 * 报告 Mapper（P2 占位） — 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法
 */
public interface ReportMapper extends BaseMapper<Report> {
  // P2 阶段不需要自定义方法，BaseMapper 已提供 selectById/selectOne/insert/updateById 等
}
