<!-- views/SystemSettingsPage.vue · 系统设置页（管理员专属） -->
<template>
  <div class="system-settings-page">
    <div v-if="!isAdmin" class="no-permission">
      <el-result icon="warning" title="无权限" sub-title="仅管理员可访问系统设置" />
    </div>

    <template v-else>
      <!-- 区域1: ccswitch 配置状态 -->
      <el-card class="section-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>ccswitch 配置服务</span>
            <el-tag :type="ccStatus.connected ? 'success' : 'danger'" size="small">
              {{ ccStatus.connected ? '已连接' : '未连接' }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="当前模型">{{ ccStatus.modelName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Base URL">{{ ccStatus.baseUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运行时间">{{ ccStatus.uptime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="配置来源">{{ ccStatus.configSource || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="card-actions">
          <el-button type="primary" :loading="ccReloading" @click="handleCcswitchReload">
            重载配置
          </el-button>
        </div>
      </el-card>

      <!-- 区域2: AI 分类配置 -->
      <el-card class="section-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>AI 噪声分类配置</span>
          </div>
        </template>
        <el-form label-width="140px" label-position="left">
          <el-form-item label="启用自动分类">
            <el-switch v-model="aiConfig.autoClassify" @change="handleAiConfigSave" />
          </el-form-item>
          <el-form-item label="最小置信度">
            <div class="slider-row">
              <el-slider
                v-model="aiConfig.minConfidence"
                :min="0"
                :max="1"
                :step="0.05"
                :marks="confidenceMarks"
                show-input
                style="width: 320px"
                @change="handleAiConfigSave"
              />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="aiClassifying" @click="handleAiClassify">
              手动触发分类
            </el-button>
            <span v-if="classifyResult" class="classify-result">
              已分类 <strong>{{ classifyResult.classifiedCount ?? 0 }}</strong> 条，已跳过 <strong>{{ classifyResult.skippedCount ?? 0 }}</strong> 条
            </span>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 区域3: 报告生成 -->
      <el-card class="section-card" shadow="hover">
        <template #header>
          <span>报告生成</span>
        </template>
        <el-form :model="reportForm" inline>
          <el-form-item label="周期">
            <el-select v-model="reportForm.period" placeholder="请选择周期" style="width: 120px">
              <el-option label="日报" value="day" />
              <el-option label="周报" value="week" />
              <el-option label="月报" value="month" />
            </el-select>
          </el-form-item>
          <el-form-item label="起始时间">
            <el-date-picker
              v-model="reportForm.startTime"
              type="datetime"
              placeholder="选择起始时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker
              v-model="reportForm.endTime"
              type="datetime"
              placeholder="选择结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="reportGenerating" @click="handleGenerateReport">
              手动生成报告
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 报告列表 -->
        <el-table :data="reportList" v-loading="reportLoading" stripe border style="width: 100%; margin-top: 16px">
          <el-table-column prop="id" label="ID" width="60" align="center" />
          <el-table-column prop="period" label="周期" width="80" align="center" />
          <el-table-column prop="title" label="报告标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createTime" label="生成时间" width="170" align="center">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'completed'" type="success" size="small">已完成</el-tag>
              <el-tag v-else-if="row.status === 'generating'" type="warning" size="small">生成中</el-tag>
              <el-tag v-else type="info" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleDownloadReport(row)">
                下载
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="reportPagination.pageNum"
            v-model:page-size="reportPagination.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="reportPagination.total"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchReports"
            @current-change="fetchReports"
          />
        </div>
      </el-card>

      <!-- 区域4: 数据管理 -->
      <el-card class="section-card" shadow="hover">
        <template #header>
          <span>数据管理</span>
        </template>
        <div class="data-actions">
          <el-upload
            :action="''"
            :auto-upload="false"
            :show-file-list="false"
            accept=".csv,.xlsx,.xls"
            @change="handleFileChange"
          >
            <el-button type="primary" :loading="importLoading">
              导入数据
            </el-button>
          </el-upload>
          <el-button type="success" :loading="exportLoading" @click="handleExportReport">
            导出报表
          </el-button>
        </div>

        <!-- 导入结果 -->
        <div v-if="importResult" class="import-result">
          <el-alert
            :title="`导入完成：成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条`"
            :type="importResult.failCount > 0 ? 'warning' : 'success'"
            :closable="false"
            show-icon
            style="margin-bottom: 12px"
          />
          <el-table v-if="importResult.errors && importResult.errors.length > 0" :data="importResult.errors" border size="small" max-height="240">
            <el-table-column prop="row" label="行号" width="80" align="center" />
            <el-table-column prop="field" label="字段" width="120" align="center" />
            <el-table-column prop="message" label="错误信息" min-width="200" show-overflow-tooltip />
          </el-table>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { getCcswitchStatus, reloadCcswitchConfig } from '@/api/ccswitch';
import { classifyNoise, updateAiConfig } from '@/api/ai';
import { queryReports, generateReport, downloadReport } from '@/api/report';
import { importData, exportReport } from '@/api/import';

const userStore = useUserStore();
const isAdmin = computed(() => userStore.isAdmin);

// ===== ccswitch 配置状态 =====
const ccStatus = ref({});
const ccReloading = ref(false);

const fetchCcswitchStatus = async () => {
  try {
    const res = await getCcswitchStatus();
    ccStatus.value = res.data || {};
  } catch (e) {
    ccStatus.value = {};
  }
};

const handleCcswitchReload = async () => {
  ccReloading.value = true;
  try {
    const res = await reloadCcswitchConfig();
    ElMessage.success(res.message || '配置重载成功');
    await fetchCcswitchStatus();
  } catch (e) {
    // 拦截器已处理
  } finally {
    ccReloading.value = false;
  }
};

// ===== AI 分类配置 =====
const aiConfig = reactive({
  autoClassify: false,
  minConfidence: 0.70
});

const confidenceMarks = {
  0: '0',
  0.5: '0.5',
  1: '1'
};

const aiClassifying = ref(false);
const classifyResult = ref(null);

const handleAiConfigSave = async () => {
  try {
    await updateAiConfig({ autoClassify: aiConfig.autoClassify, minConfidence: aiConfig.minConfidence });
    ElMessage.success('AI 配置已保存');
  } catch (e) {
    // 拦截器已处理
  }
};

const handleAiClassify = async () => {
  aiClassifying.value = true;
  classifyResult.value = null;
  try {
    const res = await classifyNoise();
    classifyResult.value = res.data || {};
    ElMessage.success(res.message || 'AI 分类完成');
  } catch (e) {
    classifyResult.value = null;
  } finally {
    aiClassifying.value = false;
  }
};

// ===== 报告生成 =====
const reportForm = reactive({
  period: 'day',
  startTime: '',
  endTime: ''
});
const reportGenerating = ref(false);
const reportLoading = ref(false);
const reportList = ref([]);
const reportPagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const fetchReports = async () => {
  reportLoading.value = true;
  try {
    const res = await queryReports({
      pageNum: reportPagination.pageNum,
      pageSize: reportPagination.pageSize
    });
    reportList.value = res.data?.records || [];
    reportPagination.total = res.data?.total || 0;
  } catch (e) {
    reportList.value = [];
  } finally {
    reportLoading.value = false;
  }
};

const handleGenerateReport = async () => {
  if (!reportForm.startTime || !reportForm.endTime) {
    ElMessage.warning('请选择起始时间和结束时间');
    return;
  }
  reportGenerating.value = true;
  try {
    const res = await generateReport({
      period: reportForm.period,
      startTime: reportForm.startTime,
      endTime: reportForm.endTime
    });
    ElMessage.success(res.message || '报告生成成功');
    fetchReports();
  } catch (e) {
    // 拦截器已处理
  } finally {
    reportGenerating.value = false;
  }
};

const handleDownloadReport = async (row) => {
  try {
    const blob = await downloadReport(row.id);
    const url = window.URL.createObjectURL(new Blob([blob], { type: 'text/markdown;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `report_${row.id}_${new Date().toISOString().slice(0, 10)}.md`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    ElMessage.success('报告下载成功');
  } catch (e) {
    // 拦截器已处理
  }
};

// ===== 数据管理 =====
const importLoading = ref(false);
const importResult = ref(null);
const exportLoading = ref(false);

const handleFileChange = async (uploadFile) => {
  const file = uploadFile.raw;
  if (!file) return;

  importLoading.value = true;
  importResult.value = null;
  try {
    const formData = new FormData();
    formData.append('file', file);
    const res = await importData(formData);
    importResult.value = res.data || {};
    const sc = importResult.value.successCount ?? 0;
    const fc = importResult.value.failCount ?? 0;
    if (fc > 0) {
      ElMessageBox.alert(
        `成功导入 ${sc} 条，失败 ${fc} 条。请查看下方错误详情。`,
        '导入结果',
        { confirmButtonText: '知道了', type: 'warning' }
      );
    } else {
      ElMessage.success(`成功导入 ${sc} 条数据`);
    }
  } catch (e) {
    importResult.value = null;
  } finally {
    importLoading.value = false;
  }
};

const handleExportReport = async () => {
  exportLoading.value = true;
  try {
    const res = await exportReport();
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `noise_report_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (e) {
    // 拦截器已处理
  } finally {
    exportLoading.value = false;
  }
};

// ===== 时间格式化 =====
const formatTime = (t) => {
  if (!t) return '-';
  const d = new Date(t);
  if (isNaN(d.getTime())) return t;
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

// ===== 初始化 =====
onMounted(() => {
  if (isAdmin.value) {
    fetchCcswitchStatus();
    fetchReports();
  }
});
</script>

<style scoped>
.system-settings-page {
  padding: 16px;
}

.no-permission {
  padding: 60px 0;
}

.section-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-actions {
  margin-top: 16px;
}

.slider-row {
  display: flex;
  align-items: center;
}

.classify-result {
  margin-left: 16px;
  color: #606266;
  font-size: 14px;
}

.classify-result strong {
  color: #409EFF;
  margin: 0 2px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.data-actions {
  display: flex;
  gap: 12px;
}

.import-result {
  margin-top: 16px;
}
</style>
