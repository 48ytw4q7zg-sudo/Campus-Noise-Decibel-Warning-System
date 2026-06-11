<!-- views/ThresholdConfigPage.vue · 阈值规则配置页面 -->
<template>
  <div class="threshold-config-page">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <el-button v-if="isAdmin" type="warning" @click="handleReload" :loading="reloadLoading">
        重载规则配置
      </el-button>
    </div>

    <!-- 主卡片 + Tab 切换 -->
    <el-card class="main-card" shadow="hover">
      <template #header>
        <div class="card-header-row">
          <span>阈值规则配置</span>
          <el-button v-if="isAdmin && isLocationTab" type="primary" @click="handleAdd">新增规则</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ===== 功能区 Tab（图书馆/食堂/操场/宿舍） ===== -->
        <el-tab-pane v-for="loc in locations" :key="loc" :label="loc" :name="loc" />

        <!-- ===== Tab: 自适应阈值 ===== -->
        <el-tab-pane label="自适应阈值" name="adaptive">
          <div class="tab-toolbar">
            <el-button type="primary" @click="fetchAdaptive" :loading="adaptiveLoading">
              刷新数据
            </el-button>
          </div>
          <div v-loading="adaptiveLoading">
            <div v-if="adaptiveData.length > 0" class="adaptive-cards-grid">
              <el-card v-for="item in adaptiveData" :key="item.location" class="adaptive-card" shadow="hover">
                <div class="adaptive-card-title">{{ item.location }}</div>
                <el-divider />
                <div class="adaptive-params">
                  <div class="param-row">
                    <span class="param-label">窗口大小</span>
                    <span class="param-value">{{ item.windowSize ?? '--' }}</span>
                  </div>
                  <div class="param-row">
                    <span class="param-label">K 值</span>
                    <span class="param-value">{{ formatNumber(item.kValue, 1) }}</span>
                  </div>
                  <div class="param-row">
                    <span class="param-label">均值</span>
                    <span class="param-value">{{ formatNumber(item.mean, 1) }} dB</span>
                  </div>
                  <div class="param-row">
                    <span class="param-label">标准差</span>
                    <span class="param-value">{{ formatNumber(item.stdDev, 1) }}</span>
                  </div>
                  <div class="param-row">
                    <span class="param-label">上限</span>
                    <span class="param-value text-red">{{ formatNumber(item.upperLimit, 1) }} dB</span>
                  </div>
                  <div class="param-row">
                    <span class="param-label">下限</span>
                    <span class="param-value text-blue">{{ formatNumber(item.lowerLimit, 1) }} dB</span>
                  </div>
                  <div class="param-row">
                    <span class="param-label">窗口记录数</span>
                    <span class="param-value">{{ item.windowRecordCount ?? '--' }}</span>
                  </div>
                </div>
              </el-card>
            </div>
            <div v-else-if="!adaptiveLoading" class="empty-hint">暂无自适应阈值数据</div>
          </div>
        </el-tab-pane>

        <!-- ===== Tab: 混合模型 ===== -->
        <el-tab-pane label="混合模型" name="hybrid">
          <div v-loading="hybridStatusLoading || hybridPerfLoading">
            <!-- 运行状态卡片 -->
            <el-card class="hybrid-status-card" shadow="hover" v-if="hybridStatus">
              <template #header>
                <span class="section-title">模型运行状态</span>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="当前模式">
                  <el-tag :type="modeTagType(hybridStatus.currentMode)" size="small">
                    {{ modeLabel(hybridStatus.currentMode) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="是否触发切换">
                  <el-tag :type="hybridStatus.isTriggered ? 'warning' : 'success'" size="small">
                    {{ hybridStatus.isTriggered ? '已触发' : '未触发' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="3窗口异常率" :span="2">
                  <div class="abnormal-rate-bar">
                    <el-progress
                      :percentage="abnormalRatePercent"
                      :color="abnormalRateColor"
                      :stroke-width="16"
                    >
                      <span class="progress-text">{{ abnormalRatePercent }}%</span>
                    </el-progress>
                  </div>
                </el-descriptions-item>
                <el-descriptions-item label="触发原因" :span="2" v-if="hybridStatus.triggerReason">
                  {{ hybridStatus.triggerReason }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>

            <!-- 性能指标卡片 -->
            <el-card class="hybrid-perf-card" shadow="hover" v-if="hybridPerformance">
              <template #header>
                <span class="section-title">模型性能指标</span>
              </template>
              <!-- 数字指标 -->
              <div class="perf-metrics-grid">
                <div class="perf-metric-item">
                  <div class="metric-value">{{ formatPercent(hybridPerformance.accuracy) }}</div>
                  <div class="metric-label">准确率</div>
                </div>
                <div class="perf-metric-item">
                  <div class="metric-value">{{ formatPercent(hybridPerformance.precision) }}</div>
                  <div class="metric-label">精确率</div>
                </div>
                <div class="perf-metric-item">
                  <div class="metric-value">{{ formatPercent(hybridPerformance.recall) }}</div>
                  <div class="metric-label">召回率</div>
                </div>
                <div class="perf-metric-item">
                  <div class="metric-value">{{ formatPercent(hybridPerformance.f1Score) }}</div>
                  <div class="metric-label">F1分数</div>
                </div>
                <div class="perf-metric-item">
                  <div class="metric-value text-warning">{{ formatPercent(hybridPerformance.falsePositiveRate) }}</div>
                  <div class="metric-label">误报率</div>
                </div>
              </div>
              <!-- 模式分布图表 -->
              <div v-if="hasModeDistribution" ref="hybridPieRef" class="hybrid-pie-chart"></div>
            </el-card>

            <div v-if="!hybridState && !hybridStatusLoading && !hybridPerfLoading" class="empty-hint">
              暂无混合模型数据
            </div>
          </div>
        </el-tab-pane>

        <!-- ===== Tab: 参数配置（仅管理员） ===== -->
        <el-tab-pane v-if="isAdmin" label="参数配置" name="config">
          <div v-loading="configLoading">
            <el-table :data="configTableData" stripe border style="width: 100%">
              <el-table-column prop="location" label="功能区" width="120" align="center" />
              <el-table-column prop="windowSize" label="窗口大小" width="120" align="center" />
              <el-table-column prop="kValue" label="K 值" width="120" align="center">
                <template #default="{ row }">
                  {{ formatNumber(row.kValue, 1) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="handleConfigEdit(row)">
                    编辑
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!configLoading && configTableData.length === 0" class="empty-hint">
              暂无参数配置数据
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- ===== 功能区规则表格（仅功能区 Tab 可见） ===== -->
      <template v-if="isLocationTab">
        <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" align="center" />
          <el-table-column prop="timeSegment" label="时段" width="150" align="center" />
          <el-table-column prop="thresholdValue" label="阈值(dB)" width="120" align="center" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                :disabled="!isAdmin"
                @change="handleStatusChange(row)"
              />
            </template>
          </el-table-column>
          <el-table-column v-if="isAdmin" label="操作" width="160" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!loading && tableData.length === 0" class="empty-hint">暂无阈值规则</div>
      </template>
    </el-card>

    <!-- 新增/编辑规则弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
      @closed="dialogFormRef?.resetFields()"
    >
      <el-form :model="dialogForm" :rules="dialogRules" ref="dialogFormRef" label-width="100px">
        <el-form-item label="功能区" v-if="!isEdit">
          <el-select v-model="dialogForm.location" placeholder="请选择功能区" style="width: 100%">
            <el-option v-for="loc in locations" :key="loc" :label="loc" :value="loc" />
          </el-select>
        </el-form-item>
        <el-form-item label="时段" prop="timeSegment">
          <el-select v-model="dialogForm.timeSegment" placeholder="请选择时段" style="width: 100%">
            <el-option
              v-for="ts in timeSegments"
              :key="ts.value"
              :label="ts.label"
              :value="ts.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值(dB)" prop="thresholdValue">
          <el-input-number
            v-model="dialogForm.thresholdValue"
            :min="20"
            :max="120"
            :step="0.5"
            :precision="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="dialogForm.description"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-radio-group v-model="dialogForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDialogSubmit" :loading="dialogLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 参数配置编辑弹窗 -->
    <el-dialog
      v-model="configDialogVisible"
      title="编辑自适应参数"
      width="450px"
      destroy-on-close
      @closed="configDialogFormRef?.resetFields()"
    >
      <el-form :model="configDialogForm" :rules="configDialogRules" ref="configDialogFormRef" label-width="120px">
        <el-form-item label="功能区">
          <el-input :model-value="configDialogForm.location" disabled />
        </el-form-item>
        <el-form-item label="窗口大小" prop="windowSize">
          <el-input-number
            v-model="configDialogForm.windowSize"
            :min="5"
            :max="200"
            :step="5"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="K 值" prop="kValue">
          <el-input-number
            v-model="configDialogForm.kValue"
            :min="0.5"
            :max="5.0"
            :step="0.1"
            :precision="1"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfigSubmit" :loading="configDialogLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as echarts from 'echarts';
import { useUserStore } from '@/stores/user';
import {
  getThresholdRules,
  createThresholdRule,
  updateThresholdRule,
  deleteThresholdRule,
  reloadThresholdRules,
  getAdaptiveThreshold,
  updateAdaptiveConfig,
  getHybridStatus,
  getHybridPerformance
} from '@/api/threshold';

const userStore = useUserStore();
const isAdmin = computed(() => userStore.role === '管理员');

// ===== 功能区 & 时段 =====
const locations = ['图书馆', '食堂', '操场', '宿舍'];

const timeSegments = [
  { label: '早间时段 (06:00-08:00)', value: '早间时段' },
  { label: '上午时段 (08:00-12:00)', value: '上午时段' },
  { label: '下午时段 (12:00-18:00)', value: '下午时段' },
  { label: '晚间时段 (18:00-22:00)', value: '晚间时段' },
  { label: '夜间时段 (22:00-06:00)', value: '夜间时段' }
];

// ===== Tab 管理 =====
const activeTab = ref('图书馆');

// 是否在功能区 Tab（显示规则表格）
const isLocationTab = computed(() => locations.includes(activeTab.value));

// ===== 功能区规则表格 =====
const tableData = ref([]);
const loading = ref(false);
const reloadLoading = ref(false);

const fetchRules = async () => {
  loading.value = true;
  try {
    const res = await getThresholdRules({ location: activeTab.value });
    tableData.value = res.data || [];
  } catch (e) {
    tableData.value = [];
  } finally {
    loading.value = false;
  }
};

const handleReload = async () => {
  reloadLoading.value = true;
  try {
    const res = await reloadThresholdRules();
    ElMessage.success(res.message || '规则重载成功');
    fetchRules();
  } catch (e) {
    // 拦截器已处理
  } finally {
    reloadLoading.value = false;
  }
};

const handleStatusChange = async (row) => {
  try {
    await updateThresholdRule(row.id, {
      status: row.status,
      version: row.version
    });
    ElMessage.success('状态更新成功');
  } catch (e) {
    row.status = row.status === 1 ? 0 : 1;
  }
};

// ===== 新增/编辑规则弹窗 =====
const dialogVisible = ref(false);
const isEdit = ref(false);
const editingId = ref(null);
const dialogFormRef = ref(null);
const dialogLoading = ref(false);

const dialogForm = reactive({
  location: '',
  timeSegment: '',
  thresholdValue: null,
  description: '',
  status: 1,
  version: 0
});

const dialogRules = {
  location: [{ required: true, message: '请选择功能区', trigger: 'change' }],
  timeSegment: [{ required: true, message: '请选择时段', trigger: 'change' }],
  thresholdValue: [{ required: true, message: '请输入阈值', trigger: 'blur' }]
};

const dialogTitle = computed(() => (isEdit.value ? '编辑阈值规则' : '新增阈值规则'));

const handleAdd = () => {
  isEdit.value = false;
  editingId.value = null;
  dialogForm.location = activeTab.value;
  dialogForm.timeSegment = '';
  dialogForm.thresholdValue = null;
  dialogForm.description = '';
  dialogForm.status = 1;
  dialogForm.version = 0;
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  isEdit.value = true;
  editingId.value = row.id;
  dialogForm.location = row.location;
  dialogForm.timeSegment = row.timeSegment;
  dialogForm.thresholdValue = row.thresholdValue;
  dialogForm.description = row.description || '';
  dialogForm.status = row.status;
  dialogForm.version = row.version;
  dialogVisible.value = true;
};

const handleDialogSubmit = async () => {
  const valid = await dialogFormRef.value.validate().catch(() => false);
  if (!valid) return;

  dialogLoading.value = true;
  try {
    if (isEdit.value) {
      const res = await updateThresholdRule(editingId.value, {
        thresholdValue: dialogForm.thresholdValue,
        description: dialogForm.description,
        status: dialogForm.status,
        version: dialogForm.version
      });
      ElMessage.success(res.message || '更新成功');
    } else {
      const res = await createThresholdRule({
        location: dialogForm.location,
        timeSegment: dialogForm.timeSegment,
        thresholdValue: dialogForm.thresholdValue,
        description: dialogForm.description
      });
      ElMessage.success(res.message || '新增成功');
    }
    dialogVisible.value = false;
    fetchRules();
  } catch (e) {
    // 拦截器已处理
  } finally {
    dialogLoading.value = false;
  }
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.location} - ${row.timeSegment}」的阈值规则吗？`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    );
    await deleteThresholdRule(row.id);
    ElMessage.success('删除成功');
    fetchRules();
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      // 拦截器已处理
    }
  }
};

// ===== 格式化工具 =====
function formatNumber(val, digits) {
  if (val == null) return '--';
  return Number(val).toFixed(digits);
}

function formatPercent(val) {
  if (val == null) return '--';
  if (val > 1) return val.toFixed(1) + '%';
  return (val * 100).toFixed(1) + '%';
}

// ===== Tab: 自适应阈值 =====
const adaptiveData = ref([]);
const adaptiveLoading = ref(false);

const fetchAdaptive = async () => {
  adaptiveLoading.value = true;
  try {
    // 并行请求 4 个功能区自适应阈值
    const results = await Promise.allSettled(
      locations.map((loc) => getAdaptiveThreshold({ location: loc }))
    );
    adaptiveData.value = results
      .filter((r) => r.status === 'fulfilled' && r.value && r.value.data)
      .map((r) => r.value.data);
  } catch (e) {
    adaptiveData.value = [];
  } finally {
    adaptiveLoading.value = false;
  }
};

// ===== Tab: 混合模型 =====
const hybridStatus = ref(null);
const hybridStatusLoading = ref(false);
const hybridPerformance = ref(null);
const hybridPerfLoading = ref(false);
const hybridPieRef = ref(null);
let hybridPieInstance = null;

const hybridState = computed(() => hybridStatus.value || hybridPerformance.value);

// 异常率百分比（0-1 转 0-100）
const abnormalRatePercent = computed(() => {
  const val = hybridStatus.value?.abnormalRate3Windows;
  if (val == null) return 0;
  return val > 1 ? Math.round(val) : Math.round(val * 100);
});

// 异常率进度条颜色
const abnormalRateColor = computed(() => {
  const pct = abnormalRatePercent.value;
  if (pct > 30) return '#F56C6C';
  if (pct > 15) return '#E6A23C';
  return '#67C23A';
});

// 是否有模式分布数据
const hasModeDistribution = computed(() => {
  const dist = hybridPerformance.value?.modeDistribution;
  if (!dist) return false;
  if (Array.isArray(dist)) return dist.length > 0;
  return Object.keys(dist).length > 0;
});

// 模式显示标签
function modeLabel(mode) {
  const map = { STAT_ADAPTIVE: '统计自适应', RULE_BASED: '规则基础' };
  return map[mode] || mode || '--';
}

// 模式标签颜色
function modeTagType(mode) {
  const map = { STAT_ADAPTIVE: 'success', RULE_BASED: 'warning' };
  return map[mode] || 'info';
}

const fetchHybridStatus = async () => {
  hybridStatusLoading.value = true;
  try {
    const res = await getHybridStatus();
    hybridStatus.value = res.data || null;
  } catch (e) {
    hybridStatus.value = null;
  } finally {
    hybridStatusLoading.value = false;
  }
};

const fetchHybridPerformance = async () => {
  hybridPerfLoading.value = true;
  try {
    const res = await getHybridPerformance();
    hybridPerformance.value = res.data || null;
    await nextTick();
    renderHybridPie();
  } catch (e) {
    hybridPerformance.value = null;
  } finally {
    hybridPerfLoading.value = false;
  }
};

// 渲染模式分布饼图
const renderHybridPie = () => {
  if (!hybridPieRef.value) return;
  const dist = hybridPerformance.value?.modeDistribution;
  if (!dist) return;

  if (!hybridPieInstance) {
    hybridPieInstance = echarts.init(hybridPieRef.value);
  }

  let pieData;
  if (Array.isArray(dist)) {
    pieData = dist.map((d) => ({
      name: modeLabel(d.mode || d.name),
      value: d.percentage != null ? d.percentage : d.value
    }));
  } else {
    pieData = Object.entries(dist).map(([mode, val]) => ({
      name: modeLabel(mode),
      value: val
    }));
  }

  const option = {
    title: {
      text: '模式分布',
      left: 'center',
      textStyle: { fontSize: 14, fontWeight: 500 }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {d}%'
    },
    legend: {
      bottom: 0,
      textStyle: { fontSize: 12 }
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['50%', '48%'],
        avoidLabelOverlap: false,
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          fontSize: 11
        },
        emphasis: {
          label: { fontSize: 15, fontWeight: 'bold' }
        },
        data: pieData,
        color: ['#67C23A', '#E6A23C', '#409EFF', '#F56C6C']
      }
    ]
  };

  hybridPieInstance.setOption(option, true);
};

// ===== Tab: 参数配置（管理员） =====
const configTableData = ref([]);
const configLoading = ref(false);
const configDialogVisible = ref(false);
const configDialogFormRef = ref(null);
const configDialogLoading = ref(false);

const configDialogForm = reactive({
  location: '',
  windowSize: 30,
  kValue: 2.0
});

const configDialogRules = {
  windowSize: [
    { required: true, message: '请输入窗口大小', trigger: 'blur' },
    { type: 'number', min: 5, max: 200, message: '范围 5-200', trigger: 'blur' }
  ],
  kValue: [
    { required: true, message: '请输入 K 值', trigger: 'blur' },
    { type: 'number', min: 0.5, max: 5.0, message: '范围 0.5-5.0', trigger: 'blur' }
  ]
};

const fetchConfigData = async () => {
  configLoading.value = true;
  try {
    // 并行请求 4 个功能区自适应阈值
    const results = await Promise.allSettled(
      locations.map((loc) => getAdaptiveThreshold({ location: loc }))
    );
    configTableData.value = results
      .filter((r) => r.status === 'fulfilled' && r.value && r.value.data)
      .map((r) => r.value.data);
  } catch (e) {
    configTableData.value = [];
  } finally {
    configLoading.value = false;
  }
};

const handleConfigEdit = (row) => {
  configDialogForm.location = row.location;
  configDialogForm.windowSize = row.windowSize ?? 30;
  configDialogForm.kValue = row.kValue ?? 2.0;
  configDialogVisible.value = true;
};

const handleConfigSubmit = async () => {
  const valid = await configDialogFormRef.value.validate().catch(() => false);
  if (!valid) return;

  configDialogLoading.value = true;
  try {
    const res = await updateAdaptiveConfig({
      areaConfigs: [{
        location: configDialogForm.location,
        windowSize: configDialogForm.windowSize,
        kValue: configDialogForm.kValue
      }]
    });
    ElMessage.success(res.message || '参数更新成功');
    configDialogVisible.value = false;
    fetchConfigData();
  } catch (e) {
    // 拦截器已处理
  } finally {
    configDialogLoading.value = false;
  }
};

// ===== Tab 切换 =====
const handleTabChange = (tabName) => {
  if (locations.includes(tabName)) {
    fetchRules();
  } else if (tabName === 'adaptive') {
    if (adaptiveData.value.length === 0) fetchAdaptive();
  } else if (tabName === 'hybrid') {
    if (!hybridStatus.value) fetchHybridStatus();
    if (!hybridPerformance.value) fetchHybridPerformance();
  } else if (tabName === 'config') {
    if (configTableData.value.length === 0) fetchConfigData();
  }
};

// ===== 窗口 resize =====
const handleResize = () => {
  hybridPieInstance?.resize();
};

onMounted(() => {
  fetchRules();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  hybridPieInstance?.dispose();
});
</script>

<style scoped>
.threshold-config-page {
  padding: 16px;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.main-card {
  border-radius: 8px;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-hint {
  text-align: center;
  color: #909399;
  font-size: 14px;
  padding: 32px 0;
}

/* ===== 功能区通用 ===== */
.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

/* ===== 自适应阈值卡片 ===== */
.adaptive-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .adaptive-cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .adaptive-cards-grid {
    grid-template-columns: 1fr;
  }
}

.adaptive-card {
  text-align: center;
}

.adaptive-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.adaptive-params {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.param-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8px;
}

.param-label {
  font-size: 13px;
  color: #909399;
}

.param-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.param-value.text-red {
  color: #F56C6C;
}

.param-value.text-blue {
  color: #409EFF;
}

/* ===== 混合模型 ===== */
.hybrid-status-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.hybrid-perf-card {
  border-radius: 8px;
}

.abnormal-rate-bar {
  padding: 4px 0;
}

.progress-text {
  font-size: 12px;
  font-weight: 600;
}

/* 性能指标网格 */
.perf-metrics-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 992px) {
  .perf-metrics-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 640px) {
  .perf-metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.perf-metric-item {
  text-align: center;
  padding: 12px 8px;
  background: #F5F7FA;
  border-radius: 8px;
}

.metric-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.4;
}

.metric-value.text-warning {
  color: #E6A23C;
}

.metric-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 饼图 */
.hybrid-pie-chart {
  width: 100%;
  height: 320px;
}
</style>
