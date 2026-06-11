package com.example.noise.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.noise.common.BusinessException;
import com.example.noise.entity.AlertLog;
import com.example.noise.mapper.AlertLogMapper;
import com.example.noise.mapper.NoiseRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AlertLogServiceImpl 单元测试 — P0-5 告警记录
 * 覆盖: 创建告警 / 分页查询 / 详情 / 确认 / 处置
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertLogServiceImpl 单元测试")
class AlertLogServiceImplTest {

  @Mock
  private AlertLogMapper alertLogMapper;

  @Mock
  private NoiseRecordMapper noiseRecordMapper;

  @InjectMocks
  private AlertLogServiceImpl alertLogService;

  // ============ 辅助工厂方法 ============

  private AlertLog newAlertLog(Long id, String confirmStatus, Integer version) {
    AlertLog alert = new AlertLog();
    alert.setId(id);
    alert.setNoiseRecordId(100L);
    alert.setLocation("图书馆");
    alert.setDecibel(new BigDecimal("75.5"));
    alert.setThresholdValue(70);
    alert.setAlertType("ABOVE_THRESHOLD");
    alert.setConfirmStatus(confirmStatus);
    alert.setVersion(version);
    return alert;
  }

  // ============ createAlert ============

  @Nested
  @DisplayName("createAlert — 创建告警记录")
  class CreateAlertTests {

    @Test
    @DisplayName("正常创建：insert 被调用且字段正确")
    void shouldInsertAlertLogWithCorrectFields() {
      // given
      Long noiseRecordId = 200L;
      String location = "教学楼";
      BigDecimal decibel = new BigDecimal("82.3");
      Integer thresholdValue = 75;
      String alertType = "SUSTAINED";

      // when
      alertLogService.createAlert(noiseRecordId, location, decibel, thresholdValue, alertType);

      // then
      ArgumentCaptor<AlertLog> captor = ArgumentCaptor.forClass(AlertLog.class);
      verify(alertLogMapper).insert(captor.capture());
      AlertLog inserted = captor.getValue();

      assertThat(inserted.getNoiseRecordId()).isEqualTo(noiseRecordId);
      assertThat(inserted.getLocation()).isEqualTo(location);
      assertThat(inserted.getDecibel()).isEqualByComparingTo(decibel);
      assertThat(inserted.getThresholdValue()).isEqualTo(thresholdValue);
      assertThat(inserted.getAlertType()).isEqualTo(alertType);
      assertThat(inserted.getConfirmStatus()).isEqualTo("未确认");
    }
  }

  // ============ queryPage ============

  @Nested
  @DisplayName("queryPage — 分页查询")
  class QueryPageTests {

    @Test
    @DisplayName("无条件筛选：仅按创建时间倒序")
    void shouldQueryWithoutFilters() {
      // given
      Page<AlertLog> page = new Page<>(1, 10);
      when(alertLogMapper.selectPage(eq(page), any())).thenReturn(page);

      // when
      IPage<AlertLog> result = alertLogService.queryPage(page, null, null, null);

      // then
      assertThat(result).isSameAs(page);
      verify(alertLogMapper).selectPage(eq(page), any());
    }

    @Test
    @DisplayName("含 location 筛选")
    void shouldFilterByLocation() {
      Page<AlertLog> page = new Page<>(1, 10);
      when(alertLogMapper.selectPage(eq(page), any())).thenReturn(page);

      IPage<AlertLog> result = alertLogService.queryPage(page, "体育馆", null, null);

      assertThat(result).isSameAs(page);
      verify(alertLogMapper).selectPage(eq(page), any());
    }

    @Test
    @DisplayName("含 location + 时间范围筛选")
    void shouldFilterByLocationAndTimeRange() {
      Page<AlertLog> page = new Page<>(1, 10);
      when(alertLogMapper.selectPage(eq(page), any())).thenReturn(page);

      IPage<AlertLog> result = alertLogService.queryPage(
          page, "图书馆", "2026-01-01 00:00:00", "2026-06-11 23:59:59");

      assertThat(result).isSameAs(page);
      verify(alertLogMapper).selectPage(eq(page), any());
    }

    @Test
    @DisplayName("空白字符串视同无筛选")
    void shouldTreatBlankStringsAsNoFilter() {
      Page<AlertLog> page = new Page<>(1, 10);
      when(alertLogMapper.selectPage(eq(page), any())).thenReturn(page);

      IPage<AlertLog> result = alertLogService.queryPage(page, "  ", "  ", "  ");

      assertThat(result).isSameAs(page);
      verify(alertLogMapper).selectPage(eq(page), any());
    }
  }

  // ============ getDetail ============

  @Nested
  @DisplayName("getDetail — 告警详情")
  class GetDetailTests {

    @Test
    @DisplayName("找到：返回告警实体")
    void shouldReturnAlertWhenFound() {
      AlertLog existing = newAlertLog(1L, "未确认", 0);
      when(alertLogMapper.selectById(1L)).thenReturn(existing);

      AlertLog result = alertLogService.getDetail(1L);

      assertThat(result).isSameAs(existing);
      assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("未找到：抛出 BusinessException(4001)")
    void shouldThrowWhenNotFound() {
      when(alertLogMapper.selectById(999L)).thenReturn(null);

      assertThatThrownBy(() -> alertLogService.getDetail(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("code")
          .isEqualTo(4001);
    }
  }

  // ============ confirmAlert ============

  @Nested
  @DisplayName("confirmAlert — 确认告警")
  class ConfirmAlertTests {

    @Test
    @DisplayName("正常确认：状态从未确认→已确认，版本+1")
    void shouldConfirmWhenUnconfirmedAndVersionMatches() {
      AlertLog alert = newAlertLog(1L, "未确认", 0);
      when(alertLogMapper.selectById(1L)).thenReturn(alert);

      alertLogService.confirmAlert(1L, 0);

      assertThat(alert.getConfirmStatus()).isEqualTo("已确认");
      assertThat(alert.getVersion()).isEqualTo(1);
      verify(alertLogMapper).updateById(alert);
    }

    @Test
    @DisplayName("告警不存在：抛出 BusinessException(4001)")
    void shouldThrowWhenAlertNotFoundForConfirm() {
      when(alertLogMapper.selectById(999L)).thenReturn(null);

      assertThatThrownBy(() -> alertLogService.confirmAlert(999L, 0))
          .isInstanceOf(BusinessException.class)
          .extracting("code")
          .isEqualTo(4001);
      verify(alertLogMapper, never()).updateById(any(AlertLog.class));
    }

    @Test
    @DisplayName("已确认状态：抛出 BusinessException(4002)")
    void shouldThrowWhenAlreadyConfirmed() {
      AlertLog alert = newAlertLog(1L, "已确认", 1);
      when(alertLogMapper.selectById(1L)).thenReturn(alert);

      assertThatThrownBy(() -> alertLogService.confirmAlert(1L, 1))
          .isInstanceOf(BusinessException.class)
          .extracting("code")
          .isEqualTo(4002);
      verify(alertLogMapper, never()).updateById(any(AlertLog.class));
    }

    @Test
    @DisplayName("已处置状态：抛出 BusinessException(4002)")
    void shouldThrowWhenAlreadyResolvedForConfirm() {
      AlertLog alert = newAlertLog(1L, "已处置", 2);
      when(alertLogMapper.selectById(1L)).thenReturn(alert);

      assertThatThrownBy(() -> alertLogService.confirmAlert(1L, 2))
          .isInstanceOf(BusinessException.class)
          .extracting("code")
          .isEqualTo(4002);
      verify(alertLogMapper, never()).updateById(any(AlertLog.class));
    }

    @Test
    @DisplayName("版本冲突：抛出 BusinessException(4003)")
    void shouldThrowOnVersionConflictForConfirm() {
      AlertLog alert = newAlertLog(1L, "未确认", 2); // DB version is 2
      when(alertLogMapper.selectById(1L)).thenReturn(alert);
      // client sends version 1 (stale)

      assertThatThrownBy(() -> alertLogService.confirmAlert(1L, 1))
          .isInstanceOf(BusinessException.class)
          .extracting("code")
          .isEqualTo(4003);
      verify(alertLogMapper, never()).updateById(any(AlertLog.class));
    }
  }

  // ============ resolveAlert ============

  @Nested
  @DisplayName("resolveAlert — 处置告警")
  class ResolveAlertTests {

    @Test
    @DisplayName("正常处置：状态从已确认→已处置，版本+1，备注写入")
    void shouldResolveWhenConfirmedAndVersionMatches() {
      AlertLog alert = newAlertLog(1L, "已确认", 1);
      when(alertLogMapper.selectById(1L)).thenReturn(alert);

      alertLogService.resolveAlert(1L, 1, "已安排人员处理");

      assertThat(alert.getConfirmStatus()).isEqualTo("已处置");
      assertThat(alert.getVersion()).isEqualTo(2);
      assertThat(alert.getRemark()).isEqualTo("已安排人员处理");
      verify(alertLogMapper).updateById(alert);
    }

    @Test
    @DisplayName("告警不存在：抛出 BusinessException(4001)")
    void shouldThrowWhenAlertNotFoundForResolve() {
      when(alertLogMapper.selectById(999L)).thenReturn(null);

      assertThatThrownBy(() -> alertLogService.resolveAlert(999L, 0, "备注"))
          .isInstanceOf(BusinessException.class)
          .extracting("code")
          .isEqualTo(4001);
      verify(alertLogMapper, never()).updateById(any(AlertLog.class));
    }

    @Test
    @DisplayName("未确认状态：抛出 BusinessException(4002)")
    void shouldThrowWhenNotYetConfirmed() {
      AlertLog alert = newAlertLog(1L, "未确认", 0);
      when(alertLogMapper.selectById(1L)).thenReturn(alert);

      assertThatThrownBy(() -> alertLogService.resolveAlert(1L, 0, "备注"))
          .isInstanceOf(BusinessException.class)
          .extracting("code")
          .isEqualTo(4002);
      verify(alertLogMapper, never()).updateById(any(AlertLog.class));
    }

    @Test
    @DisplayName("版本冲突：抛出 BusinessException(4003)")
    void shouldThrowOnVersionConflictForResolve() {
      AlertLog alert = newAlertLog(1L, "已确认", 3); // DB version is 3
      when(alertLogMapper.selectById(1L)).thenReturn(alert);
      // client sends version 2 (stale)

      assertThatThrownBy(() -> alertLogService.resolveAlert(1L, 2, "备注"))
          .isInstanceOf(BusinessException.class)
          .extracting("code")
          .isEqualTo(4003);
      verify(alertLogMapper, never()).updateById(any(AlertLog.class));
    }
  }
}
