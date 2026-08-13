// 知识库 API 接口
import { get, post, del, upload } from './request'

/**
 * 获取文档列表
 * @returns {Promise} { success, data: Document[] }
 */
export const getDocuments = () => {
  return get('/knowledge/documents')
}

/**
 * 获取知识库状态
 * @returns {Promise} { success, data: { connected, documentCount, chunkCount, readyCount } }
 */
export const getStatus = () => {
  return get('/knowledge/status')
}

/**
 * 上传文档
 * @param {File} file 文件对象
 * @param {Function} onProgress 进度回调 (0-100)
 * @returns {Promise}
 */
export const uploadDocument = (file, onProgress) => {
  return upload('/knowledge/upload', file, onProgress)
}

/**
 * 删除文档
 * @param {string} id 文档ID
 * @returns {Promise}
 */
export const deleteDocument = (id) => {
  return del(`/knowledge/${id}`)
}

/**
 * 获取文档知识块列表
 * @param {string} id 文档ID
 * @returns {Promise} { success, data: KnowledgeChunk[] }
 */
export const getDocumentChunks = (id) => {
  return get(`/knowledge/documents/${id}/chunks`)
}

/**
 * 根据原始文件名获取文档内容
 * @param {string} filename 文档原始文件名（含扩展名）
 * @returns {Promise} { success, data: { id, title, content, preview, ... } }
 */
export const getDocumentContent = (filename) => {
  return get('/knowledge/document/content', { filename })
}

/**
 * 知识检索（关键词搜索知识块）
 * @param {string} keyword 搜索关键词
 * @returns {Promise}
 */
export const searchKnowledge = (keyword) => {
  return post('/knowledge/search', { keyword })
}

/**
 * 基于知识库问答
 * @param {string} question 问题
 * @returns {Promise}
 */
export const askKnowledge = (question) => {
  return post('/knowledge/ask', { question })
}

/**
 * 触发全量知识块生成
 * @returns {Promise}
 */
export const generateAllChunks = () => {
  return post('/knowledge/chunks/generate-all')
}

/**
 * 为单个文档生成知识块
 * @param {string} docId 文档ID
 * @returns {Promise}
 */
export const generateChunksForDocument = (docId) => {
  return post(`/knowledge/documents/${docId}/chunks/generate`)
}

export default {
  getDocuments,
  getStatus,
  uploadDocument,
  deleteDocument,
  getDocumentChunks,
  getDocumentContent,
  searchKnowledge,
  askKnowledge,
  generateAllChunks,
  generateChunksForDocument
}