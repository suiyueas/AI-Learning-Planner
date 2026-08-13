/**
 * 执行结果统一视图解析工具
 *
 * 目标：将各种历史/现有执行结果数据形态，统一解析为前端渲染所需的
 * { resultType, outputText, outputJson, displayTitle, summary } 结构。
 *
 * 兼容的数据形态：
 * 1. 新统一结构：{ type, outputText, outputJson, displayTitle, summary, data, message }
 * 2. 旧结构：    { type, title, data: { query, result, source, confidence }, message }
 * 3. 规范化结构：{ content, fullData, raw }（normalizeOutput 产物）
 * 4. 后端实体：  { output, resultContent, resultType, ... }（AgentExecution 序列化）
 * 5. 纯字符串（Markdown / 文本 / JSON 字符串）
 */

/** 结果类型归一化（兼容多种历史命名） */
const RESULT_TYPE_ALIAS = {
  diagnosis: 'diagnosis',
  plan: 'plan',
  planner: 'plan',
  qa: 'qa',
  explanation: 'qa',
  tutor: 'qa',
  report: 'report',
  reporter: 'report',
  quiz: 'quiz',
  exercise: 'quiz',
  exercises: 'quiz',
  search: 'search',
  knowledge: 'knowledge',
  knowledge_search: 'knowledge',
  markdown: 'qa',
  default: 'default'
}

/** 各类型默认展示标题 */
const DISPLAY_TITLES = {
  diagnosis: '诊断报告',
  plan: '学习计划',
  qa: '答疑解惑',
  report: '学习报告',
  quiz: '测验题',
  search: '搜索结果',
  knowledge: '知识检索',
  default: '执行结果'
}

/** 空视图 */
const emptyView = () => ({
  resultType: 'default',
  outputText: '',
  outputJson: null,
  displayTitle: '',
  summary: ''
})

/** 判断对象是否为规范化结构 { content, fullData, raw } */
const isNormalizedShape = (obj) => obj && typeof obj === 'object' && 'content' in obj && 'raw' in obj

/** 判断对象是否可直接视为结果载荷（含类型/数据标识） */
const isPayloadLike = (obj) => obj && typeof obj === 'object' && (
  'type' in obj || 'outputText' in obj || 'data' in obj || 'resultType' in obj
)

/** 尝试解析 JSON 字符串，失败返回 null */
const tryParseJson = (str) => {
  if (typeof str !== 'string') return null
  const t = str.trim()
  if (!t.startsWith('{') && !t.startsWith('[')) return null
  try { return JSON.parse(t) } catch { return null }
}

/** 归一化结果类型 */
const normalizeType = (type) => RESULT_TYPE_ALIAS[String(type || '').toLowerCase()] || 'default'

/** 从载荷中提取结构化数据（优先新结构 outputJson，兼容旧 data 内嵌字段） */
const extractJson = (payload) => {
  if (!payload || typeof payload !== 'object') return null
  if (payload.outputJson) return payload.outputJson
  if (payload.data?.outputJson) return payload.data.outputJson
  // 旧结构 data 中内嵌结构化字段（非纯文本 result 时视为结构化数据）
  const data = payload.data
  if (data && typeof data === 'object' && data.result && typeof data.result === 'object') {
    return data.result
  }
  return null
}

/** 从载荷中提取人类可读文本（优先 outputText，兼容旧 data.result / message） */
const extractText = (payload) => {
  if (!payload) return ''
  if (typeof payload === 'string') return payload.trim()
  if (typeof payload.outputText === 'string' && payload.outputText.trim()) return payload.outputText.trim()
  const dataResult = payload.data?.result
  if (typeof dataResult === 'string' && dataResult.trim()) return dataResult.trim()
  if (typeof payload.result === 'string' && payload.result.trim()) return payload.result.trim()
  if (typeof payload.content === 'string' && payload.content.trim()) return payload.content.trim()
  // message 为固定成功文案时不作为结果展示
  if (typeof payload.message === 'string' && payload.message.trim() && !/已成功执行/.test(payload.message)) {
    return payload.message.trim()
  }
  return ''
}

/** 提取摘要（一句话，展示在历史列表） */
const extractSummary = (payload) => {
  if (!payload || typeof payload !== 'object') return ''
  if (typeof payload.summary === 'string' && payload.summary.trim()) return payload.summary.trim()
  if (typeof payload.data?.summary === 'string' && payload.data.summary.trim()) return payload.data.summary.trim()
  return ''
}

/** 提取展示标题 */
const extractTitle = (payload, resultType) => {
  if (!payload || typeof payload !== 'object') return ''
  if (typeof payload.displayTitle === 'string' && payload.displayTitle.trim()) return payload.displayTitle.trim()
  if (typeof payload.title === 'string' && payload.title.trim()) return payload.title.trim()
  return DISPLAY_TITLES[resultType] || ''
}

/**
 * 统一解析入口
 * @param {object|string} source 执行记录（log）或结果载荷
 * @returns {{ resultType, outputText, outputJson, displayTitle, summary }}
 */
export function resolveResultView(source) {
  if (!source) return emptyView()

  const view = emptyView()

  // ===== 1. 规范化结构 { content, fullData, raw } =====
  if (isNormalizedShape(source)) {
    const raw = source.raw
    if (typeof raw === 'string') {
      const parsed = tryParseJson(raw)
      if (parsed) {
        view.outputJson = extractJson(parsed) || view.outputJson
        view.outputText = extractText(parsed) || (typeof source.content === 'string' ? source.content.trim() : '')
        view.resultType = normalizeType(parsed.type || parsed.resultType || source.resultType)
        view.displayTitle = extractTitle(parsed, view.resultType)
        view.summary = extractSummary(parsed)
        return view
      }
      view.outputText = (typeof source.content === 'string' && source.content.trim())
        ? source.content.trim() : raw.trim()
      view.resultType = normalizeType(source.resultType)
      return view
    }
    if (raw && typeof raw === 'object') {
      view.outputJson = extractJson(raw) || extractJson(source.fullData) || raw
      view.outputText = extractText(raw) || (typeof source.content === 'string' ? source.content.trim() : '')
      view.resultType = normalizeType(raw.type || raw.resultType || source.resultType)
      view.displayTitle = extractTitle(raw, view.resultType)
      view.summary = extractSummary(raw)
      return view
    }
    view.outputText = typeof source.content === 'string' ? source.content.trim() : ''
    view.resultType = normalizeType(source.resultType)
    return view
  }

  // ===== 2. 字符串：JSON 载荷 / Markdown 文本 =====
  if (typeof source === 'string') {
    const parsed = tryParseJson(source)
    if (parsed) {
      view.outputJson = extractJson(parsed)
      view.outputText = extractText(parsed)
      view.resultType = normalizeType(parsed.type || parsed.resultType)
      view.displayTitle = extractTitle(parsed, view.resultType)
      view.summary = extractSummary(parsed)
      return view
    }
    view.outputText = source.trim()
    return view
  }

  // ===== 3. 后端实体形态 { output / resultContent } =====
  if (!isPayloadLike(source) && (source.output || source.resultContent)) {
    const raw = source.output || source.resultContent
    view.resultType = normalizeType(source.resultType || source.type)
    if (typeof raw === 'string') {
      const parsed = tryParseJson(raw)
      if (parsed) {
        view.outputJson = extractJson(parsed)
        view.outputText = extractText(parsed)
        view.resultType = normalizeType(parsed.type || parsed.resultType || view.resultType)
        view.displayTitle = extractTitle(parsed, view.resultType)
        view.summary = extractSummary(parsed)
        return view
      }
      view.outputText = raw.trim()
      return view
    }
    if (raw && typeof raw === 'object') {
      view.outputJson = extractJson(raw)
      view.outputText = extractText(raw)
      view.resultType = normalizeType(raw.type || raw.resultType || view.resultType)
      view.displayTitle = extractTitle(raw, view.resultType)
      view.summary = extractSummary(raw)
      return view
    }
  }

  // ===== 4. 普通载荷对象 =====
  if (typeof source === 'object') {
    view.outputJson = extractJson(source)
    view.outputText = extractText(source)
    view.resultType = normalizeType(source.type || source.resultType)
    view.displayTitle = extractTitle(source, view.resultType)
    view.summary = extractSummary(source)
    // detail 形态：{ resultType, result: 嵌套载荷 }（如后端详情接口返回），递归解析嵌套载荷
    if (!view.outputJson && !view.outputText && source.result && typeof source.result === 'object') {
      const nested = resolveResultView(source.result)
      if (nested.outputText || nested.outputJson || nested.displayTitle) {
        view.outputJson = nested.outputJson
        view.outputText = nested.outputText
        view.displayTitle = nested.displayTitle || view.displayTitle
        view.summary = nested.summary || view.summary
        if (view.resultType === 'default') view.resultType = nested.resultType
        return view
      }
    }
    // 文本本身是 JSON 但未提取出结构化数据：尝试解析作为 outputJson
    if (!view.outputJson && view.outputText) {
      const parsed = tryParseJson(view.outputText)
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed) === false) {
        // 仅当解析结果包含常见结构化键时才提升为 outputJson，避免误判纯文本
        const keys = parsed && typeof parsed === 'object' ? Object.keys(parsed) : []
        const looksStructured = keys.some(k => ['questions', 'phases', 'dimensions', 'results', 'blocks', 'metrics', 'items'].includes(k))
        if (looksStructured) {
          view.outputJson = parsed
          view.outputText = ''
        }
      }
    }
    // 兜底：仍无任何可展示内容时，将整个载荷对象作为 JSON 展示
    // （对齐工具调用记录的直接 JSON 展示方式，避免平铺结构如 {type,title,phases} 显示空白）
    if (!view.outputJson && !view.outputText) {
      view.outputJson = source
    }
    return view
  }

  return view
}

/**
 * 将统一视图转为历史列表摘要文本（供列表/卡片展示）
 */
export function viewToSummaryText(view) {
  if (!view) return ''
  return view.summary || view.displayTitle || (view.outputText ? (view.outputText.length > 50 ? view.outputText.substring(0, 50) + '...' : view.outputText) : '')
}
