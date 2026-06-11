<!-- views/StatisticsPage.vue · 统计分析页面 -->
<template>
  <div class="statistics-page">
    <!-- 日期范围选择器 -->
    <el-card class="filter-card" shadow="hover">
      <el-form :model="filterForm" inline>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter" :loading="loading">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Tab 切换 -->
    <el-card class="main-card" shadow="hover">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- Tab 1: 时间序列图 -->
        <el-tab-pane label="时间序列分析" name="timeseries">
          <div class="tab-toolbar">
            <el-form-item label="功能区">
              <el-select v-model="tsLocation" placeholder="请选择功能区" style="width: 200px" @change="fetchTimeseries">
                <el-option label="图书馆" value="图书馆" />
                <el-option label="食堂" value="食堂" />
                <el-option label="操场" value="操场" />
                <el-option label="宿舍" value="宿舍" />
              </el-select>
            </el-form-item>
          </div>
          <div v-show="tsChartData.length > 0" ref="tsChartRef" class="chart-container"></div>
          <div v-show="tsChartData.length === 0 && !tsLoading" class="empty-hint">暂无时间序列数据，请选择功能区后查询</div>
          <div v-loading="tsLoading" class="chart-loading"></div>
        </el-tab-pane>

        <!-- Tab 2: 功能区汇总 -->
        <el-tab-pane label="功能区汇总" name="areas">
          <!-- 汇总卡片 -->
          <div v-if="areaCards.length > 0" class="area-cards-grid">
            <el-card v-for="item in areaCards" :key="item.location" class="area-stat-card" shadow="hover">
              <div class="area-stat-title">{{ item.location }}</div>
              <div class="area-stat-values">
                <div class="stat-item">
                  <span class="stat-label">平均分贝</span>
                  <span class="stat-value">{{ item.avgDecibel != null ? item.avgDecibel.toFixed(1) : '--' }} dB</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">异常率</span>
                  <span class="stat-value" :class="{ 'text-danger': item.abnormalRate > 20 }">
                    {{ item.abnormalRate != null ? (item.abnormalRate * 100).toFixed(1) + '%' : '--' }}
                  </span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">告警次数</span>
                  <span class="stat-value">{{ item.alertCount ?? '--' }}</span>
                </div>
              </div>
            </el-card>
          </div>
          <!-- 柱状图 -->
          <div v-show="areaChartData.length > 0" ref="areaChartRef" class="chart-container"></div>
          <div v-show="areaChartData.length === 0 && !areaLoading" class="empty-hint">暂无功能区统计数据</div>
          <div v-loading="areaLoading" class="chart-loading"></div>
        </el-tab-pane>

        <!-- Tab 3: 模型性能对比 -->
        <el-tab-pane label="模型性能对比" name="models">
          <!-- 混合模型信息框 -->
          <el-alert
            type="success"
            :closable="false"
            show-icon
            class="model-info-alert"
          >
            <template #title>
              <span class="alert-title">ccswitch 混合动态阈值模型 — 准确率 92.6%（较固定阈值 78.0% 提升 14.6%）</span>
            </template>
            <div class="alert-body">
              正常时段使用统计自适应（μ±k×σ）滑动窗口计算动态阈值；特殊时段（考试周/午休/夜间静校）或连续3个窗口异常率>10%时自动回退业务规则增强判断。混合模型融合两者优势，误报率仅4.0%。
            </div>
          </el-alert>

          <!-- 模式分布实时统计 -->
          <el-card class="mode-dist-card" shadow="never">
            <div class="mode-dist-header">
              <span class="mode-dist-title">实时模型判定分布</span>
              <span class="mode-dist-total" v-if="hybridPerf">截止目前共判定 {{ hybridPerf.totalJudged || 0 }} 条记录</span>
            </div>
            <div class="mode-dist-bars" v-if="hybridPerf && hybridPerf.modeDistribution">
              <div class="mode-bar-item">
                <div class="mode-bar-label">RULE_BASED 业务规则</div>
                <div class="mode-bar-track">
                  <div class="mode-bar-fill rule-fill" :style="{ width: rulePercent + '%' }"></div>
                </div>
                <span class="mode-bar-count">{{ hybridPerf.modeDistribution.RULE_BASED || 0 }}</span>
              </div>
              <div class="mode-bar-item">
                <div class="mode-bar-label">STAT_ADAPTIVE 统计自适应</div>
                <div class="mode-bar-track">
                  <div class="mode-bar-fill adaptive-fill" :style="{ width: adaptivePercent + '%' }"></div>
                </div>
                <span class="mode-bar-count">{{ hybridPerf.modeDistribution.ADAPTIVE || 0 }}</span>
              </div>
              <div class="mode-bar-item">
                <div class="mode-bar-label">HYBRID 混合模型</div>
                <div class="mode-bar-track">
                  <div class="mode-bar-fill hybrid-fill" :style="{ width: hybridPercent + '%' }"></div>
                </div>
                <span class="mode-bar-count">{{ hybridPerf.modeDistribution.HYBRID || 0 }}</span>
              </div>
            </div>
            <div v-else class="empty-hint-small">暂无判定分布数据</div>
          </el-card>

          <div v-show="modelChartData.length > 0" class="charts-row">
            <div ref="radarChartRef" class="chart-container chart-half"></div>
            <div ref="barChartRef" class="chart-container chart-half"></div>
          </div>
          <div v-show="modelChartData.length === 0 && !modelLoading" class="empty-hint">暂无模型性能数据</div>
          <div v-loading="modelLoading" class="chart-loading"></div>
        </el-tab-pane>

        <!-- Tab 4: 热力图 -->
        <el-tab-pane label="热力图" name="heatmap">
          <div v-show="heatmapData" ref="heatmapChartRef" class="chart-container"></div>
          <div v-show="!heatmapData && !heatmapLoading" class="empty-hint">暂无热力图数据，请尝试调整日期范围</div>
          <div v-loading="heatmapLoading" class="chart-loading"></div>
        </el-tab-pane>

        <!-- Tab 5: 雷达图（多维度） -->
        <el-tab-pane label="多维度雷达图" name="radar">
          <div v-show="radarData.length > 0" ref="multiRadarChartRef" class="chart-container"></div>
          <div v-show="radarData.length === 0 && !radarLoading" class="empty-hint">暂无雷达图数据，请尝试调整日期范围</div>
          <div v-loading="radarLoading" class="chart-loading"></div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import * as echarts from 'echarts';
import { useUserStore } from '@/stores/user';
import { getTimeseries, getAreaStats, getModelPerformance, getHeatmap, getRadar } from '@/api/statistics';
import { getHybridPerformance } from '@/api/threshold';

const userStore = useUserStore();
const isAdmin = computed(() => userStore.role === '管理员');

// 日期筛选
const filterForm = reactive({
  dateRange: null
});
const loading = ref(false);

// 默认日期范围：最近 7 天
const initDateRange = () => {
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - 7);
  const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  filterForm.dateRange = [fmt(start), fmt(end)];
};
initDateRange();

// 获取日期参数
const getDateParams = () => {
  const params = {};
  if (filterForm.dateRange && filterForm.dateRange.length === 2) {
    params.dateFrom = filterForm.dateRange[0];
    params.dateTo = filterForm.dateRange[1];
  }
  return params;
};

// 当前激活 Tab
const activeTab = ref('timeseries');

// ===== Tab 1: 时间序列图 =====
const tsLocation = ref('图书馆');
const tsChartRef = ref(null);
let tsChartInstance = null;
const tsChartData = ref([]);
const tsLoading = ref(false);

const fetchTimeseries = async () => {
  if (!tsLocation.value) return;
  tsLoading.value = true;
  try {
    const params = { ...getDateParams(), location: tsLocation.value };
    const res = await getTimeseries(params);
    tsChartData.value = res.data?.points || [];
    await nextTick();
    renderTimeseriesChart();
  } catch (e) {
    tsChartData.value = [];
  } finally {
    tsLoading.value = false;
  }
};

const renderTimeseriesChart = () => {
  if (!tsChartRef.value) return;
  if (!tsChartInstance) {
    tsChartInstance = echarts.init(tsChartRef.value);
  }

  const data = tsChartData.value;
  const xData = data.map((d) => {
    const t = new Date(d.timePoint);
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(t.getMonth() + 1)}-${pad(t.getDate())} ${pad(t.getHours())}:${pad(t.getMinutes())}`;
  });

  // 正常点和异常点分开
  const normalData = data.map((d) => (d.isAbnormal ? null : d.decibel));
  const abnormalData = data.map((d) => (d.isAbnormal ? d.decibel : null));

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const items = params.filter((p) => p.value != null);
        if (items.length === 0) return '';
        let html = `<div style="font-weight:600">${items[0].axisValue}</div>`;
        items.forEach((p) => {
          html += `<div style="display:flex;align-items:center;gap:6px;margin-top:4px">
            ${p.marker} ${p.seriesName}: <strong>${p.value} dB</strong></div>`;
        });
        return html;
      }
    },
    legend: {
      data: ['正常分贝', '异常分贝'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: {
        rotate: 45,
        fontSize: 11,
        formatter: (v) => v.slice(0, 8) // 截短显示
      }
    },
    yAxis: {
      type: 'value',
      name: '分贝 (dB)',
      min: 20,
      max: 120
    },
    dataZoom: [
      { type: 'inside', start: 0, end: 100 },
      { type: 'slider', start: 0, end: 100, height: 20, bottom: 0 }
    ],
    series: [
      {
        name: '正常分贝',
        type: 'line',
        data: normalData,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: '#409EFF', width: 2 },
        connectNulls: false
      },
      {
        name: '异常分贝',
        type: 'scatter',
        data: abnormalData,
        symbolSize: 8,
        itemStyle: { color: '#F56C6C' }
      }
    ]
  };

  tsChartInstance.setOption(option, true);
};

// ===== Tab 2: 功能区汇总 =====
const areaChartRef = ref(null);
let areaChartInstance = null;
const areaChartData = ref([]);
const areaCards = ref([]);
const areaLoading = ref(false);

const fetchAreaStats = async () => {
  areaLoading.value = true;
  try {
    const res = await getAreaStats(getDateParams());
    const areas = res.data?.areas || [];
    areaChartData.value = areas;
    areaCards.value = areas;
    await nextTick();
    renderAreaChart();
  } catch (e) {
    areaChartData.value = [];
    areaCards.value = [];
  } finally {
    areaLoading.value = false;
  }
};

const renderAreaChart = () => {
  if (!areaChartRef.value) return;
  if (!areaChartInstance) {
    areaChartInstance = echarts.init(areaChartRef.value);
  }

  const data = areaChartData.value;
  const locations = data.map((d) => d.location);
  const avgDecibel = data.map((d) => (d.avgDecibel != null ? Number(d.avgDecibel.toFixed(1)) : 0));
  const abnormalRate = data.map((d) =>
    d.abnormalRate != null ? Number((d.abnormalRate * 100).toFixed(1)) : 0
  );

  const option = {
    title: {
      text: '各功能区平均分贝与异常率',
      left: 'center',
      textStyle: { fontSize: 15, fontWeight: 500 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['平均分贝 (dB)', '异常率 (%)'],
      top: 30
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 70,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: locations
    },
    yAxis: [
      {
        type: 'value',
        name: '分贝 (dB)',
        min: 0,
        max: 100
      },
      {
        type: 'value',
        name: '异常率 (%)',
        min: 0,
        max: 100
      }
    ],
    series: [
      {
        name: '平均分贝 (dB)',
        type: 'bar',
        data: avgDecibel,
        itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 50
      },
      {
        name: '异常率 (%)',
        type: 'line',
        yAxisIndex: 1,
        data: abnormalRate,
        symbol: 'circle',
        symbolSize: 10,
        lineStyle: { color: '#F56C6C', width: 2 },
        itemStyle: { color: '#F56C6C' }
      }
    ]
  };

  areaChartInstance.setOption(option, true);
};

// ===== Tab 3: 模型性能对比 =====
const radarChartRef = ref(null);
const barChartRef = ref(null);
let radarChartInstance = null;
let barChartInstance = null;
const modelChartData = ref([]);
const modelLoading = ref(false);
const hybridPerf = ref(null);

// 模型分布百分比
const totalJudged = computed(() => {
  if (!hybridPerf.value || !hybridPerf.value.modeDistribution) return 1;
  const d = hybridPerf.value.modeDistribution;
  return (d.RULE_BASED || 0) + (d.ADAPTIVE || 0) + (d.HYBRID || 0) || 1;
});
const rulePercent = computed(() =>
  hybridPerf.value && hybridPerf.value.modeDistribution
    ? ((hybridPerf.value.modeDistribution.RULE_BASED || 0) / totalJudged.value * 100).toFixed(0)
    : 0
);
const adaptivePercent = computed(() =>
  hybridPerf.value && hybridPerf.value.modeDistribution
    ? ((hybridPerf.value.modeDistribution.ADAPTIVE || 0) / totalJudged.value * 100).toFixed(0)
    : 0
);
const hybridPercent = computed(() =>
  hybridPerf.value && hybridPerf.value.modeDistribution
    ? ((hybridPerf.value.modeDistribution.HYBRID || 0) / totalJudged.value * 100).toFixed(0)
    : 0
);

const fetchModelPerformance = async () => {
  modelLoading.value = true;
  try {
    const res = await getModelPerformance();
    modelChartData.value = res.data?.models || [];
    await nextTick();
    renderModelCharts();
  } catch (e) {
    modelChartData.value = [];
  } finally {
    modelLoading.value = false;
  }
};

const fetchHybridPerf = async () => {
  try {
    const res = await getHybridPerformance();
    hybridPerf.value = res.data;
  } catch {
    hybridPerf.value = null;
  }
};

const renderModelCharts = () => {
  const data = modelChartData.value;
  if (data.length === 0) return;

  const modelNames = data.map((d) => d.modelName);
  const metrics = ['accuracy', 'precision', 'recall', 'f1Score'];

  // 雷达图：4 指标
  if (radarChartRef.value) {
    if (!radarChartInstance) {
      radarChartInstance = echarts.init(radarChartRef.value);
    }

    const indicatorMap = {
      accuracy: { name: '准确率', max: 1 },
      precision: { name: '精确率', max: 1 },
      recall: { name: '召回率', max: 1 },
      f1Score: { name: 'F1 分数', max: 1 }
    };

    const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C'];

    const option = {
      title: { text: '模型性能雷达图', left: 'center', textStyle: { fontSize: 15, fontWeight: 500 } },
      tooltip: {},
      legend: {
        data: modelNames,
        bottom: 0
      },
      radar: {
        indicator: metrics.map((m) => indicatorMap[m]),
        center: ['50%', '50%'],
        radius: '60%'
      },
      series: [
        {
          type: 'radar',
          data: data.map((d, i) => ({
            name: d.modelName,
            value: metrics.map((m) => (d[m] != null ? Number(d[m].toFixed(4)) : 0)),
            lineStyle: { color: colors[i % colors.length] },
            areaStyle: { color: colors[i % colors.length], opacity: 0.1 },
            itemStyle: { color: colors[i % colors.length] }
          }))
        }
      ]
    };

    radarChartInstance.setOption(option, true);
  }

  // 柱状图：5 指标并排对比（含 FPR）
  if (barChartRef.value) {
    if (!barChartInstance) {
      barChartInstance = echarts.init(barChartRef.value);
    }

    const allMetrics = ['accuracy', 'precision', 'recall', 'f1Score', 'fpr'];
    const allMetricLabels = ['准确率', '精确率', '召回率', 'F1', '误报率'];

    const option = {
      title: { text: '模型性能柱状对比', left: 'center', textStyle: { fontSize: 15, fontWeight: 500 } },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      legend: {
        data: modelNames,
        top: 30
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: 70,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: allMetricLabels
      },
      yAxis: {
        type: 'value',
        name: '数值',
        min: 0,
        max: 1
      },
      series: modelNames.map((name, i) => ({
        name,
        type: 'bar',
        data: allMetrics.map((m) => {
          const val = data[i]?.[m];
          return val != null ? Number(val.toFixed(4)) : 0;
        }),
        barMaxWidth: 50
      }))
    };

    barChartInstance.setOption(option, true);
  }
};

// ===== Tab 4: 热力图 =====
const heatmapChartRef = ref(null);
let heatmapChartInstance = null;
const heatmapData = ref(null);
const heatmapLoading = ref(false);

const fetchHeatmap = async () => {
  heatmapLoading.value = true;
  try {
    const res = await getHeatmap(getDateParams());
    heatmapData.value = res.data || null;
    await nextTick();
    renderHeatmapChart();
  } catch (e) {
    heatmapData.value = null;
  } finally {
    heatmapLoading.value = false;
  }
};

const renderHeatmapChart = () => {
  if (!heatmapChartRef.value || !heatmapData.value) return;
  if (!heatmapChartInstance) {
    heatmapChartInstance = echarts.init(heatmapChartRef.value);
  }

  const { xLabels = [], yLabels = [], data = [] } = heatmapData.value;

  const option = {
    title: {
      text: '功能区 x 时段 噪声热力图',
      left: 'center',
      textStyle: { fontSize: 15, fontWeight: 500 }
    },
    tooltip: {
      position: 'top',
      formatter: (p) => {
        if (!p.value || p.value[2] == null) return '';
        return `${xLabels[p.value[0]]} · ${yLabels[p.value[1]]}<br/>
          <strong>${Number(p.value[2]).toFixed(1)} dB</strong>`;
      }
    },
    grid: {
      left: '10%',
      right: '8%',
      top: 50,
      bottom: 40
    },
    xAxis: {
      type: 'category',
      data: xLabels,
      splitArea: { show: true },
      axisLabel: { rotate: 30, fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: yLabels,
      splitArea: { show: true }
    },
    visualMap: {
      min: 20,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: {
        color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C']
      },
      text: ['高', '低'],
      textStyle: { fontSize: 11 }
    },
    series: [
      {
        type: 'heatmap',
        data: data,
        label: {
          show: true,
          formatter: (p) => {
            const v = p.value?.[2];
            return v != null ? v.toFixed(1) : '';
          },
          fontSize: 10
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0,0,0,0.5)'
          }
        }
      }
    ]
  };

  heatmapChartInstance.setOption(option, true);
};

// ===== Tab 5: 雷达图（多维度） =====
const multiRadarChartRef = ref(null);
let multiRadarChartInstance = null;
const radarData = ref([]);
const radarLoading = ref(false);
const radarDimLabels = ref([]);

const fetchRadar = async () => {
  radarLoading.value = true;
  try {
    const res = await getRadar(getDateParams());
    const raw = res.data || {};
    // 兼容两种响应格式：数组 或 { locations:[], dimensionLabels:[] }
    if (Array.isArray(raw)) {
      radarData.value = raw;
      radarDimLabels.value = [];
    } else {
      radarData.value = raw.locations || raw.list || raw.items || [];
      radarDimLabels.value = raw.dimensionLabels || [];
    }
    await nextTick();
    renderMultiRadarChart();
  } catch (e) {
    radarData.value = [];
    radarDimLabels.value = [];
  } finally {
    radarLoading.value = false;
  }
};

const renderMultiRadarChart = () => {
  if (!multiRadarChartRef.value || radarData.value.length === 0) return;
  if (!multiRadarChartInstance) {
    multiRadarChartInstance = echarts.init(multiRadarChartRef.value);
  }

  const locations = radarData.value;
  const dimLabels = radarDimLabels.value.length > 0
    ? radarDimLabels.value
    : (locations[0]?.dimensions?.map((d) => d.name) || []);

  if (dimLabels.length === 0) return;

  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C'];

  const indicator = dimLabels.map((name) => ({ name, max: 100 }));

  const option = {
    title: {
      text: '功能区多维度雷达图',
      left: 'center',
      textStyle: { fontSize: 15, fontWeight: 500 }
    },
    tooltip: {},
    legend: {
      data: locations.map((d) => d.location),
      bottom: 0
    },
    radar: {
      indicator,
      center: ['50%', '52%'],
      radius: '62%'
    },
    series: [
      {
        type: 'radar',
        data: locations.map((item, i) => ({
          name: item.location,
          value: dimLabels.map((label) => {
            const dim = item.dimensions?.find((d) => d.name === label);
            return dim?.value != null ? Number(dim.value.toFixed(1)) : 0;
          }),
          lineStyle: { color: colors[i % colors.length] },
          areaStyle: { color: colors[i % colors.length], opacity: 0.1 },
          itemStyle: { color: colors[i % colors.length] }
        }))
      }
    ]
  };

  multiRadarChartInstance.setOption(option, true);
};

// ===== Tab 切换触发首次加载 =====
const handleTabChange = (tabName) => {
  nextTick(() => {
    switch (tabName) {
      case 'timeseries':
        if (tsChartData.value.length === 0) fetchTimeseries();
        break;
      case 'areas':
        if (areaChartData.value.length === 0) fetchAreaStats();
        break;
      case 'models':
        if (modelChartData.value.length === 0) fetchModelPerformance();
        fetchHybridPerf();
        break;
      case 'heatmap':
        if (!heatmapData.value) fetchHeatmap();
        break;
      case 'radar':
        if (radarData.value.length === 0) fetchRadar();
        break;
    }
  });
};

// 筛选按钮
const handleFilter = () => {
  if (activeTab.value === 'timeseries') fetchTimeseries();
  else if (activeTab.value === 'areas') fetchAreaStats();
  else if (activeTab.value === 'heatmap') fetchHeatmap();
  else if (activeTab.value === 'radar') fetchRadar();
};

// ===== 窗口 resize =====
const handleResize = () => {
  tsChartInstance?.resize();
  areaChartInstance?.resize();
  radarChartInstance?.resize();
  barChartInstance?.resize();
  heatmapChartInstance?.resize();
  multiRadarChartInstance?.resize();
};

// ===== 监听图表容器变化 =====
watch([tsChartData, areaChartData, modelChartData], () => {
  // 数据变化后延迟 resize 确保渲染完整
  nextTick(() => setTimeout(handleResize, 100));
});

onMounted(() => {
  fetchTimeseries();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  tsChartInstance?.dispose();
  areaChartInstance?.dispose();
  radarChartInstance?.dispose();
  barChartInstance?.dispose();
  heatmapChartInstance?.dispose();
  multiRadarChartInstance?.dispose();
});
</script>

<style scoped>
.statistics-page {
  padding: 16px;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.filter-card :deep(.el-form) {
  margin-bottom: 0;
}

.main-card {
  border-radius: 8px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.tab-toolbar :deep(.el-form-item) {
  margin-bottom: 0;
}

.chart-container {
  width: 100%;
  height: 420px;
}

.charts-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.chart-half {
  flex: 1;
  min-width: 360px;
  height: 420px;
}

.chart-loading {
  height: 200px;
}

.empty-hint {
  text-align: center;
  color: #909399;
  font-size: 14px;
  padding: 48px 0;
}

/* 功能区汇总卡片 */
.area-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 1200px) {
  .area-cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .area-cards-grid {
    grid-template-columns: 1fr;
  }
  .chart-half {
    flex: 1 1 100%;
  }
}

.area-stat-card {
  text-align: center;
}

.area-stat-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.area-stat-values {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.stat-value.text-danger {
  color: #F56C6C;
}

/* 模型信息提示框 */
.model-info-alert {
  margin-bottom: 16px;
  border-radius: 8px;
}
.model-info-alert :deep(.el-alert__title) {
  font-size: 15px;
  font-weight: 600;
}
.alert-body {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}

/* 模型分布卡片 */
.mode-dist-card {
  margin-bottom: 16px;
  border-radius: 8px;
}
.mode-dist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.mode-dist-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.mode-dist-total {
  font-size: 12px;
  color: #909399;
}
.mode-dist-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mode-bar-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mode-bar-label {
  font-size: 12px;
  color: #606266;
  min-width: 160px;
}
.mode-bar-track {
  flex: 1;
  height: 16px;
  background: #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}
.mode-bar-fill {
  height: 100%;
  border-radius: 8px;
  transition: width 0.6s ease;
}
.rule-fill { background: linear-gradient(90deg, #E6A23C, #F7D06A); }
.adaptive-fill { background: linear-gradient(90deg, #409EFF, #79BBFF); }
.hybrid-fill { background: linear-gradient(90deg, #67C23A, #95D475); }
.mode-bar-count {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  min-width: 40px;
  text-align: right;
}
.empty-hint-small {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 16px 0;
}
</style>
