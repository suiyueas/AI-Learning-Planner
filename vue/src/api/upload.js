// 统一文件上传 API
import service from './request'

/**
 * 上传用户头像
 * @param {File} file 头像文件
 * @param {function} onProgress 上传进度回调
 * @returns {Promise} 上传结果，data.avatarUrl 为头像访问路径
 */
export function uploadAvatar(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return service({
    method: 'post',
    url: '/upload/avatar',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
      ? (progressEvent) => {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      : undefined
  })
}

/**
 * 上传知识库文档
 * @param {File} file 文档文件
 * @param {string} docId 文档ID（可选）
 * @param {function} onProgress 上传进度回调
 * @returns {Promise} 上传结果，data.filePath 为文件访问路径
 */
export function uploadKnowledge(file, docId, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  if (docId) {
    formData.append('docId', docId)
  }
  return service({
    method: 'post',
    url: '/upload/knowledge',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
      ? (progressEvent) => {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      : undefined
  })
}

/**
 * 上传MCP工具文件
 * @param {File} file 工具文件
 * @param {string} toolId 工具ID（可选）
 * @returns {Promise} 上传结果，data.filePath 为文件访问路径
 */
export function uploadMcp(file, toolId) {
  const formData = new FormData()
  formData.append('file', file)
  if (toolId) {
    formData.append('toolId', toolId)
  }
  return service({
    method: 'post',
    url: '/upload/mcp',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 删除文件
 * @param {string} type 文件类型：avatar / knowledge / mcp
 * @param {string} filename 文件名
 * @returns {Promise} 删除结果
 */
export function deleteFile(type, filename) {
  return service({
    method: 'delete',
    url: `/upload/${type}/${filename}`
  })
}
