/**
 * Markdown 渲染工具
 * 使用 marked 将 Markdown 文本渲染为 HTML，再经 DOMPurify 净化，
 * 防止 AI 输出/用户输入中的恶意 HTML（<script>、onerror 等）注入 DOM
 */
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// 配置 marked
marked.setOptions({
  breaks: true,      // 支持换行符转 <br>
  gfm: true,         // 启用 GitHub 风格 Markdown
  headerIds: false,   // 不生成标题 ID
  mangle: false       // 不转义邮箱地址
})

/**
 * 将 Markdown 文本渲染为 HTML
 * @param {string} markdown - Markdown 文本
 * @returns {string} 渲染后的 HTML
 */
export function renderMarkdown(markdown) {
  if (!markdown) return ''
  try {
    return DOMPurify.sanitize(marked.parse(markdown))
  } catch {
    // 解析失败时转义并返回纯文本
    return markdown
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>')
  }
}

/**
 * 解析结果内容，兼容 JSON 和纯文本/Markdown 格式
 * @param {string|object} content - 原始内容字符串或对象
 * @param {string} resultType - 结果类型（如 'rag_answer', 'document_summary' 等）
 * @returns {object|string} 解析后的内容，JSON 返回对象，文本返回原字符串
 */
export function parseResultContent(content, resultType) {
  // 如果是空值
  if (!content) return null

  // 如果已经是对象（不需要解析），直接返回
  if (typeof content === 'object') {
    return content
  }

  // 纯文本类型列表，这些类型直接返回原始内容
  const textTypes = ['rag_answer', 'document_summary', 'explanation', 'plan_content', 'markdown']
  if (textTypes.includes(resultType)) {
    return content
  }

  // 先尝试 JSON 解析
  try {
    return JSON.parse(content)
  } catch {
    // JSON 解析失败时，如果内容看起来是 Markdown/文本格式，直接返回
    if (/^[#>`\-*\d.]/.test(content.trim())) {
      return content
    }
    // 其他情况也返回原始内容
    return content
  }
}