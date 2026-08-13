// 认证 API
import { post } from './request'

/**
 * 用户登录
 */
export function login(data) {
  return post('/auth/login', data)
}

/**
 * 用户注册
 */
export function register(data) {
  return post('/auth/register', data)
}
