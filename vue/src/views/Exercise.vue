<template>
  <div class="exercise-page">
    <div class="bg-layer">
      <div class="bg-aurora">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <div class="bg-grid"></div>
    </div>

    <header class="page-header">
      <button class="back-btn" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" /></svg>
        <span>返回</span>
      </button>
      <h1 class="page-title">
        <span class="title-icon">📝</span>
        <span class="title-text">习题生成</span>
      </h1>
      <button class="generate-btn" :disabled="generating" @click="generateExercises">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="23 4 23 10 17 10" /><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10" /></svg>
        <span>{{ generating ? '生成中...' : '生成习题' }}</span>
      </button>
    </header>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-icon total">📊</span>
        <div class="stat-info"><span class="stat-num">{{ exercises.length }}</span><span class="stat-label">总习题</span></div>
      </div>
      <div class="stat-card">
        <span class="stat-icon completed">✅</span>
        <div class="stat-info"><span class="stat-num">{{ completedCount }}</span><span class="stat-label">已掌握</span></div>
      </div>
      <div class="stat-card">
        <span class="stat-icon wrong">❌</span>
        <div class="stat-info"><span class="stat-num">{{ wrongCount }}</span><span class="stat-label">待复习</span></div>
      </div>
      <div class="stat-card">
        <span class="stat-icon rate">🎯</span>
        <div class="stat-info"><span class="stat-num">{{ accuracy }}%</span><span class="stat-label">正确率</span></div>
      </div>
    </div>

    <div class="filter-bar">
      <button v-for="f in filters" :key="f.id" class="filter-btn" :class="{ active: activeFilter === f.id }" @click="activeFilter = f.id">
        <span class="filter-dot" :class="f.color"></span>
        <span>{{ f.label }}</span>
        <span class="filter-count">{{ f.count }}</span>
      </button>
    </div>

    <TransitionGroup name="exercise-list" tag="div" class="exercise-list">
      <div v-for="ex in filteredExercises" :key="ex.id" class="exercise-card" :class="{ expanded: expandedId === ex.id }" @click="toggleExpand(ex.id)">
        <div class="ex-header">
          <span class="ex-tag" :class="ex.difficulty">{{ diffLabels[ex.difficulty] }}</span>
          <span class="ex-category">{{ ex.category }}</span>
          <span class="ex-date">{{ formatDate(ex.createdAt) }}</span>
        </div>
        <h3 class="ex-title">{{ ex.title }}</h3>
        <p class="ex-question">{{ ex.question }}</p>
        <div v-if="expandedId === ex.id" class="ex-answer-area">
          <div v-if="ex.type === 'choice'" class="ex-options">
            <label v-for="(opt, i) in ex.options" :key="i" class="option-item" :class="{ selected: selectedAnswers[ex.id] === i, correct: showAnswers && i === ex.answer, wrong: showAnswers && selectedAnswers[ex.id] === i && i !== ex.answer }" @click.stop="selectAnswer(ex.id, i)">
              <span class="option-marker">{{ ['A','B','C','D'][i] }}</span>
              <span class="option-text">{{ opt }}</span>
            </label>
          </div>
          <div v-else class="ex-text-answer">
            <textarea v-model="textAnswers[ex.id]" class="answer-input" placeholder="输入你的答案..." rows="3" @click.stop></textarea>
          </div>
          <div v-if="!showAnswers" class="ex-actions">
            <button class="btn-submit" @click.stop="submitAnswer(ex)">提交答案</button>
          </div>
          <div v-if="showAnswers" class="ex-result" :class="ex.userCorrect ? 'correct' : 'wrong'">
            <span class="result-icon">{{ ex.userCorrect ? '✅' : '❌' }}</span>
            <span class="result-text">{{ ex.userCorrect ? '回答正确！' : '回答错误' }}</span>
            <span v-if="!ex.userCorrect" class="result-hint">正确答案：{{ ex.type === 'choice' ? ['A','B','C','D'][ex.answer] : ex.answer }}</span>
          </div>
          <div v-if="showAnswers && ex.explanation" class="ex-explanation">
            <span class="explain-label">💡 解析</span>
            <p>{{ ex.explanation }}</p>
          </div>
        </div>
        <div class="ex-footer">
          <span class="ex-status" :class="ex.status">{{ statusLabels[ex.status] }}</span>
          <button class="btn-delete" title="删除" @click.stop="deleteExercise(ex.id)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" /></svg>
          </button>
        </div>
      </div>
    </TransitionGroup>

    <div v-if="filteredExercises.length === 0" class="empty-state">
      <div class="empty-icon-wrap">📝</div>
      <h3 class="empty-title">{{ exercises.length === 0 ? '还没有习题' : '没有匹配的习题' }}</h3>
      <p class="empty-desc">{{ exercises.length === 0 ? '点击上方「生成习题」按钮开始练习' : '试试其他筛选条件' }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const STORAGE_KEY = 'ai_exercise_data'
const exercises = ref([])
const generating = ref(false)
const expandedId = ref(null)
const showAnswers = ref(false)
const selectedAnswers = ref({})
const textAnswers = ref({})
const activeFilter = ref('all')

const diffLabels = { easy: '简单', medium: '中等', hard: '困难' }
const statusLabels = { pending: '待做', completed: '已掌握', wrong: '待复习' }

const filters = computed(() => [
  { id: 'all', label: '全部', count: exercises.value.length, color: 'cyan' },
  { id: 'pending', label: '待做', count: exercises.value.filter(e => e.status === 'pending').length, color: 'gray' },
  { id: 'completed', label: '已掌握', count: exercises.value.filter(e => e.status === 'completed').length, color: 'green' },
  { id: 'wrong', label: '待复习', count: exercises.value.filter(e => e.status === 'wrong').length, color: 'red' }
])

const filteredExercises = computed(() => {
  if (activeFilter.value === 'all') return exercises.value
  return exercises.value.filter(e => e.status === activeFilter.value)
})

const completedCount = computed(() => exercises.value.filter(e => e.status === 'completed').length)
const wrongCount = computed(() => exercises.value.filter(e => e.status === 'wrong').length)
const accuracy = computed(() => {
  const answered = exercises.value.filter(e => e.status === 'completed' || e.status === 'wrong')
  return answered.length ? Math.round((completedCount.value / answered.length) * 100) : 0
})

function generateMockExercises() {
  const topics = [
    { title: 'Python 列表推导式', category: 'Python', difficulty: 'easy', options: ['[x*2 for x in range(5)]', '(x*2 for x in range(5))', '{x*2 for x in range(5)}', 'x*2 for x in range(5)'], answer: 0, explanation: '列表推导式使用方括号 []，生成一个列表。' },
    { title: 'Vue 3 响应式原理', category: 'Vue', difficulty: 'medium', options: ['Object.defineProperty', 'Proxy', 'Object.observe', 'Reflect'], answer: 1, explanation: 'Vue 3 使用 Proxy 实现响应式系统，比 Vue 2 的 Object.defineProperty 更强大。' },
    { title: 'HTTP 状态码含义', category: '网络', difficulty: 'easy', options: ['200 表示资源未找到', '404 表示服务器错误', '500 表示请求成功', '301 表示永久重定向'], answer: 3, explanation: '301 Moved Permanently 表示请求的资源已被永久移动到新位置。' },
    { title: 'CSS Flexbox 对齐', category: 'CSS', difficulty: 'medium', options: ['justify-content 控制交叉轴', 'align-items 控制主轴', 'justify-content 控制主轴', 'align-content 控制单个元素'], answer: 2, explanation: 'justify-content 控制主轴方向的对齐，align-items 控制交叉轴方向。' },
    { title: 'Git 分支合并', category: '工具', difficulty: 'medium', options: ['git merge 和 git rebase 作用相同', 'git rebase 会保留分支历史', 'git merge 会创建合并提交', 'git rebase 比 merge 更安全'], answer: 2, explanation: 'git merge 会创建一个新的合并提交来整合分支。' },
    { title: '数据库索引原理', category: '数据库', difficulty: 'hard', options: ['索引越多查询越快', 'B+树索引支持范围查询', '哈希索引支持排序', '全文索引用于精确匹配'], answer: 1, explanation: 'B+树索引天然支持范围查询（> < BETWEEN），这是其重要特性。' },
    { title: 'JavaScript 闭包', category: 'JavaScript', difficulty: 'hard', options: ['闭包会泄漏内存', '闭包可以访问外部函数变量', '闭包只能访问全局变量', '箭头函数不能形成闭包'], answer: 1, explanation: '闭包是指内部函数可以访问外部函数作用域中变量的能力。' },
    { title: 'React Hooks 规则', category: 'React', difficulty: 'medium', options: ['Hooks 可以在条件语句中使用', 'Hooks 只能在函数组件顶层调用', 'Hooks 可以在循环中使用', 'Hooks 可以在类组件中使用'], answer: 1, explanation: 'React Hooks 必须在函数组件的最顶层调用，不能在条件、循环或嵌套函数中使用。' },
  ]
  const selected = topics.sort(() => Math.random() - 0.5).slice(0, 4 + Math.floor(Math.random() * 3))
  return selected.map((t, i) => ({
    id: 'ex-' + Date.now() + '-' + i,
    title: t.title,
    question: '以下关于「' + t.title + '」的描述，哪一项是正确的？',
    category: t.category,
    difficulty: t.difficulty,
    type: 'choice',
    options: t.options,
    answer: t.answer,
    explanation: t.explanation,
    status: 'pending',
    userCorrect: null,
    createdAt: Date.now() - Math.floor(Math.random() * 86400000 * 7)
  }))
}

function generateExercises() {
  generating.value = true
  showAnswers.value = false
  expandedId.value = null
  setTimeout(() => {
    const newExs = generateMockExercises()
    exercises.value = [...newExs, ...exercises.value]
    saveData()
    generating.value = false
  }, 800)
}

function toggleExpand(id) {
  if (expandedId.value === id) {
    expandedId.value = null
    showAnswers.value = false
  } else {
    expandedId.value = id
    showAnswers.value = false
    selectedAnswers.value = {}
  }
}

function selectAnswer(exId, idx) {
  selectedAnswers.value[exId] = idx
}

function submitAnswer(ex) {
  const exRef = exercises.value.find(e => e.id === ex.id)
  if (!exRef) return
  showAnswers.value = true
  if (ex.type === 'choice') {
    exRef.userCorrect = selectedAnswers.value[ex.id] === ex.answer
  } else {
    exRef.userCorrect = false
  }
  exRef.status = exRef.userCorrect ? 'completed' : 'wrong'
  saveData()
}

function deleteExercise(id) {
  exercises.value = exercises.value.filter(e => e.id !== id)
  saveData()
}

function loadData() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      exercises.value = JSON.parse(saved)
    }
  } catch (e) { console.error('加载习题数据失败:', e) }
}

function saveData() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(exercises.value))
}

function formatDate(ts) {
  const d = new Date(ts)
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/modules')
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;
.exercise-page {
  min-height: calc(100vh - 68px);
  background: $bg-primary;
  position: relative;
  overflow: hidden;
  padding: 0;
}
.bg-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-aurora { position: absolute; inset: 0;
  background: radial-gradient(ellipse at 70% 20%, rgba($accent-primary,0.06) 0%, transparent 50%), radial-gradient(ellipse at 30% 80%, rgba(123,97,255,0.05) 0%, transparent 50%), radial-gradient(ellipse at 50% 50%, rgba(0,85,255,0.04) 0%, transparent 50%);
  animation: auroraDrift 20s ease-in-out infinite;
}
@keyframes auroraDrift { 0%,100% { transform: scale(1) rotate(0deg); } 33% { transform: scale(1.08) rotate(0.8deg); } 66% { transform: scale(0.95) rotate(-0.6deg); } }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba($accent-primary,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(123,97,255,0.03) 1px, transparent 1px); background-size: 40px 40px; animation: gridPulse 8s ease-in-out infinite alternate; }
@keyframes gridPulse { 0% { opacity: 0.3; } 100% { opacity: 0.6; } }

.page-header {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; gap: 16px;
  padding: 16px 32px;
  background: rgba($bg-primary,0.85); backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba($accent-secondary,0.08);
}
.back-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; background: rgba($accent-secondary,0.06);
  border: 1px solid rgba($accent-secondary,0.1); border-radius: 8px;
  color: $text-secondary; font-size: 0.82rem; cursor: pointer;
  transition: all 0.25s ease;
  &:hover { border-color: rgba($accent-primary,0.2); color: $accent-primary; }
}
.page-title { flex: 1; display: flex; align-items: center; gap: 10px; }
.title-icon { font-size: 1.3rem; }
.title-text {
  font-size: 1.05rem; font-weight: 700;
  background: linear-gradient(135deg, $accent-primary, #3a86ff);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.generate-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 18px;
  background: linear-gradient(135deg, rgba($accent-primary,0.15), rgba(0,85,255,0.1));
  border: 1px solid rgba($accent-primary,0.2); border-radius: 10px;
  color: $accent-primary; font-size: 0.85rem; font-weight: 600; cursor: pointer;
  transition: all 0.25s ease;
  &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 0 20px rgba($accent-primary,0.15); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px;
  padding: 20px 32px 0; position: relative; z-index: 1;
}
.stat-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 18px;
  background: rgba($bg-primary,0.6); backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.1); border-radius: 12px;
  .stat-num { font-size: 1.4rem; font-weight: 800; font-family: 'JetBrains Mono', monospace; color: $accent-primary; }
  .stat-label { font-size: 0.7rem; color: $text-muted; }
  .stat-icon { font-size: 1.2rem; }
  .stat-info { display: flex; flex-direction: column; gap: 2px; }
}

.filter-bar {
  display: flex; gap: 8px; padding: 16px 32px;
  position: relative; z-index: 1; flex-wrap: wrap;
}
.filter-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px; background: rgba($bg-primary,0.6);
  border: 1px solid rgba($accent-secondary,0.1); border-radius: 20px;
  font-size: 13px; color: $text-secondary; cursor: pointer;
  transition: all 0.2s;
  &.active { border-color: $accent-primary; color: $accent-primary; background: rgba($accent-primary,0.08); }
}
.filter-dot { width: 8px; height: 8px; border-radius: 50%; &.cyan { background: $accent-primary; } &.gray { background: $text-muted; } &.green { background: $accent-emerald; } &.red { background: $accent-red; } }
.filter-count { font-size: 11px; padding: 0 5px; background: rgba($accent-secondary,0.1); border-radius: 8px; }

.exercise-list {
  max-width: 1200px; margin: 0 auto;
  padding: 0 32px 60px; position: relative; z-index: 1;
  display: flex; flex-direction: column; gap: 12px;
}
.exercise-card {
  padding: 18px 20px;
  background: rgba($bg-primary,0.5); backdrop-filter: blur(12px);
  border: 1px solid rgba($accent-secondary,0.08); border-radius: 14px;
  cursor: pointer; transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  &:hover { transform: translateY(-2px); border-color: rgba($accent-primary,0.15); }
  &.expanded { border-color: rgba($accent-primary,0.2); background: rgba($bg-primary,0.7); }
}
.ex-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.ex-tag {
  font-size: 0.7rem; padding: 2px 10px; border-radius: 10px; font-weight: 600;
  &.easy { background: rgba(16,185,129,0.12); color: $accent-emerald; border: 1px solid rgba(16,185,129,0.2); }
  &.medium { background: rgba(245,158,11,0.12); color: $accent-amber; border: 1px solid rgba(245,158,11,0.2); }
  &.hard { background: rgba(239,68,68,0.12); color: $accent-red; border: 1px solid rgba(239,68,68,0.2); }
}
.ex-category { font-size: 0.72rem; color: $text-muted; }
.ex-date { margin-left: auto; font-size: 0.7rem; color: $text-muted; }
.ex-title { font-size: 1rem; font-weight: 700; color: $text-primary; margin: 0 0 8px; }
.ex-question { font-size: 0.85rem; color: $text-secondary; line-height: 1.6; margin: 0; }

.ex-answer-area { margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba($accent-secondary,0.08); }
.ex-options { display: flex; flex-direction: column; gap: 8px; }
.option-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-radius: 10px;
  border: 1px solid rgba($accent-secondary,0.1);
  cursor: pointer; transition: all 0.2s;
  &:hover { border-color: rgba($accent-primary,0.15); background: rgba($accent-primary,0.03); }
  &.selected { border-color: $accent-primary; background: rgba($accent-primary,0.06); }
  &.correct { border-color: $accent-emerald; background: rgba(16,185,129,0.08); .option-marker { color: $accent-emerald; } }
  &.wrong { border-color: $accent-red; background: rgba(239,68,68,0.08); .option-marker { color: $accent-red; } }
}
.option-marker { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; border-radius: 6px; background: rgba($accent-secondary,0.08); font-size: 0.75rem; font-weight: 700; color: $text-secondary; flex-shrink: 0; }
.option-text { font-size: 0.85rem; color: $text-secondary; }

.answer-input {
  width: 100%; padding: 12px; background: rgba(0,0,0,0.2);
  border: 1px solid rgba($accent-secondary,0.1); border-radius: 10px;
  color: $text-primary; font-size: 0.85rem; outline: none; resize: vertical;
  font-family: inherit; box-sizing: border-box;
  &:focus { border-color: rgba($accent-primary,0.25); }
}

.ex-actions { margin-top: 12px; }
.btn-submit {
  padding: 8px 20px;
  background: linear-gradient(135deg, $accent-primary, #3a86ff);
  border: none; border-radius: 8px; color: #fff;
  font-size: 0.82rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s;
  &:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba($accent-primary,0.2); }
}

.ex-result {
  display: flex; align-items: center; gap: 8px; margin-top: 12px;
  padding: 10px 14px; border-radius: 10px;
  &.correct { background: rgba(16,185,129,0.08); border: 1px solid rgba(16,185,129,0.15); }
  &.wrong { background: rgba(239,68,68,0.08); border: 1px solid rgba(239,68,68,0.15); }
  .result-icon { font-size: 1rem; }
  .result-text { font-size: 0.85rem; font-weight: 600; color: $text-primary; }
  .result-hint { font-size: 0.78rem; color: $accent-red; margin-left: auto; }
}

.ex-explanation {
  margin-top: 12px; padding: 12px; border-radius: 10px;
  background: rgba(123,97,255,0.06); border: 1px solid rgba(123,97,255,0.1);
  .explain-label { font-size: 0.8rem; font-weight: 600; color: #a78bfa; }
  p { font-size: 0.82rem; color: $text-secondary; line-height: 1.6; margin: 6px 0 0; }
}

.ex-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; padding-top: 10px; border-top: 1px solid rgba($accent-secondary,0.04); }
.ex-status {
  font-size: 0.7rem; padding: 2px 10px; border-radius: 10px; font-weight: 500;
  &.pending { background: rgba(148,163,184,0.1); color: $text-muted; }
  &.completed { background: rgba(16,185,129,0.1); color: $accent-emerald; }
  &.wrong { background: rgba(239,68,68,0.1); color: $accent-red; }
}
.btn-delete {
  display: flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; background: transparent;
  border: none; border-radius: 6px; color: rgba(239,68,68,0.3);
  cursor: pointer; opacity: 0; transition: all 0.2s;
  .exercise-card:hover & { opacity: 1; }
  &:hover { color: $accent-red; background: rgba(239,68,68,0.08); }
}

.empty-state {
  text-align: center; padding: 60px 20px;
  .empty-icon-wrap { font-size: 3rem; margin-bottom: 12px; }
  .empty-title { font-size: 1.1rem; font-weight: 700; color: $text-primary; margin: 0 0 6px; }
  .empty-desc { font-size: 0.85rem; color: $text-muted; margin: 0; }
}

.exercise-list-enter-active { transition: all 0.3s ease; }
.exercise-list-leave-active { transition: all 0.25s ease; }
.exercise-list-enter-from { opacity: 0; transform: translateY(12px); }
.exercise-list-leave-to { opacity: 0; transform: translateX(20px); }

@media (max-width: 1024px) {
  .page-header { padding: 12px 20px; }
  .stats-row { grid-template-columns: repeat(2, 1fr); padding: 16px 20px 0; }
  .filter-bar { padding: 12px 20px; }
  .exercise-list { padding: 0 20px 40px; }
}
@media (max-width: 640px) {
  .page-header { padding: 10px 12px; gap: 10px; flex-wrap: wrap; }
  .back-btn span { display: none; }
  .generate-btn span { display: none; }
  .stats-row { gap: 8px; padding: 12px 12px 0; }
  .stat-card { padding: 10px 14px; }
  .stat-card .stat-num { font-size: 1.1rem; }
  .exercise-list { padding: 0 12px 30px; }
  .exercise-card { padding: 14px 16px; }
}
</style>
