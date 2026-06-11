<!-- views/DashboardPage.vue · 仪表盘页面 -->
<template>
  <div class="dashboard-page">
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

const overviewList = ref([])
const alertList = ref([])
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
  // 每 10 秒自动刷新
  refreshTimer = setInterval(() => {
    fetchDashboard()
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
</style>
