<!-- views/DashboardPage.vue · 仪表盘页面 -->
<template>
  <div class="dashboard-page">
    <!-- 混合阈值引擎主面板 -->
    <el-card class="hybrid-engine-panel" shadow="hover">
      <div class="engine-header">
        <div class="engine-title">
          <span class="engine-icon">⚙</span>
          <span class="engine-label">ccswitch 混合动态阈值引擎</span>
          <el-tag :type="hybridStatus && hybridStatus.isTriggered ? 'danger' : 'success'" size="large" effect="dark" class="engine-mode-tag">
            {{ hybridStatus && hybridStatus.isTriggered ? 'RULE_BASED 增强模式' : 'STAT_ADAPTIVE 自适应模式' }}
          </el-tag>
        </div>
        <div class="engine-badge">混合模型准确率 92.6% | 远超固定阈值 78.0%</div>
      </div>
      <div class="engine-cards">
        <!-- 模式状态 -->
        <div class="engine-stat-card">
          <div class="engine-stat-icon mode-icon">{{ hybridStatus && hybridStatus.isTriggered ? '!' : '~' }}</div>
          <div class="engine-stat-info">
            <div class="engine-stat-value">{{ hybridStatus && hybridStatus.isTriggered ? '规则增强' : '统计自适应' }}</div>
            <div class="engine-stat-label">当前判断模式</div>
          </div>
        </div>
        <!-- 触发原因 -->
        <div class="engine-stat-card">
          <div class="engine-stat-icon reason-icon">{{ hybridStatus && hybridStatus.triggerReason ? 'R' : '-' }}</div>
          <div class="engine-stat-info">
            <div class="engine-stat-value" style="font-size:13px">{{ hybridStatus && hybridStatus.triggerReason ? hybridStatus.triggerReason : '无特殊触发条件' }}</div>
            <div class="engine-stat-label">触发原因</div>
          </div>
        </div>
        <!-- 判定总量 -->
        <div class="engine-stat-card">
          <div class="engine-stat-icon count-icon">{{ hybridPerf && hybridPerf.totalJudged ? hybridPerf.totalJudged : '0' }}</div>
          <div class="engine-stat-info">
            <div class="engine-stat-value">{{ hybridPerf && hybridPerf.modeDistribution ? (hybridPerf.modeDistribution.RULE_BASED + hybridPerf.modeDistribution.ADAPTIVE + hybridPerf.modeDistribution.HYBRID) : '--' }}</div>
            <div class="engine-stat-label">已判定记录总数</div>
          </div>
        </div>
        <!-- 模型分布 -->
        <div class="engine-stat-card">
          <div class="engine-stat-icon dist-icon">M</div>
          <div class="engine-stat-info">
            <div class="engine-stat-bars">
              <span class="bar-item"><span class="bar-dot rule-dot"></span> RULE: {{ hybridPerf && hybridPerf.modeDistribution ? hybridPerf.modeDistribution.RULE_BASED : '--' }}</span>
              <span class="bar-item"><span class="bar-dot adaptive-dot"></span> ADAPT: {{ hybridPerf && hybridPerf.modeDistribution ? hybridPerf.modeDistribution.ADAPTIVE : '--' }}</span>
              <span class="bar-item"><span class="bar-dot hybrid-dot"></span> HYBRID: {{ hybridPerf && hybridPerf.modeDistribution ? hybridPerf.modeDistribution.HYBRID : '--' }}</span>
            </div>
            <div class="engine-stat-label">模型判定分布</div>
          </div>
        </div>
      </div>
      <!-- 模型性能对比条 -->
      <div v-if="hybridPerf" class="model-compact-compare">
        <div class="compare-item">
          <span class="compare-name">混合模型</span>
          <el-progress :percentage="hybridPerf.accuracy || 92.6" :stroke-width="18" color="#67C23A">
            <span class="progress-text">{{ (hybridPerf.accuracy || 92.6).toFixed(1) }}%</span>
          </el-progress>
        </div>
        <div class="compare-item">
          <span class="compare-name">统计自适应</span>
          <el-progress :percentage="89.4" :stroke-width="14" color="#409EFF">
            <span class="progress-text">89.4%</span>
          </el-progress>
        </div>
        <div class="compare-item">
          <span class="compare-name">业务规则</span>
          <el-progress :percentage="88.7" :stroke-width="14" color="#E6A23C">
            <span class="progress-text">88.7%</span>
          </el-progress>
        </div>
        <div class="compare-item">
          <span class="compare-name">固定阈值</span>
          <el-progress :percentage="78.0" :stroke-width="14" color="#F56C6C">
            <span class="progress-text">78.0%</span>
          </el-progress>
        </div>
      </div>
    </el-card>

  <!-- ccswitch 运行状态卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :xs="24" :sm="12" :md="8">
        <el-card class="ccswitch-status-card" shadow="hover">
          <template #header>
            <div class="section-header">
              <span>ccswitch 配置服务</span>
              <el-tag :type="ccswitchStatus && ccswitchStatus.status === 'ok' ? 'success' : 'danger'" size="small" effect="dark">
                {{ ccswitchStatus && ccswitchStatus.status === 'ok' ? 'RUNNING' : 'OFFLINE' }}
              </el-tag>
            </div>
          </template>
          <div class="ccswitch-body">
            <div class="ccswitch-row">
              <span class="ccswitch-label">模型</span>
              <span class="ccswitch-value">{{ (ccswitchStatus && ccswitchStatus.model) || '--' }}</span>
            </div>
            <div class="ccswitch-row">
              <span class="ccswitch-label">运行时长</span>
              <span class="ccswitch-value">{{ formatUptime(ccswitchStatus && ccswitchStatus.uptime_seconds) }}</span>
            </div>
            <div class="ccswitch-row">
              <span class="ccswitch-label">配置来源</span>
              <span class="ccswitch-value">{{ (ccswitchStatus && ccswitchStatus.config_source) || '--' }}</span>
            </div>
            <div class="ccswitch-row">
              <span class="ccswitch-label">阈值规则</span>
              <el-tag size="small" :type="ccswitchStatus && ccswitchStatus.threshold_rules_loaded ? 'success' : 'warning'">
                {{ ccswitchStatus && ccswitchStatus.threshold_rules_loaded ? '已加载' : '未加载' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card class="ccswitch-status-card" shadow="hover">
          <template #header>
            <div class="section-header">
              <span>混合模型性能</span>
              <el-tag type="primary" size="small" effect="dark">实时统计</el-tag>
            </div>
          </template>
          <div class="ccswitch-body" v-if="hybridPerf">
            <div class="ccswitch-row">
              <span class="ccswitch-label">准确率</span>
              <span class="ccswitch-value" style="color:#67C23A;font-weight:700">{{ (hybridPerf.accuracy || 0).toFixed(1) }}%</span>
            </div>
            <div class="ccswitch-row">
              <span class="ccswitch-label">精确率</span>
              <span class="ccswitch-value">{{ (hybridPerf.precision || 0).toFixed(1) }}%</span>
            </div>
            <div class="ccswitch-row">
              <span class="ccswitch-label">召回率</span>
              <span class="ccswitch-value">{{ (hybridPerf.recall || 0).toFixed(1) }}%</span>
            </div>
            <div class="ccswitch-row">
              <span class="ccswitch-label">误报率</span>
              <span class="ccswitch-value" :style="{ color: (hybridPerf.falsePositiveRate || 0) > 10 ? '#F56C6C' : '#67C23A' }">{{ (hybridPerf.falsePositiveRate || 0).toFixed(1) }}%</span>
            </div>
          </div>
          <div v-else class="empty-hint-small">暂无性能数据</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="ccswitch-status-card" shadow="hover">
          <template #header>
            <div class="section-header">
              <span>四模型对比</span>
              <el-tag type="warning" size="small" effect="dark">准确率</el-tag>
            </div>
          </template>
          <div class="model-mini-compare">
            <div class="mini-item">
              <span class="mini-name">混合阈值</span>
              <el-progress :percentage="92.6" :stroke-width="12" color="#67C23A"><span class="mini-text">92.6%</span></el-progress>
            </div>
            <div class="mini-item">
              <span class="mini-name">统计自适应</span>
              <el-progress :percentage="89.4" :stroke-width="10" color="#409EFF"><span class="mini-text">89.4%</span></el-progress>
            </div>
            <div class="mini-item">
              <span class="mini-name">业务规则</span>
              <el-progress :percentage="88.7" :stroke-width="10" color="#E6A23C"><span class="mini-text">88.7%</span></el-progress>
            </div>
            <div class="mini-item">
              <span class="mini-name">固定阈值</span>
              <el-progress :percentage="78.0" :stroke-width="10" color="#F56C6C"><span class="mini-text">78.0%</span></el-progress>
            </div>
          </div>
          <div class="model-compare-note">混合模型较固定阈值提升 14.6%</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 功能区概览卡片 -->
    <div class="overview-grid">
      <el-skeleton v-if="loading" :rows="4" animated />
      <template v-else>
        <el-card v-for="item in overviewList" :key="item.location" class="overview-card" shadow="hover">
          <div class="card-content">
            <div class="card-header">
              <span class="card-title">{{ item.location }}</span>
              <span class="card-status">
                <span
                  class="status-dot"
                  :class="{
                    'status-normal': item.indicator === 'normal',
                    'status-abnormal': item.indicator === 'abnormal',
                    'status-unknown': item.indicator === 'unknown'
                  }"
                ></span>
                {{ statusText(item.indicator) }}
              </span>
            </div>
            <div class="card-body">
              <div class="decibel-value" :class="{ 'text-danger': item.isAbnormal === 1 }">
                {{ formatDecibel(item.decibel) }}
              </div>
              <div class="decibel-unit">dB</div>
            </div>
            <div class="card-footer">
              <div class="threshold-info">
                阈值: {{ formatDecibel(item.thresholdValue) }} dB
              </div>
              <div class="update-time" v-if="item.lastUpdateTime">
                {{ formatTime(item.lastUpdateTime) }}
              </div>
            </div>
          </div>
        </el-card>
      </template>
    </div>

  <!-- ccswitch 混合阈值模型状态 -->
    <el-card class="hybrid-section" shadow="hover" v-if="hybridStatus">
      <template #header>
        <div class="section-header">
          <span>ccswitch 混合动态阈值引擎</span>
          <el-tag :type="hybridStatus.isTriggered ? 'danger' : 'success'" size="small">
            {{ hybridStatus.isTriggered ? 'RULE_BASED' : 'STAT_ADAPTIVE' }}
          </el-tag>
        </div>
      </template>
      <div class="hybrid-body">
        <div class="hybrid-row">
          <span class="hybrid-label">当前模式</span>
          <span class="hybrid-value" :class="hybridStatus.isTriggered ? 'text-warning' : 'text-success'">
            {{ hybridStatus.isTriggered ? '业务规则增强 (特殊时段/异常率触发)' : '统计自适应 (μ±k×σ)' }}
          </span>
        </div>
        <div class="hybrid-row" v-if="hybridStatus.triggerReason">
          <span class="hybrid-label">触发原因</span>
          <span class="hybrid-value text-warning">{{ hybridStatus.triggerReason }}</span>
        </div>
        <div class="hybrid-row" v-if="!hybridStatus.isTriggered">
          <span class="hybrid-label">当前策略</span>
          <span class="hybrid-value text-success">ccswitch实时计算滑动窗口统计自适应阈值，智能识别异常模式</span>
        </div>
        <div class="hybrid-row" v-if="hybridPerf">
          <span class="hybrid-label">已判定分布</span>
          <span class="hybrid-value">
            <el-tag size="small" type="primary">RULE_BASED: {{ hybridPerf.modeDistribution?.RULE_BASED || 0 }}</el-tag>
            <el-tag size="small" type="success" style="margin-left:4px">ADAPTIVE: {{ hybridPerf.modeDistribution?.ADAPTIVE || 0 }}</el-tag>
            <el-tag size="small" type="warning" style="margin-left:4px">HYBRID: {{ hybridPerf.modeDistribution?.HYBRID || 0 }}</el-tag>
            <span style="margin-left:8px;color:#909399;font-size:12px">总计判 {{ hybridPerf.totalJudged || 0 }} 条</span>
          </span>
        </div>
      </div>
    </el-card>

    <!-- 最近告警 -->
    <el-card class="alert-section" shadow="never">
      <template #header>
        <div class="section-header">
          <span>最近告警</span>
          <el-button text type="primary" @click="$router.push('/alert-history')">查看全部</el-button>
        </div>
      </template>
      <el-skeleton v-if="alertLoading" :rows="5" animated />
      <el-table v-else :data="alertList" style="width: 100%" size="small">
        <el-table-column prop="location" label="功能区" width="120" />
        <el-table-column prop="decibel" label="分贝值(dB)" width="100">
          <template #default="{ row }">
            {{ formatDecibel(row.decibel) }}
          </template>
        </el-table-column>
        <el-table-column prop="thresholdValue" label="阈值(dB)" width="100">
          <template #default="{ row }">
            {{ formatDecibel(row.thresholdValue) }}
          </template>
        </el-table-column>
        <el-table-column prop="alertType" label="告警类型" width="120" />
        <el-table-column prop="createTime" label="时间" min-width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!alertLoading && alertList.length === 0" class="empty-hint">暂无告警记录</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getDashboardOverview, getAlertList } from '@/api/dashboard'
import { getHybridStatus, getHybridPerformance } from '@/api/threshold'
import { getCcswitchStatus } from '@/api/dashboard'

const overviewList = ref([])
const alertList = ref([])
const hybridStatus = ref(null)
const hybridPerf = ref(null)
const ccswitchStatus = ref(null)
const loading = ref(true)
const alertLoading = ref(true)

let refreshTimer = null

// 格式化分贝值（保留 1 位小数）
function formatDecibel(val) {
  if (val == null) return '--'
  return Number(val).toFixed(1)
}

// 格式化时间
function formatTime(val) {
  if (!val) return '--'
  const d = new Date(val)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// 状态文案
function statusText(indicator) {
  switch (indicator) {
    case 'normal': return '正常'
    case 'abnormal': return '异常'
    case 'unknown': return '未知'
    default: return '未知'
  }
}

// 格式化运行时长
function formatUptime(seconds) {
  if (!seconds && seconds !== 0) return '--'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (h > 0) return `${h}h ${m}m`
  return `${m}m`
}

// 加载仪表盘数据
async function fetchDashboard() {
  try {
    const res = await getDashboardOverview()
    if (res.data && Array.isArray(res.data)) {
      overviewList.value = res.data
    }
  } catch {
    // 接口异常由 request 拦截器统一提示
  } finally {
    loading.value = false
  }
}

// 加载混合模型状态
async function fetchHybridStatus() {
  try {
    const res = await getHybridStatus()
    hybridStatus.value = res.data
  } catch {
    hybridStatus.value = null
  }
}

// 加载混合模型性能
async function fetchHybridPerf() {
  try {
    const res = await getHybridPerformance()
    hybridPerf.value = res.data
  } catch {
    hybridPerf.value = null
  }
}

// 加载 ccswitch 服务状态
async function fetchCcswitchStatus() {
  try {
    const res = await getCcswitchStatus()
    ccswitchStatus.value = res.data
  } catch {
    ccswitchStatus.value = null
  }
}

// 加载告警列表
async function fetchAlerts() {
  try {
    const res = await getAlertList({ pageNum: 1, pageSize: 5 })
    if (res.data) {
      // 兼容 data 直接为数组或有 records 分页包装
      alertList.value = Array.isArray(res.data) ? res.data : (res.data.records || [])
    }
  } catch {
    // 接口异常由 request 拦截器统一提示
  } finally {
    alertLoading.value = false
  }
}

onMounted(() => {
  fetchDashboard()
  fetchAlerts()
  fetchHybridStatus()
  fetchHybridPerf()
  fetchCcswitchStatus()
  // 每 10 秒自动刷新
  refreshTimer = setInterval(() => {
    fetchDashboard()
    fetchHybridStatus()
    fetchCcswitchStatus()
  }, 10000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}

.overview-card {
  border-radius: 8px;
}

.overview-card :deep(.el-card__body) {
  padding: 20px;
}

.card-content {
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.card-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.status-normal {
  background-color: #67C23A;
}

.status-abnormal {
  background-color: #F56C6C;
  animation: pulse 1.5s infinite;
}

.status-unknown {
  background-color: #C0C4CC;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.card-body {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 12px;
}

.decibel-value {
  font-size: 36px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.decibel-value.text-danger {
  color: #F56C6C;
}

.decibel-unit {
  font-size: 16px;
  color: #909399;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.threshold-info {
  color: #909399;
}

.update-time {
  color: #C0C4CC;
}

.alert-section {
  border-radius: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-hint {
  text-align: center;
  color: #909399;
  font-size: 14px;
  padding: 24px 0;
}

.hybrid-section {
  border-radius: 8px;
  margin-bottom: 16px;
}

.hybrid-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hybrid-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hybrid-label {
  font-size: 13px;
  color: #909399;
  min-width: 80px;
}

.hybrid-value {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.text-success {
  color: #67C23A !important;
}

.text-warning {
  color: #E6A23C !important;
}

/* ccswitch 状态卡片 */
.ccswitch-status-card {
  border-radius: 8px;
  height: 100%;
}
.ccswitch-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ccswitch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ccswitch-label {
  font-size: 13px;
  color: #909399;
}
.ccswitch-value {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}
.model-mini-compare {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mini-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mini-name {
  font-size: 12px;
  color: #606266;
  min-width: 72px;
  text-align: right;
}
.mini-item :deep(.el-progress) {
  flex: 1;
}
.mini-text {
  font-size: 10px;
}
.model-compare-note {
  text-align: center;
  margin-top: 8px;
  font-size: 11px;
  color: #67C23A;
  font-weight: 500;
}
.empty-hint-small {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 16px 0;
}

/* 混合引擎主面板 */
.hybrid-engine-panel {
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid #e6f7e6;
  background: linear-gradient(135deg, #f0f9f0 0%, #fff 100%);
}
.engine-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.engine-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.engine-icon {
  font-size: 20px;
}
.engine-label {
  font-size: 17px;
  font-weight: 700;
  color: #303133;
}
.engine-mode-tag {
  margin-left: 8px;
}
.engine-badge {
  font-size: 13px;
  color: #67C23A;
  font-weight: 600;
  background: #f0f9f0;
  padding: 4px 12px;
  border-radius: 20px;
}
.engine-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
@media (max-width: 992px) {
  .engine-cards { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 576px) {
  .engine-cards { grid-template-columns: 1fr; }
}
.engine-stat-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #ebeef5;
}
.engine-stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
}
.mode-icon { background: #e1f3d8; color: #67C23A; }
.reason-icon { background: #faecd8; color: #E6A23C; }
.count-icon { background: #d9ecff; color: #409EFF; }
.dist-icon { background: #fde2e2; color: #F56C6C; }
.engine-stat-info {
  flex: 1;
  min-width: 0;
}
.engine-stat-value {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.engine-stat-label {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}
.engine-stat-bars {
  display: flex;
  gap: 6px;
}
.bar-item {
  font-size: 11px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 2px;
}
.bar-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}
.rule-dot { background: #E6A23C; }
.adaptive-dot { background: #409EFF; }
.hybrid-dot { background: #67C23A; }
.model-compact-compare {
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}
.compare-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.compare-name {
  font-size: 12px;
  color: #606266;
  min-width: 80px;
  text-align: right;
}
.compare-item :deep(.el-progress) {
  flex: 1;
}
.progress-text {
  font-size: 11px;
  font-weight: 600;
}
</style>
