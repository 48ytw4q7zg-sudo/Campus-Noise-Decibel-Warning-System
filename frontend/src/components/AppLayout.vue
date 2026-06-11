<!-- components/AppLayout.vue · 侧边导航布局组件 -->
<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '200px'" class="app-aside">
      <div class="aside-header" @click="isCollapse = !isCollapse">
        <span v-show="!isCollapse" class="aside-title">校园噪音预警</span>
        <el-icon :size="20" class="collapse-icon">
          <Fold v-if="!isCollapse" />
          <Expand v-else />
        </el-icon>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/noise-monitor">
          <el-icon><Monitor /></el-icon>
          <span>噪声监测</span>
        </el-menu-item>
        <el-menu-item index="/alert-history">
          <el-icon><Bell /></el-icon>
          <span>告警记录</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/area-config">
          <el-icon><Location /></el-icon>
          <span>功能区配置</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/threshold-config">
          <el-icon><Setting /></el-icon>
          <span>阈值配置</span>
        </el-menu-item>
        <el-menu-item index="/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计分析</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/settings">
          <el-icon><Tools /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" :size="22">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <el-space :size="12">
            <el-tag v-if="userStore.isAdmin" type="danger" size="small">管理员</el-tag>
            <el-tag v-else type="info" size="small">普通用户</el-tag>
            <span class="username">{{ userStore.username || '用户' }}</span>
            <el-button type="danger" size="small" plain @click="handleLogout">退出登录</el-button>
          </el-space>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Odometer, Monitor, Bell, Location, Setting, DataAnalysis, Tools, Fold, Expand
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()
const isCollapse = ref(false)

// 当前激活菜单项
const activeMenu = computed(() => route.path)

// 退出登录
function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
  }).catch(() => {})
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.app-aside {
  background-color: #304156;
  overflow: hidden;
  transition: width 0.3s;
}

.aside-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.aside-title {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.collapse-icon {
  color: #bfcbd9;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: 56px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: #409EFF;
}

.username {
  color: #303133;
  font-size: 14px;
}

.app-main {
  background-color: #f5f7fa;
  min-height: calc(100vh - 56px);
}
</style>
