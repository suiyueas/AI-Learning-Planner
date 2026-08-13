// 模型管理 API
// 支持前端动态切换模型

import request from './request'

/**
 * 获取当前模型信息
 * @returns {Promise} 当前模型配置
 */
export async function getCurrentModel() {
  try {
    const response = await request.get('/model/current')
    // 后端直接返回数据，不包装在 data 中
    return response
  } catch (error) {
    console.error('获取当前模型失败:', error)
    // 返回默认值
    return {
      provider: 'qwen',
      displayName: 'Qwen-Max（阿里云）',
      modelName: 'qwen-max',
      baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode'
    }
  }
}

/**
 * 获取所有可用模型
 * @returns {Promise} 可用模型列表
 */
export async function getAvailableModels() {
  try {
    const response = await request.get('/model/available')
    // 后端直接返回数据，不包装在 data 中
    return response
  } catch (error) {
    console.error('获取可用模型失败:', error)
    // 返回默认值
    return {
      models: {
        qwen: { provider: 'qwen', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode', modelName: 'qwen-max' },
        deepseek: { provider: 'deepseek', baseUrl: 'https://api.deepseek.com', modelName: 'deepseek-chat' },
        xiaomi: { provider: 'xiaomi', baseUrl: 'https://api.xiaomimimo.com/v1', modelName: 'xiaomi/mimo-v2-pro' }
      },
      current: 'qwen'
    }
  }
}

/**
 * 切换模型
 * @param {string} provider - 模型提供者
 * @param {string} apiKey - API Key（可选）
 * @returns {Promise} 切换结果
 */
export async function switchModel(provider, apiKey = '') {
  try {
    const response = await request.post('/model/switch', { provider, apiKey })
    // 后端直接返回数据，不包装在 data 中
    return response
  } catch (error) {
    console.error('切换模型失败:', error)
    throw error
  }
}
