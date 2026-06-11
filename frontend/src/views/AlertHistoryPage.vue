<template>
  <div class="alert-history-page">
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
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 告警表格 -->
    <el-card class="table-card" shadow="hover">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="location" label="功能区" width="100" align="center" />
        <el-table-column prop="decibel" label="分贝值(dB)" width="110" align="center" />
        <el-table-column prop="thresholdValue" label="阈值(dB)" width="100" align="center" />
        <el-table-column prop="alertType" label="告警类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="alertTypeTag(row.alertType)" size="small">{{ row.alertType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confirmStatus" label="确认状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="confirmStatusTag(row.confirmStatus)" size="small">{{ row.confirmStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="告警时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
            <template v-if="isAdmin">
              <el-button
                v-if="row.confirmStatus === '未确认'"
                type="warning"
                link
                size="small"
                @click="handleConfirm(row)"
              >确认</el-button>
              <el-button
                v-if="row.confirmStatus === '已确认'"
                type="success"
                link
                size="small"
                @click="handleResolve(row)"
              >处置</el-button>
            </template>
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
    <el-dialog v-model="detailVisible" title="告警详情" width="550px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="告警ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="关联噪声记录ID">{{ detailData.noiseRecordId }}</el-descriptions-item>
        <el-descriptions-item label="功能区">{{ detailData.location }}</el-descriptions-item>
        <el-descriptions-item label="分贝值">{{ detailData.decibel }} dB(A)</el-descriptions-item>
        <el-descriptions-item label="触发阈值">{{ detailData.thresholdValue }} dB(A)</el-descriptions-item>
        <el-descriptions-item label="告警类型">
          <el-tag :type="alertTypeTag(detailData.alertType)" size="small">{{ detailData.alertType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="确认状态">
          <el-tag :type="confirmStatusTag(detailData.confirmStatus)" size="small">{{ detailData.confirmStatus }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="告警时间">{{ formatTime(detailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ formatTime(detailData.updateTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 处置弹窗 -->
    <el-dialog v-model="resolveVisible" title="处置告警" width="450px" destroy-on-close>
      <el-form :model="resolveForm" :rules="resolveRules" ref="resolveFormRef" label-width="100px">
        <el-descriptions :column="1" border size="small" style="margin-bottom: 16px">
          <el-descriptions-item label="告警ID">{{ resolveTarget.id }}</el-descriptions-item>
          <el-descriptions-item label="功能区">{{ resolveTarget.location }}</el-descriptions-item>
          <el-descriptions-item label="分贝值">{{ resolveTarget.decibel }} dB(A)</el-descriptions-item>
        </el-descriptions>
        <el-form-item label="处置备注" prop="remark">
          <el-input
            v-model="resolveForm.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入处置备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResolveSubmit" :loading="resolveLoading">确认处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { queryAlerts, getAlertDetail, confirmAlert, resolveAlert } from '@/api/alert';

const userStore = useUserStore();
const isAdmin = computed(() => userStore.role === '管理员');

// 告警类型标签颜色
const alertTypeTag = (type) => {
  const map = { '超阈值': 'danger', '骤升': 'warning', '夜间异常': 'info' };
  return map[type] || 'info';
};

// 确认状态标签颜色
const confirmStatusTag = (status) => {
  const map = { '未确认': 'danger', '已确认': 'warning', '已处置': 'success' };
  return map[status] || 'info';
};

// 筛选表单
const filterForm = reactive({
  location: '',
  dateRange: null
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

const formatTime = (t) => {
  if (!t) return '-';
  const d = new Date(t);
  if (isNaN(d.getTime())) return t;
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const buildParams = () => {
  const params = {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  };
  if (filterForm.location) params.location = filterForm.location;
  if (filterForm.dateRange && filterForm.dateRange.length === 2) {
    params.dateFrom = filterForm.dateRange[0];
    params.dateTo = filterForm.dateRange[1];
  }
  return params;
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await queryAlerts(buildParams());
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

const handleSearch = () => {
  pagination.pageNum = 1;
  fetchData();
};

const handleReset = () => {
  filterForm.location = '';
  filterForm.dateRange = null;
  pagination.pageNum = 1;
  fetchData();
};

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
    const res = await getAlertDetail(row.id);
    detailData.value = res.data || {};
    detailVisible.value = true;
  } catch (e) {
    // 拦截器已处理
  }
};

// 确认告警
const handleConfirm = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入确认信息（可选）', '确认告警', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPlaceholder: '确认备注（可选）',
      inputType: 'textarea',
      inputMaxlength: 500
    });
    const confirmResult = await confirmAlert(row.id, { version: row.version, remark: value || '' });
    ElMessage.success(confirmResult.message || '告警已确认');
    fetchData();
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      // 拦截器已处理接口错误，这里只处理取消
    }
  }
};

// 处置告警
const resolveVisible = ref(false);
const resolveTarget = ref({});
const resolveForm = reactive({ remark: '' });
const resolveFormRef = ref(null);
const resolveLoading = ref(false);

const resolveRules = {
  remark: [{ required: true, message: '请输入处置备注', trigger: 'blur' }]
};

const handleResolve = (row) => {
  resolveTarget.value = { ...row };
  resolveForm.remark = '';
  resolveVisible.value = true;
};

const handleResolveSubmit = async () => {
  const valid = await resolveFormRef.value.validate().catch(() => false);
  if (!valid) return;
  resolveLoading.value = true;
  try {
    const res = await resolveAlert(resolveTarget.value.id, {
      version: resolveTarget.value.version,
      remark: resolveForm.remark
    });
    ElMessage.success(res.message || '告警已处置');
    resolveVisible.value = false;
    fetchData();
  } catch (e) {
    // 拦截器已处理
  } finally {
    resolveLoading.value = false;
  }
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.alert-history-page {
  padding: 16px;
}

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
