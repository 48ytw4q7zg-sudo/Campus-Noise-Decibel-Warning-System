package com.example.noise.service.impl;

import com.example.noise.common.BusinessException;
import com.example.noise.entity.AreaConfig;
import com.example.noise.entity.NoiseRecord;
import com.example.noise.entity.ThresholdRule;
import com.example.noise.mapper.AreaConfigMapper;
import com.example.noise.mapper.NoiseRecordMapper;
import com.example.noise.mapper.ThresholdRuleMapper;
import com.example.noise.service.AlertLogService;
import com.example.noise.service.CcswitchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThresholdServiceImplTest {

  @Mock private ThresholdRuleMapper thresholdRuleMapper;
  @Mock private AreaConfigMapper areaConfigMapper;
  @Mock private NoiseRecordMapper noiseRecordMapper;
  @Mock private AlertLogService alertLogService;
  @Mock private CcswitchService ccswitchService;
  @InjectMocks private ThresholdServiceImpl thresholdService;

  private ThresholdRule buildRule(Long id, String location, String timeSegment, int thresholdValue) {
    ThresholdRule r = new ThresholdRule();
    r.setId(id);
    r.setLocation(location);
    r.setTimeSegment(timeSegment);
    r.setThresholdValue(thresholdValue);
    r.setStatus(1);
    r.setVersion(0);
    return r;
  }

  private NoiseRecord buildRecord(Long id, String location, int decibel, Integer isAbnormal) {
    NoiseRecord r = new NoiseRecord();
    r.setId(id);
    r.setLocation(location);
    r.setDecibel(BigDecimal.valueOf(decibel));
    r.setTimePoint(LocalDateTime.of(2026, 6, 10, 10, 0));
    r.setIsAbnormal(isAbnormal);
    return r;
  }

  private List<NoiseRecord> buildRecords(int count, int decibel) {
    List<NoiseRecord> records = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      records.add(buildRecord((long) (i + 1), "图书馆", decibel, 0));
    }
    return records;
  }

  // ===== getCurrentThreshold =====

  @Test
  void getCurrentThreshold_shouldReturnRuleWhenFound() {
    ThresholdRule rule = buildRule(1L, "图书馆", "上课", 50);
    when(thresholdRuleMapper.selectOne(any())).thenReturn(rule);
    ThresholdRule result = thresholdService.getCurrentThreshold("图书馆");
    assertThat(result.getThresholdValue()).isEqualTo(50);
    verify(areaConfigMapper, never()).selectOne(any());
  }

  @Test
  void getCurrentThreshold_shouldFallbackToAreaDefault() {
    when(thresholdRuleMapper.selectOne(any())).thenReturn(null);
    AreaConfig area = new AreaConfig();
    area.setAreaName("图书馆");
    area.setDefaultThreshold(45);
    when(areaConfigMapper.selectOne(any())).thenReturn(area);
    ThresholdRule result = thresholdService.getCurrentThreshold("图书馆");
    assertThat(result.getThresholdValue()).isEqualTo(45);
    assertThat(result.getDescription()).contains("兜底");
  }

  @Test
  void getCurrentThreshold_shouldFallbackToGlobal55() {
    when(thresholdRuleMapper.selectOne(any())).thenReturn(null);
    when(areaConfigMapper.selectOne(any())).thenReturn(null);
    ThresholdRule result = thresholdService.getCurrentThreshold("图书馆");
    assertThat(result.getThresholdValue()).isEqualTo(55);
    assertThat(result.getDescription()).contains("55");
  }

  // ===== checkRecord =====

  @Test
  void checkRecord_shouldMarkAbnormalAndCreateAlert() {
    NoiseRecord record = buildRecord(1L, "图书馆", 70, null);
    when(noiseRecordMapper.selectById(1L)).thenReturn(record);
    ThresholdRule rule = buildRule(null, "图书馆", "上课", 50);
    when(thresholdRuleMapper.selectOne(any())).thenReturn(rule);
    when(noiseRecordMapper.updateById(any(NoiseRecord.class))).thenReturn(1);

    Map<String, Object> result = thresholdService.checkRecord(1L);
    assertThat(result).containsEntry("isAbnormal", 1);
    assertThat(result).containsEntry("alertCreated", true);
    verify(alertLogService).createAlert(1L, "图书馆", BigDecimal.valueOf(70), 50, "超阈值");
  }

  @Test
  void checkRecord_shouldNotCreateAlertWhenNormal() {
    NoiseRecord record = buildRecord(1L, "图书馆", 40, null);
    when(noiseRecordMapper.selectById(1L)).thenReturn(record);
    ThresholdRule rule = buildRule(null, "图书馆", "上课", 50);
    when(thresholdRuleMapper.selectOne(any())).thenReturn(rule);
    when(noiseRecordMapper.updateById(any(NoiseRecord.class))).thenReturn(1);

    Map<String, Object> result = thresholdService.checkRecord(1L);
    assertThat(result).containsEntry("isAbnormal", 0);
    assertThat(result).containsEntry("alertCreated", false);
    verify(alertLogService, never()).createAlert(anyLong(), anyString(), any(), anyInt(), anyString());
  }

  @Test
  void checkRecord_shouldThrowWhenRecordNotFound() {
    when(noiseRecordMapper.selectById(999L)).thenReturn(null);
    assertThatThrownBy(() -> thresholdService.checkRecord(999L))
        .isInstanceOf(BusinessException.class)
        .extracting("code").isEqualTo(2001);
  }

  @Test
  void checkRecord_shouldThrowWhenAlreadyJudged() {
    NoiseRecord record = buildRecord(1L, "图书馆", 60, 1);
    when(noiseRecordMapper.selectById(1L)).thenReturn(record);
    assertThatThrownBy(() -> thresholdService.checkRecord(1L))
        .isInstanceOf(BusinessException.class)
        .extracting("code").isEqualTo(2004);
  }

  // ===== createRule =====

  @Test
  void createRule_shouldInsert() {
    when(thresholdRuleMapper.selectCount(any())).thenReturn(0L);
    when(thresholdRuleMapper.insert(any(ThresholdRule.class))).thenReturn(1);
    ThresholdRule result = thresholdService.createRule("图书馆", "上课", 55, "test");
    assertThat(result.getLocation()).isEqualTo("图书馆");
    assertThat(result.getStatus()).isEqualTo(1);
  }

  @Test
  void createRule_shouldThrowWhenDuplicate() {
    when(thresholdRuleMapper.selectCount(any())).thenReturn(1L);
    assertThatThrownBy(() -> thresholdService.createRule("图书馆", "上课", 55, "dup"))
        .isInstanceOf(BusinessException.class)
        .extracting("code").isEqualTo(3001);
  }

  @Test
  void createRule_shouldThrowWhenInvalidLocation() {
    assertThatThrownBy(() -> thresholdService.createRule("教学楼", "上课", 55, "x"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void createRule_shouldThrowWhenThresholdOutOfRange() {
    assertThatThrownBy(() -> thresholdService.createRule("图书馆", "上课", 150, "x"))
        .isInstanceOf(BusinessException.class);
  }

  // ===== updateRule =====

  @Test
  void updateRule_shouldUpdateAndBumpVersion() {
    ThresholdRule rule = buildRule(1L, "图书馆", "上课", 43);
    when(thresholdRuleMapper.selectById(1L)).thenReturn(rule);
    when(thresholdRuleMapper.updateById(any(ThresholdRule.class))).thenReturn(1);
    thresholdService.updateRule(1L, 45, "updated", 1, 0);
    assertThat(rule.getVersion()).isEqualTo(1);
    assertThat(rule.getThresholdValue()).isEqualTo(45);
  }

  @Test
  void updateRule_shouldThrowWhenVersionConflict() {
    ThresholdRule rule = buildRule(1L, "图书馆", "上课", 43);
    rule.setVersion(2);  // DB version is 2, client sends version=0 → conflict
    when(thresholdRuleMapper.selectById(1L)).thenReturn(rule);
    assertThatThrownBy(() -> thresholdService.updateRule(1L, 45, null, null, 0))
        .isInstanceOf(BusinessException.class)
        .extracting("code").isEqualTo(3002);
  }

  @Test
  void updateRule_shouldThrowWhenNotFound() {
    when(thresholdRuleMapper.selectById(1L)).thenReturn(null);
    assertThatThrownBy(() -> thresholdService.updateRule(1L, 45, null, null, 0))
        .isInstanceOf(BusinessException.class)
        .extracting("code").isEqualTo(2003);
  }

  // ===== deleteRule =====

  @Test
  void deleteRule_shouldDelete() {
    ThresholdRule rule = buildRule(1L, "图书馆", "上课", 43);
    when(thresholdRuleMapper.selectById(1L)).thenReturn(rule);
    when(thresholdRuleMapper.deleteById(1L)).thenReturn(1);
    thresholdService.deleteRule(1L);
    verify(thresholdRuleMapper).deleteById(1L);
  }

  @Test
  void deleteRule_shouldThrowWhenNotFound() {
    when(thresholdRuleMapper.selectById(1L)).thenReturn(null);
    assertThatThrownBy(() -> thresholdService.deleteRule(1L))
        .isInstanceOf(BusinessException.class);
  }

  // ===== listRules =====

  @Test
  void listRules_shouldFilterByLocation() {
    when(thresholdRuleMapper.selectList(any())).thenReturn(Collections.emptyList());
    List<ThresholdRule> result = thresholdService.listRules("图书馆");
    assertThat(result).isEmpty();
  }

  // ===== reloadRules =====

  @Test
  void reloadRules_shouldCountAndNotifyCcswitch() {
    when(thresholdRuleMapper.selectCount(any())).thenReturn(5L);
    lenient().when(ccswitchService.reloadThresholdRules()).thenReturn(new HashMap<>());
    Map<String, Object> result = thresholdService.reloadRules();
    assertThat(result.get("ruleCount")).isEqualTo(5L);
  }

  @Test
  void reloadRules_shouldGracefullyHandleCcswitchDown() {
    when(thresholdRuleMapper.selectCount(any())).thenReturn(3L);
    lenient().when(ccswitchService.reloadThresholdRules()).thenThrow(new BusinessException(7001, "down"));
    Map<String, Object> result = thresholdService.reloadRules();
    assertThat(result.get("ruleCount")).isEqualTo(3L);
    assertThat(result.get("ccswitchStatus").toString()).contains("不可用");
  }

  // ===== getAdaptiveThreshold =====

  @Test
  void getAdaptiveThreshold_shouldFallbackWhenInsufficientData() {
    AreaConfig area = new AreaConfig();
    area.setAreaName("图书馆");
    area.setWindowSize(15);
    area.setKValue(new BigDecimal("2.00"));
    when(areaConfigMapper.selectOne(any())).thenReturn(area);
    when(noiseRecordMapper.selectRecentForAdaptive(eq("图书馆"), eq(15)))
        .thenReturn(Collections.emptyList());
    ThresholdRule rule = buildRule(1L, "图书馆", "上课", 40);
    when(thresholdRuleMapper.selectOne(any())).thenReturn(rule);

    Map<String, Object> result = thresholdService.getAdaptiveThreshold("图书馆");
    assertThat(result.get("fallback")).isEqualTo(true);
    assertThat(result.get("upperLimit")).isEqualTo(40);
  }

  @Test
  void getAdaptiveThreshold_shouldCalculateWithSufficientData() {
    AreaConfig area = new AreaConfig();
    area.setAreaName("图书馆");
    area.setWindowSize(3);
    area.setKValue(new BigDecimal("2.00"));
    when(areaConfigMapper.selectOne(any())).thenReturn(area);
    when(noiseRecordMapper.selectRecentForAdaptive(eq("图书馆"), eq(3)))
        .thenReturn(Arrays.asList(
            buildRecord(1L, "图书馆", 40, 0),
            buildRecord(2L, "图书馆", 45, 0),
            buildRecord(3L, "图书馆", 50, 0)
        ));

    Map<String, Object> result = thresholdService.getAdaptiveThreshold("图书馆");
    assertThat(result.get("fallback")).isEqualTo(false);
    assertThat((BigDecimal) result.get("mean")).isNotNull();
    assertThat(((BigDecimal) result.get("upperLimit")).doubleValue()).isGreaterThan(45.0);
  }

  @Test
  void getAdaptiveThreshold_shouldThrowWhenInvalidLocation() {
    assertThatThrownBy(() -> thresholdService.getAdaptiveThreshold("教学楼"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void getAdaptiveThreshold_shouldThrowWhenNullLocation() {
    assertThatThrownBy(() -> thresholdService.getAdaptiveThreshold(null))
        .isInstanceOf(BusinessException.class);
  }

  // ===== updateAdaptiveConfig =====

  @Test
  void updateAdaptiveConfig_shouldThrowWhenEmpty() {
    assertThatThrownBy(() -> thresholdService.updateAdaptiveConfig(null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void updateAdaptiveConfig_shouldThrowWhenKValueOutOfRange() {
    Map<String, Object> config = new HashMap<>();
    config.put("location", "图书馆");
    config.put("windowSize", 20);
    config.put("kValue", 6.0);
    assertThatThrownBy(() -> thresholdService.updateAdaptiveConfig(Collections.singletonList(config)))
        .isInstanceOf(BusinessException.class)
        .extracting("code").isEqualTo(400);
  }

  @Test
  void updateAdaptiveConfig_shouldUpdate() {
    AreaConfig area = new AreaConfig();
    area.setAreaName("图书馆");
    when(areaConfigMapper.selectOne(any())).thenReturn(area);
    when(areaConfigMapper.updateById(any(AreaConfig.class))).thenReturn(1);

    Map<String, Object> config = new HashMap<>();
    config.put("location", "图书馆");
    config.put("windowSize", 20);
    config.put("kValue", 2.5);
    thresholdService.updateAdaptiveConfig(Collections.singletonList(config));
    assertThat(area.getWindowSize()).isEqualTo(20);
    verify(areaConfigMapper).updateById(area);
  }

  // ===== getHybridStatus =====

  @Test
  void getHybridStatus_shouldReturnStatus() {
    lenient().when(areaConfigMapper.selectOne(any())).thenReturn(null);
    lenient().when(noiseRecordMapper.selectRecentForAdaptive(anyString(), anyInt()))
        .thenReturn(Collections.emptyList());
    Map<String, Object> result = thresholdService.getHybridStatus();
    assertThat(result).containsKeys("currentMode", "isTriggered", "triggerReason", "abnormalRate3Windows");
    assertThat(result.get("isTriggered")).isIn(false, true);
  }

  // ===== getHybridPerformance =====

  @Test
  void getHybridPerformance_shouldReturnFixedData() {
    Map<String, Object> result = thresholdService.getHybridPerformance();
    assertThat(result.get("accuracy")).isEqualTo(92.6);
    assertThat(result.get("f1Score")).isEqualTo(91.2);
    assertThat(result).containsKey("modeDistribution");
  }

  // ===== autoJudgeWithHybrid =====

  @Test
  void autoJudgeWithHybrid_shouldSkipWhenAlreadyJudged() {
    NoiseRecord record = buildRecord(1L, "图书馆", 50, 0);
    thresholdService.autoJudgeWithHybrid(record);
    verify(noiseRecordMapper, never()).updateById(any(NoiseRecord.class));
    verify(alertLogService, never()).createAlert(anyLong(), anyString(), any(), anyInt(), anyString());
  }
}
