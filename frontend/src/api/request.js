import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    // Blob 响应（文件下载/导出）直接透传，不做 JSON 解析
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code === 200) {
      return res
    } else if (res.code === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      ElMessage.error('未登录')
      return Promise.reject(new Error(res.message))
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        localStorage.removeItem('username')
        localStorage.removeItem('role')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(error)
      }
      if (status === 403) {
        ElMessage.error('无权限访问')
        return Promise.reject(error)
      }
      if (status >= 500) {
        ElMessage.error('服务器内部错误，请稍后重试')
        return Promise.reject(error)
      }
    }
    ElMessage.error('网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
