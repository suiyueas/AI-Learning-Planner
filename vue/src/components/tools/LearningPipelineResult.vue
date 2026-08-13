<template>
  <div class="pipeline-result">
    <!-- 流水线总览 -->
    <div class="pipeline-overview">
      <span class="pipeline-emoji">🎓</span>
      <span class="pipeline-title">智能学习闭环</span>
      <span v-if="executionTime" class="pipeline-duration">
        总耗时 {{ formatDuration(executionTime) }}
      </span>
    </div>

    <!-- 流水线步骤条 -->
    <div class="pipeline-steps">
      <template v-for="(step, idx) in stepDefs" :key="step.key">
        <div class="pipeline-step" :class="{ done: isStepDone(step.key), current: false }">
          <div class="step-icon">{{ isStepDone(step.key) ? '✅' : '⏳' }}</div>
          <div class="step-name">{{ step.name }}</div>
        </div>
        <div v-if="idx < stepDefs.length - 1" class="pipeline-arrow">→</div>
      </template>
    </div>

    <!-- Step 1: 文档摘要 -->
    <div v-if="summary" class="pipeline-card">
      <div class="card-head">
        <span class="card-tag">STEP 1</span>
        <span class="card-title">📝 文档摘要</span>
        <span v-if="summary.fallback" class="card-badge">降级</span>
      </div>
      <div class="card-body">
        <p class="summary-text">{{ summary.summary }}</p>
        <div v-if="Array.isArray(summary.keywords) && summary.keywords.length" class="kw-row">
          <span v-for="(kw, i) in summary.keywords" :key="i" class="kw-tag">{{ kw }}</span>
        </div>
        <div v-if="summary.readTimeEstimate" class="meta-row">
          <span class="meta-item">⏱️ 阅读约 {{ summary.readTimeEstimate }}</span>
        </div>
      </div>
    </div>

    <!-- Step 2: 知识点提取 -->
    <div v-if="knowledgePoints" class="pipeline-card">
      <div class="card-head">
        <span class="card-tag">STEP 2</span>
        <span class="card-title">🏷️ 知识点提取</span>
        <span class="card-count">{{ pointCount }} 个</span>
      </div>
      <div class="card-body">
        <div v-if="pointList.length" class="point-list">
          <div v-for="(p, i) in pointList" :key="i" class="point-item">
            <span class="point-kw">{{ p.keyword || p.name || p }}</span>
            <span v-if="p.description" class="point-desc">{{ p.description }}</span>
            <span v-if="p.importance" class="point-importance">⭐ {{ p.importance }}</span>
          </div>
        </div>
        <div v-if="knowledgePoints.graphLinkedCount" class="meta-row">
          <span class="meta-item">🔗 关联知识图谱 {{ knowledgePoints.graphLinkedCount }} 个节点</span>
        </div>
      </div>
    </div>

    <!-- Step 3: 测验题 -->
    <div v-if="quiz" class="pipeline-card">
      <div class="card-head">
        <span class="card-tag">STEP 3</span>
        <span class="card-title">✏️ 测验题</span>
        <span class="card-count">{{ quizCount }} 道</span>
      </div>
      <div class="card-body">
        <div v-if="quizList.length" class="quiz-list">
          <div v-for="(q, i) in quizList" :key="i" class="quiz-item">
            <div class="quiz-q">
              <span class="quiz-idx">{{ i + 1 }}.</span>
              <span class="quiz-text">{{ q.question }}</span>
              <span v-if="q.type" class="quiz-type">{{ typeLabel(q.type) }}</span>
            </div>
            <div v-if="Array.isArray(q.options) && q.options.length" class="quiz-options">
              <span
v-for="(opt, oi) in q.options" :key="oi" class="quiz-opt"
                :class="{ correct: opt === q.answer }"
>
                {{ opt }}
              </span>
            </div>
            <div v-if="q.answer" class="quiz-answer">✅ 答案：{{ q.answer }}</div>
            <div v-if="q.explanation" class="quiz-exp">💡 {{ q.explanation }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下一步建议 -->
    <div v-if="nextSteps.length" class="pipeline-card next-card">
      <div class="card-head">
        <span class="card-tag">NEXT</span>
        <span class="card-title">🚀 下一步建议</span>
      </div>
      <div class="card-body">
        <ul class="next-list">
          <li v-for="(s, i) in nextSteps" :key="i" class="next-item">{{ s }}</li>
        </ul>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!summary && !knowledgePoints && !quiz" class="result-empty">
      ⚠️ 未获取到学习报告数据
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'

const props = defineProps({
  data: {
    type: [Object, Array],
    default: null
  }
})

const stepDefs = [
  { key: 'summary', name: '文档摘要' },
  { key: 'knowledgePoints', name: '知识点提取' },
  { key: 'quiz', name: '生成测验题' }
]

const steps = computed(() => {
  if (!props.data) return {}
  if (props.data.steps) return props.data.steps
  return props.data
})

const summary = computed(() => steps.value.summary || null)
const knowledgePoints = computed(() => steps.value.knowledgePoints || null)
const quiz = computed(() => steps.value.quiz || null)
const nextSteps = computed(() => {
  if (!props.data) return []
  if (Array.isArray(props.data.nextSteps)) return props.data.nextSteps
  return []
})
const executionTime = computed(() => props.data?.executionTimeMs || null)

const pointList = computed(() => {
  const pts = knowledgePoints.value?.knowledgePoints
  if (Array.isArray(pts)) return pts
  return []
})
const pointCount = computed(() => pointList.value.length || knowledgePoints.value?.count || 0)

const quizList = computed(() => {
  const qs = quiz.value?.questions
  if (Array.isArray(qs)) return qs
  return []
})
const quizCount = computed(() => quizList.value.length || quiz.value?.count || 0)

function isStepDone(key) {
  if (key === 'summary') return !!summary.value
  if (key === 'knowledgePoints') return !!knowledgePoints.value
  if (key === 'quiz') return !!quiz.value
  return false
}

function typeLabel(type) {
  const map = { choice: '选择题', judgment: '判断题', fill: '填空题', mixed: '混合' }
  return map[type] || type
}

function formatDuration(ms) {
  if (ms == null) return ''
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}

onMounted(() => {
  // 若数据是字符串（异常返回），直接透传显示
  if (typeof props.data === 'string') {
    // 保持空态由模板处理
  }
})
</script>

<style lang="scss" scoped>
.pipeline-result {
  padding: 4px 0;
}

.pipeline-overview {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.pipeline-emoji { font-size: 1.2rem; }

.pipeline-title {
  font-size: 14px;
  font-weight: 600;
  color: #f0f0ff;
  font-family: 'JetBrains Mono', monospace;
}

.pipeline-duration {
  margin-left: auto;
  font-size: 11px;
  color: #8080a8;
  font-family: 'JetBrains Mono', monospace;
}

/* 步骤条 */
.pipeline-steps {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}

.pipeline-step {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 8px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.1);

  &.done {
    background: rgba(0, 245, 212, 0.06);
    border-color: rgba(0, 245, 212, 0.15);
  }
}

.step-icon { font-size: 12px; }

.step-name {
  font-size: 11px;
  color: #c0c0e0;
  white-space: nowrap;
}

.pipeline-arrow {
  color: #606090;
  font-size: 12px;
}

/* 分步卡片 */
.pipeline-card {
  padding: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  margin-bottom: 12px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.card-tag {
  font-size: 10px;
  font-weight: 700;
  color: #00f5d4;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.5px;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: #e8e8ff;
}

.card-count {
  margin-left: auto;
  font-size: 11px;
  color: #8080a8;
  font-family: 'JetBrains Mono', monospace;
}

.card-badge {
  margin-left: auto;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 6px;
  color: #ffb86c;
  background: rgba(255, 184, 108, 0.1);
  border: 1px solid rgba(255, 184, 108, 0.2);
}

.card-body {
  font-size: 12px;
  color: #c0c0e0;
}

.summary-text {
  line-height: 1.7;
  margin: 0 0 10px;
  white-space: pre-wrap;
}

.kw-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.kw-tag {
  padding: 2px 8px;
  background: rgba(0, 245, 212, 0.06);
  border: 1px solid rgba(0, 245, 212, 0.1);
  border-radius: 6px;
  font-size: 11px;
  color: #00f5d4;
  font-family: 'JetBrains Mono', monospace;
}

.meta-row {
  margin-top: 8px;
}

.meta-item {
  font-size: 11px;
  color: #8080a8;
}

/* 知识点 */
.point-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.point-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 6px 10px;
  background: rgba(100, 100, 180, 0.04);
  border-radius: 8px;
}

.point-kw {
  font-weight: 600;
  color: #ffb86c;
  font-size: 12px;
}

.point-desc {
  flex: 1;
  color: #9090b8;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.point-importance {
  font-size: 10px;
  color: #ff6b9d;
}

/* 测验题 */
.quiz-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quiz-item {
  padding: 10px;
  background: rgba(100, 100, 180, 0.04);
  border-radius: 8px;
}

.quiz-q {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 6px;
}

.quiz-idx {
  color: #00f5d4;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

.quiz-text {
  flex: 1;
  color: #d0d0f0;
  line-height: 1.5;
}

.quiz-type {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  color: #3a86ff;
  background: rgba(58, 134, 255, 0.1);
  white-space: nowrap;
}

.quiz-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.quiz-opt {
  padding: 2px 8px;
  font-size: 11px;
  color: #9090b8;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 6px;

  &.correct {
    color: #00f5d4;
    background: rgba(0, 245, 212, 0.08);
    border: 1px solid rgba(0, 245, 212, 0.15);
  }
}

.quiz-answer {
  font-size: 11px;
  color: #00f5d4;
  margin-bottom: 4px;
}

.quiz-exp {
  font-size: 11px;
  color: #8080a8;
  line-height: 1.5;
}

/* 下一步建议 */
.next-card {
  border-color: rgba(58, 134, 255, 0.15);
}

.next-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.next-item {
  font-size: 12px;
  color: #a0a0c8;
  line-height: 1.5;
}

.result-empty {
  text-align: center;
  padding: 24px;
  color: #8080a8;
  font-size: 13px;
}
</style>
