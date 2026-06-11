package com.example.noise.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.noise.entity.NoiseRecord;
import com.example.noise.entity.dto.NoiseRecordBatchDTO;
import com.example.noise.entity.dto.NoiseRecordRequest;
import com.example.noise.entity.dto.NoiseLatestVO;

import java.util.List;
import java.util.Map;

/**
 * 噪声记录 Service 接口 — P0-2 噪声数据采集与存储
 */
public interface NoiseRecordService {

  /**
   * 创建单条噪声记录（双通道：传感器上报 / 管理员录入）
   *
   * @param req      噪声记录请求 DTO
   * @param isSensor true=传感器通道, false=管理员通道
   * @return 新记录的 id
   */
  Long createRecord(NoiseRecordRequest req, boolean isSensor);

  /**
   * 批量导入噪声记录（仅管理员）
   *
   * @param batch 批量请求 DTO（含 records 列表）
   * @return Map {"successCount": N, "failCount": M}
   */
  Map<String, Integer> batchCreate(NoiseRecordBatchDTO batch);

  /**
   * 获取各功能区最新一条噪声记录（仪表盘用，固定 4 条）
   * 某功能区无数据时对应位置为 null
   *
   * @return 4 个元素的列表，顺序: 图书馆, 食堂, 操场, 宿舍
   */
  List<NoiseLatestVO> getLatestPerArea();

  /**
   * 分页查询噪声记录列表
   *
   * @param page       MyBatis-Plus 分页对象
   * @param location   功能区筛选（可选）
   * @param dateFrom   起始时间 ISO 8601（可选）
   * @param dateTo     截止时间 ISO 8601（可选）
   * @param minDb      最小分贝值（可选）
   * @param maxDb      最大分贝值（可选）
   * @param isAbnormal 异常状态筛选 0/1（可选）
   * @param sortBy     排序字段白名单 {time_point, decibel, create_time}，默认 time_point
   * @param sortOrder  排序方向 ASC/DESC，默认 DESC
   * @return 分页结果
   */
  IPage<NoiseRecord> queryPage(IPage<NoiseRecord> page, String location,
                               String dateFrom, String dateTo,
                               Double minDb, Double maxDb,
                               Integer isAbnormal, String sortBy,
                               String sortOrder);

  /**
   * 查询噪声记录详情
   *
   * @param id 记录 id
   * @return 噪声记录实体
   */
  NoiseRecord getDetail(Long id);

  /**
   * 高级筛选噪声记录（P1-5 扩展：支持 keyword + noiseType）
   *
   * @param page       MyBatis-Plus 分页对象
   * @param keyword    关键字（deviceId 模糊匹配）
   * @param noiseType  噪声类型（P2 预留筛选，可选）
   * @param location   功能区筛选（可选）
   * @param dateFrom   起始时间（可选）
   * @param dateTo     截止时间（可选）
   * @param minDb      最小分贝值（可选）
   * @param maxDb      最大分贝值（可选）
   * @param isAbnormal 异常状态 0/1（可选）
   * @param sortBy     排序字段白名单，默认 time_point
   * @param sortOrder  排序方向 ASC/DESC，默认 DESC
   * @return 分页结果
   */
  IPage<NoiseRecord> searchAdvanced(IPage<NoiseRecord> page, String keyword, String noiseType,
                                     String location, String dateFrom, String dateTo,
                                     Double minDb, Double maxDb,
                                     Integer isAbnormal, String sortBy, String sortOrder);

  /**
   * 导出噪声记录（CSV 导出，不分页，上限 10000 条）
   *
   * @param location   功能区筛选（可选）
   * @param dateFrom   起始时间（可选）
   * @param dateTo     截止时间（可选）
   * @param minDb      最小分贝值（可选）
   * @param maxDb      最大分贝值（可选）
   * @param isAbnormal 异常状态 0/1（可选）
   * @return 噪声记录列表
   */
  List<NoiseRecord> exportRecords(String location, String dateFrom, String dateTo,
                                   Double minDb, Double maxDb, Integer isAbnormal);
}
