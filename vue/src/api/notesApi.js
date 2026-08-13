// 学习笔记API
import { get, post, put, del } from './request'

/**
 * 获取用户笔记列表
 * @returns {Promise} Note[]
 */
export const getNotes = () => {
  return get('/notes')
}

/**
 * 获取笔记详情
 * @param {number} id 笔记ID
 */
export const getNoteById = (id) => {
  return get(`/notes/${id}`)
}

/**
 * 创建笔记
 * @param {string} title 标题
 * @param {string} content 正文
 * @param {string} tags 标签（逗号分隔）
 */
export const createNote = (title, content, tags = '') => {
  return post('/notes', { title, content, tags })
}

/**
 * 更新笔记
 * @param {number} id 笔记ID
 * @param {string} title 标题
 * @param {string} content 正文
 * @param {string} tags 标签（逗号分隔）
 */
export const updateNote = (id, title, content, tags = '') => {
  return put(`/notes/${id}`, { title, content, tags })
}

/**
 * 删除笔记
 * @param {number} id 笔记ID
 */
export const deleteNote = (id) => {
  return del(`/notes/${id}`)
}

/**
 * 导出笔记（生成导出内容）
 * @param {Array<number>} noteIds 笔记ID列表（空/null 导出全部）
 * @param {string} format 格式 markdown/text
 * @param {boolean} includeCodeBlocks 是否包含代码块
 * @param {boolean} includeTags 是否包含标签
 * @returns {Promise} { content, filename, format, noteCount }
 */
export const exportNotes = (noteIds = null, format = 'markdown', includeCodeBlocks = true, includeTags = true) => {
  return post('/notes/export', { noteIds, format, includeCodeBlocks, includeTags })
}

/**
 * 后端下载端点（content 走 URL 参数，适用于短内容；长内容建议用前端 Blob 下载）
 * @param {string} content 导出内容
 * @param {string} format 格式 markdown/text
 */
export const downloadExport = (content, format = 'markdown') => {
  return get('/notes/export/download', { content, format })
}

export default {
  getNotes,
  getNoteById,
  createNote,
  updateNote,
  deleteNote,
  exportNotes,
  downloadExport
}
