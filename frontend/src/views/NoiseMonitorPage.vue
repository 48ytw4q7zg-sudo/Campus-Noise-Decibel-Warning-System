<template>
  <div class="noise-monitor-page">
    <!-- 手动录入表单（仅管理员可见） -->
    <el-card v-if="isAdmin" class="entry-card" shadow="hover">
      <template #header>
        <span>手动录入噪声数据</span>
      </template>
      <el-form :model="entryForm" :rules="entryRules" ref="entryFormRef" inline>
        <el-form-item label="功能区" prop="location">
          <el-select v-model="entryForm.location" placeholder="请选择功能区" style="width: 160px">
            <el-option label="图书馆" value="图书馆" />
            <el-option label="食堂" value="食堂" />
            <el-option label="操场" value="操场" />
            <el-option label="宿舍" value="宿舍" />
          </el-select>
        </el-form-item>
        <el-form-item label="分贝值" prop="decibel">
          <el-input-number v-model="entryForm.decibel" :min="20" :max="120" :step="0.1" :precision="1" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleEntrySubmit" :loading="entryLoading">提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 筛选区域 -->
    <el-card class="filter-card" shadow="hover">
      <el-form :model="filterForm" inline @submit.prevent="handleSearch">
        <el-form-item label="功能区">
          <el-select v-model="filterForm.location" placeholder="全部" clearable style="width: 140px">
            <el-option label="图书馆" value="图书馆" />
            <el-option label="食堂" value="食堂" />
            <el-option label="操场" value="操场" />
            <el-option label="宿舍" value="宿舍" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="分贝范围">
          <el-input-number v-model="filterForm.minDb" :min="20" :max="120" :step="0.1" :precision="1" placeholder="最小" style="width: 110px" />
          <span style="margin: 0 6px; color: #999">-</span>
          <el-input-number v-model="filterForm.maxDb" :min="20" :max="120" :step="0.1" :precision="1" placeholder="最大" style="width: 110px" />
        </el-form-item>
        <el-form-item label="异常状态">
          <el-select v-model="filterForm.isAbnormal" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="false" />
            <el-option label="异常" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <!-- 管理员导入导出 -->
          <template v-if="isAdmin">
            <el-divider direction="vertical" />
            <el-upload
              :action="''"
              :auto-upload="false"
              :show-file-list="false"
              accept=".csv,.xlsx,.xls"
              @change="handleImportFile"
            >
              <el-button type="warning" :loading="importLoading">导入数据</el-button>
            </el-upload>
            <el-button type="success" :loading="exportLoading" @click="handleExportReport">导出报表</el-button>
          </template>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="location" label="功能区" width="100" align="center" />
        <el-table-column prop="decibel" label="分贝值(dB)" width="110" align="center" />
        <el-table-column prop="timePoint" label="时间点" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.timePoint) }}
          </template>
        </el-table-column>
        <el-table-column prop="deviceId" label="设备ID" width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="isAbnormal" label="异常状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isAbnormal === true" type="danger" size="small">异常</el-tag>
            <el-tag v-else-if="row.isAbnormal === false" type="success" size="small">正常</el-tag>
            <el-tag v-else type="info" size="small">未判断</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="judgedByModel" label="判定模型" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.judgedByModel" type="warning" size="small">{{ judgedByModelMap[row.judgedByModel] || row.judgedByModel }}</el-tag>
            <span v-else style="color: #999">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="噪声记录详情" width="550px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="记录ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="功能区">{{ detailData.location }}</el-descriptions-item>
        <el-descriptions-item label="分贝值">{{ detailData.decibel }} dB(A)</el-descriptions-item>
        <el-descriptions-item label="时间点">{{ formatTime(detailData.timePoint) }}</el-descriptions-item>
        <el-descriptions-item label="设备ID">{{ detailData.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="异常状态">
          <el-tag v-if="detailData.isAbnormal === true" type="danger" size="small">异常</el-tag>
          <el-tag v-else-if="detailData.isAbnormal === false" type="success" size="small">正常</el-tag>
          <el-tag v-else type="info" size="small">未判断</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="判定模型">
          <el-tag v-if="detailData.judgedByModel" type="warning" size="small">{{ judgedByModelMap[detailData.judgedByModel] || detailData.judgedByModel }}</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="噪声类型">{{ detailData.noiseType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(detailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ formatTime(detailData.updateTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { queryNoiseRecords, createNoiseRecord, getNoiseDetail } from '@/api/noise';
import { importData, exportReport } from '@/api/import';

const userStore = useUserStore();
const isAdmin = computed(() => userStore.role === '管理员');

// 判定模型映射
const judgedByModelMap = {
  RULE_BASED: '业务规则',
  ADAPTIVE: '统计自适应',
  HYBRID: '混合模型'
};

// 录入表单
const entryForm = reactive({ location: '', decibel: null });
const entryFormRef = ref(null);
const entryLoading = ref(false);

const entryRules = {
  location: [{ required: true, message: '请选择功能区', trigger: 'change' }],
  decibel: [{ required: true, message: '请输入分贝值', trigger: 'blur' }]
};

const handleEntrySubmit = async () => {
  const valid = await entryFormRef.value.validate().catch(() => false);
  if (!valid) return;
  entryLoading.value = true;
  try {
    const res = await createNoiseRecord({
      location: entryForm.location,
      decibel: entryForm.decibel
    });
    ElMessage.success(res.message || '录入成功');
    entryForm.location = '';
    entryForm.decibel = null;
    entryFormRef.value.resetFields();
    fetchData();
  } catch (e) {
    // 拦截器已处理通用错误
  } finally {
    entryLoading.value = false;
  }
};

// 筛选表单
const filterForm = reactive({
  location: '',
  dateRange: null,
  minDb: null,
  maxDb: null,
  isAbnormal: null
});

// 表格数据
const tableData = ref([]);
const loading = ref(false);

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
});

// 时间格式化
const formatTime = (t) => {
  if (!t) return '-';
  const d = new Date(t);
  if (isNaN(d.getTime())) return t;
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

// 构建查询参数
const buildParams = () => {
  const params = {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    sortBy: 'time_point',
    sortOrder: 'desc'
  };
  if (filterForm.location) params.location = filterForm.location;
  if (filterForm.dateRange && filterForm.dateRange.length === 2) {
    params.dateFrom = filterForm.dateRange[0];
    params.dateTo = filterForm.dateRange[1];
  }
  if (filterForm.minDb !== null && filterForm.minDb !== undefined && filterForm.minDb !== '') {
    params.minDb = filterForm.minDb;
  }
  if (filterForm.maxDb !== null && filterForm.maxDb !== undefined && filterForm.maxDb !== '') {
    params.maxDb = filterForm.maxDb;
  }
  if (filterForm.isAbnormal !== null && filterForm.isAbnormal !== undefined) {
    params.isAbnormal = filterForm.isAbnormal;
  }
  return params;
};

// 获取数据
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await queryNoiseRecords(buildParams());
    tableData.value = res.data.records || [];
    pagination.total = res.data.total || 0;
    pagination.pageNum = res.data.pageNum || 1;
    pagination.pageSize = res.data.pageSize || 20;
  } catch (e) {
    tableData.value = [];
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1;
  fetchData();
};

// 重置
const handleReset = () => {
  filterForm.location = '';
  filterForm.dateRange = null;
  filterForm.minDb = null;
  filterForm.maxDb = null;
  filterForm.isAbnormal = null;
  pagination.pageNum = 1;
  fetchData();
};

// 分页切换
const handlePageChange = () => fetchData();
const handleSizeChange = () => {
  pagination.pageNum = 1;
  fetchData();
};

// 详情弹窗
const detailVisible = ref(false);
const detailData = ref({});

const handleDetail = async (row) => {
  try {
    const res = await getNoiseDetail(row.id);
    detailData.value = res.data || {};
    detailVisible.value = true;
  } catch (e) {
    // 拦截器已处理
  }
};

// 导入导出
const importLoading = ref(false);
const exportLoading = ref(false);

const handleImportFile = async (uploadFile) => {
  const file = uploadFile.raw;
  if (!file) return;

  importLoading.value = true;
  try {
    const formData = new FormData();
    formData.append('file', file);
    const res = await importData(formData);
    const result = res.data || {};
    const sc = result.successCount ?? 0;
    const fc = result.failCount ?? 0;
    if (fc > 0) {
      ElMessageBox.alert(
        `成功导入 ${sc} 条，失败 ${fc} 条。`,
        '导入结果',
        { confirmButtonText: '知道了', type: 'warning' }
      );
    } else {
      ElMessage.success(`成功导入 ${sc} 条数据`);
    }
    // 导入成功后刷新列表
    if (sc > 0) fetchData();
  } catch (e) {
    // 拦截器已处理
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

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.noise-monitor-page {
  padding: 16px;
}

.entry-card,
.filter-card,
.table-card {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
