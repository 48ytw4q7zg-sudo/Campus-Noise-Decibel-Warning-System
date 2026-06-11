import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  // 从 localStorage 恢复登录态
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value)

  // 是否管理员
  const isAdmin = computed(() => role.value === '管理员')

  /**
   * 登录：保存 token 与用户信息到 localStorage 和 state
   * @param {string} t - JWT token
   * @param {string|number} id - 用户 ID
   * @param {string} r - 角色（普通用户 / 管理员）
   * @param {string} [name] - 用户名
   */
  function login(t, id, r, name) {
    token.value = t
    userId.value = String(id)
    role.value = r
    if (name) {
      username.value = name
      localStorage.setItem('username', name)
    }

    localStorage.setItem('token', t)
    localStorage.setItem('userId', String(id))
    localStorage.setItem('role', r)
  }

  /**
   * 退出登录：清空 localStorage 和 state，跳转登录页
   */
  function logout() {
    token.value = ''
    userId.value = ''
    username.value = ''
    role.value = ''

    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')

    router.push('/login')
  }

  return { token, userId, username, role, isLoggedIn, isAdmin, login, logout }
})
