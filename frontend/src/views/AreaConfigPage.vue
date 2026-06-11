<template>
  <div class="area-config-page">
    <el-card class="table-card" shadow="hover">
      <template #header>
        <span>功能区配置管理</span>
      </template>
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="areaName" label="功能区名称" width="120" align="center" />
        <el-table-column prop="noiseSensitivity" label="噪声敏感度" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="sensitivityTag(row.noiseSensitivity)" size="small">{{ sensitivityLabel(row.noiseSensitivity) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="defaultThreshold" label="默认阈值(dB)" width="130" align="center" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑功能区配置" width="500px" destroy-on-close @closed="editFormRef?.resetFields()">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="120px">
        <el-form-item label="功能区名称">
          <el-input :model-value="editForm.areaName" disabled />
        </el-form-item>
        <el-form-item label="噪声敏感度" prop="noiseSensitivity">
          <el-select v-model="editForm.noiseSensitivity" placeholder="请选择敏感度" style="width: 100%">
            <el-option label="高" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="低" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认阈值(dB)" prop="defaultThreshold">
          <el-input-number v-model="editForm.defaultThreshold" :min="0" :max="120" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="editLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getAreas, updateArea } from '@/api/area';

// 表格数据
const tableData = ref([]);
const loading = ref(false);

// 敏感度显示
const sensitivityLabel = (val) => {
  const map = { 1: '高', 2: '中', 3: '低' };
  return map[val] || '未知';
};

const sensitivityTag = (val) => {
  const map = { 1: 'danger', 2: 'warning', 3: 'info' };
  return map[val] || 'info';
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getAreas();
    tableData.value = res.data || [];
  } catch (e) {
    tableData.value = [];
  } finally {
    loading.value = false;
  }
};

// 编辑弹窗
const editVisible = ref(false);
const editForm = reactive({
  id: null,
  areaName: '',
  noiseSensitivity: null,
  defaultThreshold: null,
  description: '',
  status: 1,
  version: 0
});
const editFormRef = ref(null);
const editLoading = ref(false);

const editRules = {
  noiseSensitivity: [{ required: true, message: '请选择敏感度', trigger: 'change' }],
  defaultThreshold: [{ required: true, message: '请输入默认阈值', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
};

const handleEdit = (row) => {
  editForm.id = row.id;
  editForm.areaName = row.areaName;
  editForm.noiseSensitivity = row.noiseSensitivity;
  editForm.defaultThreshold = row.defaultThreshold;
  editForm.description = row.description || '';
  editForm.status = row.status;
  editForm.version = row.version;
  editVisible.value = true;
};

const handleEditSubmit = async () => {
  const valid = await editFormRef.value.validate().catch(() => false);
  if (!valid) return;
  editLoading.value = true;
  try {
    const res = await updateArea(editForm.id, {
      noiseSensitivity: editForm.noiseSensitivity,
      defaultThreshold: editForm.defaultThreshold,
      description: editForm.description,
      status: editForm.status,
      version: editForm.version
    });
    ElMessage.success(res.message || '保存成功');
    editVisible.value = false;
    fetchData();
  } catch (e) {
    // 拦截器已处理
  } finally {
    editLoading.value = false;
  }
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.area-config-page {
  padding: 16px;
}
</style>
