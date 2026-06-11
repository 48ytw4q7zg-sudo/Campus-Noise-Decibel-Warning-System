import request from './request'

/**
 * 用户注册
 * @param {Object} data - { username, password, role }
 * @returns {Promise}
 */
export function register(data) {
  return request.post('/auth/register', data)
}

/**
 * 用户登录
 * @param {Object} data - { username, password }
 * @returns {Promise} - { token, userId, role }
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 获取当前用户信息
 * @returns {Promise}
 */
export function getProfile() {
  return request.get('/auth/profile')
}

/**
 * 修改密码
 * @param {Object} data - { oldPassword, newPassword }
 * @returns {Promise}
 */
export function changePassword(data) {
  return request.put('/auth/password', data)
}
