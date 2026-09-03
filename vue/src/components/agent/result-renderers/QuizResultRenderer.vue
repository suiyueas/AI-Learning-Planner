<template>
  <!-- 习题结果渲染：复用工具页已实现的 QuizResult 交互组件 -->
  <QuizResultView v-if="hasQuestions" :data="quizData" />
  <!-- 无结构化题目数据时，回退为 Markdown 文本展示 -->
  <div v-else-if="text" class="quiz-result-fallback" v-html="renderedText"></div>
  <div v-else class="quiz-result-empty">暂无题目数据</div>
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'
import QuizResultView from '@/components/tools/QuizResult.vue'

const props = defineProps({
  data: { type: [Object, Array], default: null },
  text: { type: String, default: '' }
})

/** 从各种结构提取题目列表（与 tools/QuizResult 兼容） */
const questions = computed(() => {
  const d = props.data
  if (!d) return []
  if (Array.isArray(d)) return d
  if (Array.isArray(d.questions)) return d.questions
  if (Array.isArray(d.quizzes)) return d.quizzes
  if (Array.isArray(d.exercises)) return d.exercises
  if (Array.isArray(d.items)) return d.items
  if (Array.isArray(d.data?.questions)) return d.data.questions
  return []
})

const hasQuestions = computed(() => questions.value.length > 0)

/** 传给 QuizResultView 的题目结构（优先 outputJson 直传） */
const quizData = computed(() => {
  const d = props.data
  if (!d) return null
  if (Array.isArray(d) || d.questions || d.quizzes || d.exercises) return d
  return { questions: questions.value }
})

const renderedText = computed(() => renderMarkdown(props.text || ''))
</script>

<style lang="scss" scoped>
.quiz-result-fallback {
  padding: 14px; background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08); border-radius: 10px;
  font-size: 13px; line-height: 1.8; color: #c0c0e0;
  max-height: 420px; overflow-y: auto;
  :deep(pre) { background: rgba(0, 0, 0, 0.3); padding: 12px; border-radius: 8px; overflow-x: auto; }
  :deep(code) { background: rgba(100, 100, 180, 0.12); padding: 2px 6px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; font-size: 12px; color: #00f5d4; }
  :deep(pre code) { background: none; padding: 0; color: #c0c0e0; }
}
.quiz-result-empty { text-align: center; padding: 24px; color: #606090; font-size: 13px; }
</style>