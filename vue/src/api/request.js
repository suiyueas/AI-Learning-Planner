// Axios 统一配置
import axios from 'axios'
import { useAppStore } from '@/stores/appStore'

// 创建 Axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 可以在这里添加 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // 设置加载状态
    const appStore = useAppStore()
    if (config.showLoading !== false) {
      appStore.setLoading(true, config.loadingText || '加载中...')
    }
    
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const appStore = useAppStore()
    appStore.setLoading(false)
    
    const res = response.data
    
    // 如果响应数据为空（如 200 无内容），返回空对象避免 null 引用
    if (res === null || res === undefined) {
      return { code: 200, data: null }
    }
    
    // 如果后端直接返回数据（没有 code/success 包装），直接返回
    if (res && typeof res === 'object' && !('code' in res) && !('success' in res)) {
      return res
    }
    
    // 根据后端返回的状态码进行判断
    if (res.code === 200 || res.code === 0 || res.success) {
      return res
    }
    
    // 错误处理
    const errorMessage = res.message || res.msg || '请求失败'
    appStore.setError({
      message: errorMessage,
      code: res.code,
      timestamp: new Date()
    })
    
    return Promise.reject(new Error(errorMessage))
  },
  (error) => {
    const appStore = useAppStore()
    appStore.setLoading(false)

    // 优先透传后端返回的具体错误信息（如校验失败原因、用户名已存在等）
    const serverMessage = error.response?.data?.message || error.response?.data?.msg

    // 网络错误处理
    let errorMessage = '网络错误，请稍后重试'

    if (error.response) {
      const status = error.response.status
      if (serverMessage) {
        errorMessage = serverMessage
      } else {
        switch (status) {
          case 400:
            errorMessage = '请求参数错误'
            break
          case 401:
            errorMessage = '未授权，请重新登录'
            // Token 无效或过期，清除并跳转登录
            localStorage.removeItem('token')
            localStorage.removeItem('userId')
            if (window.location.pathname !== '/login') {
              window.location.href = '/login'
            }
            break
          case 403:
            // 已认证但权限不足：保留登录态，仅提示拒绝访问（不跳登录）
            errorMessage = serverMessage || '拒绝访问，您没有权限执行此操作'
            break
          case 404:
            errorMessage = '请求地址不存在'
            break
          case 500:
            errorMessage = '服务器内部错误'
            break
          case 502:
            errorMessage = '网关错误'
            break
          case 503:
            errorMessage = '服务不可用'
            break
          case 504:
            errorMessage = '网关超时'
            break
          default:
            errorMessage = `请求失败 (${status})`
        }
      }
    } else if (error.code === 'ECONNABORTED') {
      errorMessage = '请求超时，请稍后重试'
    } else if (error.message.includes('Network Error')) {
      errorMessage = '网络连接异常，请检查网络'
    }

    // 将具体错误信息挂到 error.message，使调用方 catch(e) 能拿到真实原因
    error.message = errorMessage

    appStore.setError({
      message: errorMessage,
      code: error.response?.status || -1,
      timestamp: new Date()
    })

    return Promise.reject(error)
  }
)

// 封装 GET 请求
export const get = (url, params = {}, config = {}) => {
  return service({
    method: 'get',
    url,
    params,
    ...config
  })
}

// 封装 POST 请求
export const post = (url, data = {}, config = {}) => {
  return service({
    method: 'post',
    url,
    data,
    ...config
  })
}

// 封装 PUT 请求
export const put = (url, data = {}, config = {}) => {
  return service({
    method: 'put',
    url,
    data,
    ...config
  })
}

// 封装 DELETE 请求
export const del = (url, data = {}, config = {}) => {
  return service({
    method: 'delete',
    url,
    data,
    ...config
  })
}

// 封装文件上传
export const upload = (url, file, onProgress = null) => {
  const formData = new FormData()
  formData.append('file', file)
  
  return service({
    method: 'post',
    url,
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress) {
        const percentCompleted = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total
        )
        onProgress(percentCompleted)
      }
    }
  })
}

// 封装文件下载
export const download = (url, params = {}, filename = '') => {
  return service({
    method: 'get',
    url,
    params,
    responseType: 'blob'
  }).then((response) => {
    const blob = new Blob([response])
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = filename || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
  })
}

export default service