<template>
  <div class="login-wrapper">
    <div class="login-card">
      <h1 class="login-title">校园噪音分贝预警员系统</h1>
      <p class="login-subtitle">登录或注册以继续</p>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            @keyup.enter="handleLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
              >
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                placeholder="请输入密码"
                show-password
                type="password"
              >
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button
                :loading="loginLoading"
                class="submit-btn"
                type="primary"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="2-20个字符"
              >
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                placeholder="6-32个字符"
                show-password
                type="password"
              >
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                placeholder="请再次输入密码"
                show-password
                type="password"
              >
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="角色" prop="role">
              <el-select
                v-model="registerForm.role"
                class="full-width"
                placeholder="请选择角色"
              >
                <el-option label="普通用户" value="普通用户" />
                <el-option label="管理员" value="管理员" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                :loading="registerLoading"
                class="submit-btn"
                type="success"
                @click="handleRegister"
              >
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login, register } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// Tab 切换
const activeTab = ref('login')

// 登录表单
const loginFormRef = ref(null)
const loginLoading = ref(false)
const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 个字符', trigger: 'blur' }
  ]
}

// 注册表单
const registerFormRef = ref(null)
const registerLoading = ref(false)
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  role: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度为 8-32 个字符', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)/, message: '密码必须包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 登录处理
async function handleLogin() {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  loginLoading.value = true
  try {
    const res = await login({
      username: loginForm.username,
      password: loginForm.password
    })
    // res 已经被 axios 拦截器处理，code === 200 时直接返回 {code, message, data}
    const { token, userId, role } = res.data
    userStore.login(token, userId, role)
    ElMessage.success('登录成功')

    // 跳转到 redirect 参数指定的页面，或默认跳首页
    const redirectPath = route.query.redirect || '/'
    router.push(redirectPath)
  } catch {
    // 错误已在 axios 拦截器中通过 ElMessage.error 提示，这里只需停止 loading
  } finally {
    loginLoading.value = false
  }
}

// 注册处理
async function handleRegister() {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }

  registerLoading.value = true
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password,
      role: registerForm.role
    })
    ElMessage.success('注册成功，请登录')

    // 切回登录 Tab 并自动填入用户名
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = ''
    // 清空注册表单
    registerForm.username = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerForm.role = ''
  } catch {
    // 错误已在 axios 拦截器中提示
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  align-items: center;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  display: flex;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
}

.login-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  max-width: 420px;
  padding: 40px 36px;
  width: 100%;
}

.login-title {
  color: #2c3e50;
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
  text-align: center;
}

.login-subtitle {
  color: #909399;
  font-size: 14px;
  margin-bottom: 24px;
  text-align: center;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.submit-btn {
  width: 100%;
}

.full-width {
  width: 100%;
}
</style>
