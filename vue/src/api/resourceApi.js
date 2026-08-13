// 资源API
import { get } from './request'

/**
 * 获取资源列表
 * @returns {Promise} 资源列表
 */
export const getResources = () => {
  return get('/resources')
}

/**
 * 获取热门资源
 * @returns {Promise} 热门资源列表
 */
export const getHotResources = () => {
  return get('/resources/hot')
}

/**
 * 获取资源详情
 * @param {string} id 资源ID
 * @returns {Promise} 资源详情
 */
export const getResource = (id) => {
  return get(`/resources/${id}`)
}

/**
 * 根据节点ID获取资源
 * @param {string} nodeId 节点ID
 * @returns {Promise} 资源列表
 */
export const getResourcesByNode = (nodeId) => {
  return get(`/resources/node/${nodeId}`)
}

export default {
  getResources,
  getHotResources,
  getResource,
  getResourcesByNode
}
