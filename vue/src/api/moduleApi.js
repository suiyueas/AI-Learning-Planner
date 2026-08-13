// 功能模块API
import { get } from './request'

/**
 * 获取功能模块列表
 * @returns {Promise} 模块列表
 */
export const getModules = () => {
  return get('/modules')
}

/**
 * 获取功能模块分组列表
 * @returns {Promise} 模块分组列表
 */
export const getModuleGroups = () => {
  return get('/modules/groups')
}

export default {
  getModules,
  getModuleGroups
}
