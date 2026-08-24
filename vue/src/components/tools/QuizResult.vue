<template>
  <div class="quiz-result">
    <div class="result-header">
      <div v-if="summary" class="result-summary">
        <span class="summary-icon">✏️</span>
        <span class="summary-text">{{ summary }}</span>
      </div>
      <div class="result-actions">
        <button class="action-btn" title="复制结果" @click="handleCopy">
          <span>📋</span> 复制
        </button>
        <button class="action-btn" title="导出JSON" @click="handleExport">
          <span>📥</span> 导出
        </button>
        <button class="action-btn" title="查看原始JSON" @click="showRawJson = !showRawJson">
          <span>📄</span> JSON
        </button>
      </div>
    </div>

    <div v-if="questions && questions.length > 0" class="question-list">
      <div
        v-for="(q, idx) in questions"
        :key="idx"
        class="question-card"
        :class="{ 'is-expanded': expandedItems[idx] }"
      >
        <div class="question-header" @click="toggleItem(idx)">
          <span class="q-number">#{{ idx + 1 }}</span>
          <span class="q-text">{{ q.question || q.title || q.text || `题目 ${idx + 1}` }}</span>
          <span class="q-toggle">{{ expandedItems[idx] ? '▼' : '▶' }}</span>
        </div>

        <div v-if="expandedItems[idx]" class="question-body">
          <!-- 选项 -->
          <div v-if="q.options && q.options.length > 0" class="q-options">
            <div
              v-for="(opt, oi) in q.options"
              :key="oi"
              class="q-option"
              :class="{ 
                'is-selected': selectedOptions[idx] === optionLabel(opt, oi),
                'is-correct': showAnswer[idx] && (opt.isCorrect || opt.correct),
                'is-wrong': showAnswer[idx] && selectedOptions[idx] === optionLabel(opt, oi) && !(opt.isCorrect || opt.correct)
              }"
              @click="handleOptionClick(idx, opt, oi)"
            >
              <span class="opt-marker">{{ String.fromCharCode(65 + oi) }}</span>
              <span class="opt-text">{{ opt.label || opt.text || opt }}</span>
              <span v-if="showAnswer[idx] && (opt.isCorrect || opt.correct)" class="opt-correct">✓</span>
            </div>
          </div>

          <!-- 答案区域（可折叠） -->
          <div class="q-answer-area">
            <button class="q-answer-toggle" @click.stop="toggleAnswer(idx)">
              {{ showAnswer[idx] ? '🙈 隐藏答案' : '👁️ 查看答案' }}
            </button>
            <div v-if="showAnswer[idx]" class="q-answer">
              <div class="q-answer-label">正确答案：</div>
              <div class="q-answer-text">{{ q.answer || q.correctAnswer || q.correct_answer || '未提供' }}</div>
              <div v-if="q.explanation" class="q-explanation">
                <span class="q-explanation-label">解析：</span>
                {{ q.explanation }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showRawJson" class="raw-content">
      <div class="raw-label">原始JSON数据</div>
      <pre class="raw-json">{{ formattedRawJson }}</pre>
    </div>

    <div v-else-if="showRaw && rawContent" class="raw-content">
      <div class="raw-label">返回数据</div>
      <pre class="raw-json">{{ rawContent }}</pre>
    </div>

    <!-- 空态仅当确实没有题目时显示（与题目列表互斥，避免有题目时误显示“未生成题目数据”） -->
    <div v-else-if="!questions || questions.length === 0" class="result-empty">⚠️ 未生成题目数据</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  data: {
    type: [Object, Array],
    default: null
  }
})

const expandedItems = ref({})
const showAnswer = ref({})
const selectedOptions = ref({})
const showRawJson = ref(false)

/**
 * 从各种响应结构中提取题目列表
 * 兼容：裸数组 / { questions } / { success, data: { questions } } / { data: { questions } } 等包装结构
 */
const resolveQuestions = (data) => {
  if (!data) return null
  if (Array.isArray(data)) return data
  if (data.questions) return data.questions
  if (data.quizzes) return data.quizzes
  if (data.exercises) return data.exercises
  if (data.items) return data.items
  // 兼容 { success, data: {...} } / { data: {...} } 包装
  if (data.data && typeof data.data === 'object') {
    if (Array.isArray(data.data)) return data.data
    if (data.data.questions) return data.data.questions
    if (data.data.quizzes) return data.data.quizzes
    if (data.data.exercises) return data.data.exercises
    if (data.data.items) return data.data.items
  }
  return null
}

const questions = computed(() => resolveQuestions(props.data))

const summary = computed(() => {
  if (!props.data) return null
  if (props.data.summary) return props.data.summary
  if (props.data.message) return props.data.message
  // 兼容 { success, data: {...} } 包装结构
  if (props.data.data && typeof props.data.data === 'object') {
    if (props.data.data.summary) return props.data.data.summary
    if (props.data.data.message) return props.data.data.message
  }
  if (questions.value && questions.value.length > 0) {
    return `已生成 ${questions.value.length} 道题目`
  }
  return null
})

const showRaw = ref(false)
const rawContent = ref('')

const formattedRawJson = computed(() => {
  if (!props.data) return ''
  try {
    return typeof props.data === 'string' 
      ? props.data 
      : JSON.stringify(props.data, null, 2)
  } catch {
    return String(props.data)
  }
})

onMounted(() => {
  if (!questions.value && props.data) {
    showRaw.value = true
    try {
      rawContent.value = typeof props.data === 'string'
        ? props.data
        : JSON.stringify(props.data, null, 2)
    } catch {
      rawContent.value = String(props.data)
    }
  }
})

const optionLabel = (opt, oi) => String.fromCharCode(65 + oi)

function toggleItem(idx) {
  expandedItems.value[idx] = !expandedItems.value[idx]
}

function toggleAnswer(idx) {
  showAnswer.value[idx] = !showAnswer.value[idx]
}

function handleOptionClick(idx, opt, oi) {
  const label = optionLabel(opt, oi)
  if (selectedOptions.value[idx] === label) {
    delete selectedOptions.value[idx]
  } else {
    selectedOptions.value[idx] = label
  }
}

async function handleCopy() {
  try {
    const text = formattedRawJson.value
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = formattedRawJson.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('已复制到剪贴板')
  }
}

function handleExport() {
  const blob = new Blob([formattedRawJson.value], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `quiz_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已开始下载 JSON 文件')
}
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;
.quiz-result {
  padding: 4px 0;
}

.result-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(123, 97, 255, 0.04);
  border: 1px solid rgba(123, 97, 255, 0.08);
  border-radius: 10px;
  margin-bottom: 12px;
  font-size: 13px;
  color: $text-secondary;
  .summary-icon { font-size: 1.1rem; }
  .summary-text { font-weight: 500; }
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.question-card {
  padding: 0;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.25s ease;

  &:hover {
    border-color: rgba(123, 97, 255, 0.15);
  }

  &.is-expanded {
    border-color: rgba(123, 97, 255, 0.15);
    background: rgba(255, 255, 255, 0.05);
  }
}

.question-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.03);
  }
}

.q-number {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(123, 97, 255, 0.1);
  color: $accent-purple;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

.q-text {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #e0e0f0;
  line-height: 1.4;
}

.q-toggle {
  flex-shrink: 0;
  color: $text-muted;
  font-size: 10px;
  transition: transform 0.2s;
}

.question-body {
  padding: 0 16px 14px;
}

.q-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.q-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.06);
  border-radius: 8px;
  font-size: 12px;
  color: #b0b0d0;
  transition: all 0.2s;

  &.is-correct {
    background: rgba(16, 185, 129, 0.06);
    border-color: rgba(16, 185, 129, 0.15);
    color: #d0f0e0;
  }
}

.opt-marker {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($accent-secondary, 0.08);
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  color: $text-muted;
  font-family: 'JetBrains Mono', monospace;

  .is-correct & {
    background: rgba(16, 185, 129, 0.15);
    color: $accent-emerald;
  }
}

.opt-text { flex: 1; line-height: 1.4; }

.opt-correct {
  flex-shrink: 0;
  color: $accent-emerald;
  font-weight: 700;
}

.q-answer-area {
  margin-top: 8px;
}

.q-answer-toggle {
  padding: 6px 14px;
  background: rgba($accent-secondary, 0.06);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 6px;
  color: $text-muted;
  font-size: 11px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: rgba($accent-primary, 0.2);
    color: $accent-primary;
    background: rgba($accent-primary, 0.06);
  }
}

.q-answer {
  margin-top: 10px;
  padding: 12px;
  background: rgba(16, 185, 129, 0.04);
  border: 1px solid rgba(16, 185, 129, 0.1);
  border-radius: 8px;
}

.q-answer-label {
  font-size: 11px;
  font-weight: 600;
  color: $accent-emerald;
  margin-bottom: 4px;
}

.q-answer-text {
  font-size: 13px;
  color: #d0f0e0;
  font-weight: 500;
}

.q-explanation {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(16, 185, 129, 0.08);
  font-size: 12px;
  color: $text-muted;
  line-height: 1.5;
}

.q-explanation-label {
  color: $text-muted;
  font-weight: 600;
}

.raw-content {
  margin-top: 8px;
  .raw-label {
    font-size: 12px;
    font-weight: 600;
    color: $text-muted;
    margin-bottom: 8px;
  }
  .raw-json {
    padding: 12px;
    background: rgba($accent-secondary, 0.04);
    border: 1px solid rgba($accent-secondary, 0.08);
    border-radius: 8px;
    font-size: 12px;
    line-height: 1.6;
    color: $text-secondary;
    overflow-x: auto;
    font-family: 'JetBrains Mono', monospace;
    white-space: pre-wrap;
    word-break: break-all;
    margin: 0;
    max-height: 300px;
    overflow-y: auto;
  }
}

.result-empty {
  text-align: center;
  padding: 24px;
  color: $text-muted;
  font-size: 13px;
}

.result-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.result-summary {
  flex: 1;
  min-width: 200px;
  margin-bottom: 0;
}

.result-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  background: rgba($accent-secondary, 0.06);
  border: 1px solid rgba($accent-secondary, 0.1);
  border-radius: 6px;
  color: $text-muted;
  font-size: 11px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    border-color: rgba($accent-primary, 0.2);
    color: $accent-primary;
    background: rgba($accent-primary, 0.06);
  }

  span {
    font-size: 12px;
  }
}

.q-option {
  cursor: pointer;

  &.is-selected {
    background: rgba(74, 144, 249, 0.08);
    border-color: rgba(74, 144, 249, 0.25);
    color: #e0f0ff;
  }

  &.is-wrong {
    background: rgba(255, 77, 77, 0.08);
    border-color: rgba(255, 77, 77, 0.25);
    color: #ffe0e0;
  }
}
</style>