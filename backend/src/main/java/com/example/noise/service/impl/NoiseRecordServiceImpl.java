package com.example.noise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.example.noise.common.BusinessException;
import com.example.noise.entity.NoiseRecord;
import com.example.noise.entity.dto.NoiseRecordBatchDTO;
import com.example.noise.entity.dto.NoiseRecordRequest;
import com.example.noise.entity.dto.NoiseLatestVO;
import com.example.noise.mapper.NoiseRecordMapper;
import com.example.noise.service.NoiseRecordService;
import com.example.noise.service.ThresholdService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 噪声记录 Service 实现 — P0-2 噪声数据采集与存储
 */
@Service
public class NoiseRecordServiceImpl implements NoiseRecordService {

  /** 合法的四大功能区 */
  private static final Set<String> VALID_LOCATIONS = Set.of("图书馆", "食堂", "操场", "宿舍");

  /** 四大功能区列表（getLatestPerArea / 模拟器 遍历顺序） */
  private static final List<String> AREAS = List.of("图书馆", "食堂", "操场", "宿舍");

  /** 分贝值下限 */
  private static final double DECIBEL_MIN = 20.0;

  /** 分贝值上限 */
  private static final double DECIBEL_MAX = 120.0;

  /** 批量导入单次上限 */
  private static final int BATCH_MAX_SIZE = 5000;

  /** 默认判断模型 */
  private static final String DEFAULT_JUDGED_BY_MODEL = "RULE_BASED";

  /** 管理员录入设备ID前缀 */
  private static final String MANUAL_DEVICE_PREFIX = "MANUAL_";

  /** 传感器默认设备ID */
  private static final String SENSOR_DEFAULT_DEVICE_ID = "SENSOR";

  /** 排序字段白名单：API 字段名 → Lambda 列映射 */
  private static final Map<String, SFunction<NoiseRecord, ?>> SORT_COLUMN_MAP = Map.of(
      "time_point", NoiseRecord::getTimePoint,
      "decibel", NoiseRecord::getDecibel,
      "create_time", NoiseRecord::getCreateTime
  );

  private final NoiseRecordMapper noiseRecordMapper;
  private final ThresholdService thresholdService;

  public NoiseRecordServiceImpl(NoiseRecordMapper noiseRecordMapper,
                                ThresholdService thresholdService) {
    this.noiseRecordMapper = noiseRecordMapper;
    this.thresholdService = thresholdService;
  }

  @Override
  public Long createRecord(NoiseRecordRequest req, boolean isSensor) {
    // 校验功能区合法性
    if (req.getLocation() == null || !VALID_LOCATIONS.contains(req.getLocation())) {
      throw new BusinessException(2002, "功能区不存在");
    }

    // 校验分贝值范围
    if (req.getDecibel() == null
        || req.getDecibel() < DECIBEL_MIN
        || req.getDecibel() > DECIBEL_MAX) {
      throw new BusinessException(2004, "分贝值不在合法范围内（20.0-120.0）");
    }

    // 组装实体
    NoiseRecord record = new NoiseRecord();
    record.setLocation(req.getLocation());
    record.setDecibel(BigDecimal.valueOf(req.getDecibel()));
    record.setTimePoint(req.getTimePoint() != null ? req.getTimePoint() : LocalDateTime.now());

    // 双通道 deviceId 处理
    if (isSensor) {
      // 传感器通道：优先取请求中的 deviceId（Controller 已从 X-Device-Id Header 提取），无则默认 SENSOR
      record.setDeviceId(req.getDeviceId() != null && !req.getDeviceId().isEmpty()
          ? req.getDeviceId() : SENSOR_DEFAULT_DEVICE_ID);
    } else {
      // 管理员通道：自动填充 MANUAL_ + 时间戳
      record.setDeviceId(MANUAL_DEVICE_PREFIX + System.currentTimeMillis());
    }

    // 默认值：未判断异常状态，使用基于规则的模型
    record.setIsAbnormal(null);
    record.setJudgedByModel(DEFAULT_JUDGED_BY_MODEL);

    noiseRecordMapper.insert(record);
    return record.getId();
  }

  @Override
  public Map<String, Integer> batchCreate(NoiseRecordBatchDTO batch) {
    // 校验批量上限
    if (batch.getRecords() == null || batch.getRecords().size() > BATCH_MAX_SIZE) {
      throw new BusinessException(5001, "批量数据超限");
    }

    int successCount = 0;
    int failCount = 0;

    for (NoiseRecordRequest req : batch.getRecords()) {
      try {
        // 复用单条创建逻辑（管理员通道）
        createRecord(req, false);
        successCount++;
      } catch (BusinessException e) {
        // 单条校验失败不影响其他记录，仅计数
        failCount++;
      }
    }

    Map<String, Integer> result = new HashMap<>();
    result.put("successCount", successCount);
    result.put("failCount", failCount);
    return result;
  }

  @Override
  public List<NoiseLatestVO> getLatestPerArea() {
    List<NoiseLatestVO> result = new ArrayList<>(AREAS.size());

    for (String area : AREAS) {
      LambdaQueryWrapper<NoiseRecord> wrapper = new LambdaQueryWrapper<>();
      wrapper.eq(NoiseRecord::getLocation, area)
             .orderByDesc(NoiseRecord::getTimePoint)
             .last("LIMIT 1");
      NoiseRecord record = noiseRecordMapper.selectOne(wrapper);

      if (record != null) {
        NoiseLatestVO vo = new NoiseLatestVO();
        vo.setId(record.getId());
        vo.setLocation(record.getLocation());
        vo.setDecibel(record.getDecibel());
        vo.setTimePoint(record.getTimePoint());
        // 查询当前功能区对应时段的阈值
        try {
          vo.setThresholdValue(thresholdService.getCurrentThreshold(area).getThresholdValue());
        } catch (Exception e) {
          vo.setThresholdValue(55); // 兜底全局默认值
        }
        vo.setIsAbnormal(record.getIsAbnormal());
        result.add(vo);
      } else {
        // 该功能区无数据，对应位置为 null
        result.add(null);
      }
    }

    return result;
  }

  @Override
  public IPage<NoiseRecord> queryPage(IPage<NoiseRecord> page, String location,
                                       String dateFrom, String dateTo,
                                       Double minDb, Double maxDb,
                                       Integer isAbnormal, String sortBy,
                                       String sortOrder) {
    LambdaQueryWrapper<NoiseRecord> wrapper = new LambdaQueryWrapper<>();

    // 功能区筛选
    if (location != null && !location.isEmpty()) {
      wrapper.eq(NoiseRecord::getLocation, location);
    }

    // 分贝范围筛选
    if (minDb != null) {
      wrapper.ge(NoiseRecord::getDecibel, BigDecimal.valueOf(minDb));
    }
    if (maxDb != null) {
      wrapper.le(NoiseRecord::getDecibel, BigDecimal.valueOf(maxDb));
    }

    // 时间范围筛选
    if (dateFrom != null && !dateFrom.isEmpty()) {
      wrapper.ge(NoiseRecord::getTimePoint, LocalDateTime.parse(dateFrom));
    }
    if (dateTo != null && !dateTo.isEmpty()) {
      wrapper.le(NoiseRecord::getTimePoint, LocalDateTime.parse(dateTo));
    }

    // 异常状态筛选
    if (isAbnormal != null) {
      wrapper.eq(NoiseRecord::getIsAbnormal, isAbnormal);
    }

    // 排序字段白名单校验 → 默认 time_point
    SFunction<NoiseRecord, ?> sortColumn = SORT_COLUMN_MAP.getOrDefault(sortBy, NoiseRecord::getTimePoint);
    if ("ASC".equalsIgnoreCase(sortOrder)) {
      wrapper.orderByAsc(sortColumn);
    } else {
      wrapper.orderByDesc(sortColumn);
    }

    return noiseRecordMapper.selectPage(page, wrapper);
  }

  @Override
  public NoiseRecord getDetail(Long id) {
    NoiseRecord record = noiseRecordMapper.selectById(id);
    if (record == null) {
      throw new BusinessException(2003, "噪声记录不存在");
    }
    return record;
  }

  @Override
  public IPage<NoiseRecord> searchAdvanced(IPage<NoiseRecord> page, String keyword,
                                            String noiseType, String location,
                                            String dateFrom, String dateTo,
                                            Double minDb, Double maxDb,
                                            Integer isAbnormal, String sortBy,
                                            String sortOrder) {
    LambdaQueryWrapper<NoiseRecord> wrapper = new LambdaQueryWrapper<>();

    // 关键字：deviceId 模糊匹配
    if (keyword != null && !keyword.isEmpty()) {
      wrapper.like(NoiseRecord::getDeviceId, keyword);
    }

    // 噪声类型筛选（P2 预留）
    if (noiseType != null && !noiseType.isEmpty()) {
      wrapper.eq(NoiseRecord::getNoiseType, noiseType);
    }

    // 功能区筛选
    if (location != null && !location.isEmpty()) {
      wrapper.eq(NoiseRecord::getLocation, location);
    }

    // 分贝范围筛选
    if (minDb != null) {
      wrapper.ge(NoiseRecord::getDecibel, BigDecimal.valueOf(minDb));
    }
    if (maxDb != null) {
      wrapper.le(NoiseRecord::getDecibel, BigDecimal.valueOf(maxDb));
    }

    // 时间范围筛选
    if (dateFrom != null && !dateFrom.isEmpty()) {
      wrapper.ge(NoiseRecord::getTimePoint, LocalDateTime.parse(dateFrom));
    }
    if (dateTo != null && !dateTo.isEmpty()) {
      wrapper.le(NoiseRecord::getTimePoint, LocalDateTime.parse(dateTo));
    }

    // 异常状态筛选
    if (isAbnormal != null) {
      wrapper.eq(NoiseRecord::getIsAbnormal, isAbnormal);
    }

    // 排序字段白名单校验 → 默认 time_point
    SFunction<NoiseRecord, ?> sortColumn = SORT_COLUMN_MAP.getOrDefault(sortBy, NoiseRecord::getTimePoint);
    if ("ASC".equalsIgnoreCase(sortOrder)) {
      wrapper.orderByAsc(sortColumn);
    } else {
      wrapper.orderByDesc(sortColumn);
    }

    return noiseRecordMapper.selectPage(page, wrapper);
  }

  @Override
  public List<NoiseRecord> exportRecords(String location, String dateFrom, String dateTo,
                                          Double minDb, Double maxDb, Integer isAbnormal) {
    LambdaQueryWrapper<NoiseRecord> wrapper = new LambdaQueryWrapper<>();

    // 功能区筛选
    if (location != null && !location.isEmpty()) {
      wrapper.eq(NoiseRecord::getLocation, location);
    }

    // 分贝范围筛选
    if (minDb != null) {
      wrapper.ge(NoiseRecord::getDecibel, BigDecimal.valueOf(minDb));
    }
    if (maxDb != null) {
      wrapper.le(NoiseRecord::getDecibel, BigDecimal.valueOf(maxDb));
    }

    // 时间范围筛选
    if (dateFrom != null && !dateFrom.isEmpty()) {
      wrapper.ge(NoiseRecord::getTimePoint, LocalDateTime.parse(dateFrom));
    }
    if (dateTo != null && !dateTo.isEmpty()) {
      wrapper.le(NoiseRecord::getTimePoint, LocalDateTime.parse(dateTo));
    }

    // 异常状态筛选
    if (isAbnormal != null) {
      wrapper.eq(NoiseRecord::getIsAbnormal, isAbnormal);
    }

    wrapper.orderByDesc(NoiseRecord::getTimePoint);

    // 先查总数，超 10000 抛异常
    Long count = noiseRecordMapper.selectCount(wrapper);
    if (count > 10000) {
      throw new BusinessException(5001, "导出数据量超过上限（10000条），请缩小筛选范围");
    }

    // 不分页，上限 10000 条已校验
    wrapper.last("LIMIT 10000");
    return noiseRecordMapper.selectList(wrapper);
  }

  /**
   * 模拟器定时生成噪声数据（每 5 分钟）
   * 随机选功能区，生成 35.0-85.0 dB(A) 的模拟数据
   */
  @Scheduled(fixedRate = 300000)
  public void generateSimulatedData() {
    String location = AREAS.get(ThreadLocalRandom.current().nextInt(AREAS.size()));
    BigDecimal decibel = BigDecimal.valueOf(
        35.0 + ThreadLocalRandom.current().nextDouble() * 50.0)
        .setScale(1, RoundingMode.HALF_UP);

    NoiseRecord record = new NoiseRecord();
    record.setLocation(location);
    record.setDecibel(decibel);
    record.setTimePoint(LocalDateTime.now());
    record.setDeviceId("SIMULATOR");
    record.setIsAbnormal(null); // 待业务规则判断
    record.setJudgedByModel("RULE_BASED");
    noiseRecordMapper.insert(record);

    // 自动触发混合模型判断
    try {
      thresholdService.autoJudgeWithHybrid(record);
    } catch (Exception e) {
      // 判断失败不阻塞模拟生成
    }
  }
}
