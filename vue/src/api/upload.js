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