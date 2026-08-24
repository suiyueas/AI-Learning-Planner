<template>
  <div class="assessment-page">
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
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" /></svg><span>返回</span>
      </button>
      <h1 class="page-title">
        <span class="title-icon">🎯</span>
        <span class="title-text">能力测评</span>
      </h1>
      <button v-if="phase === 'result'" class="retry-btn" @click="startAssessment">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10" /><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10" /></svg>
        <span>重新测评</span>
      </button>
    </header>

    <!-- ===== 状态一：开始页（含科目选择） ===== -->
    <div v-if="phase === 'welcome'" class="content">
      <!-- AI 智能推荐区 -->
      <div v-if="aiRecommendation" class="ai-recommendation-card glass-card">
        <div class="ai-rec-header">
          <span class="ai-rec-icon">🎯</span>
          <span class="ai-rec-title">AI 智能推荐</span>
          <span class="ai-rec-badge">个性化</span>
        </div>
        <div class="ai-rec-content">
          <p class="ai-rec-message">{{ aiRecommendation.message }}</p>
          <div v-if="aiRecommendation.reasons?.length > 0" class="ai-rec-reasons">
            <span v-for="(reason, i) in aiRecommendation.reasons" :key="i" class="reason-tag">{{ reason }}</span>
          </div>
        </div>
        <button class="ai-rec-action-btn" @click="applyAIRecommendation">
          开始推荐测评 →
        </button>
      </div>

      <div class="welcome-card glass-card">
        <div class="welcome-icon">📋</div>
        <h2 class="welcome-title">多维度能力测评</h2>
        <p class="welcome-desc">选择科目后开始测评，系统将自动生成题目评估你的知识水平。</p>
        <div class="subject-input-area">
          <label class="selector-label">输入测评科目</label>
          <el-input
            v-model="subjectInput"
            placeholder="请输入您想测评的科目，如：机器学习、Python数据分析"
            clearable
            size="large"
            class="subject-input"
          />
          <div class="quick-subjects">
            <el-tag
              v-for="tag in quickSubjects"
              :key="tag"
              :class="{ active: subjectInput === tag }"
              class="quick-tag"
              @click="subjectInput = tag"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>

        <!-- 自适应难度推荐（基于历史测评真实数据） -->
        <div v-if="adaptiveConfig" class="adaptive-recommend">
          <div class="adaptive-recommend-header">
            <span class="adaptive-icon">🧠</span>
            <div class="adaptive-recommend-text">
              <span class="adaptive-title">自适应难度推荐</span>
              <span class="adaptive-desc">基于你 {{ adaptiveConfig.totalAttempts || 0 }} 次《{{ subjectInput }}》测评历史，推荐合适难度</span>
            </div>
            <span v-if="adaptiveLoading" class="adaptive-loading">分析中...</span>
          </div>
          <div class="adaptive-recommend-body">
            <div class="adaptive-item">
              <span class="adaptive-item-label">推荐难度</span>
              <span class="adaptive-item-value">{{ adaptiveDifficultyLabel }}</span>
            </div>
            <div class="adaptive-item">
              <span class="adaptive-item-label">历史正确率</span>
              <span class="adaptive-item-value">{{ adaptiveConfig.historicalAccuracy ?? '—' }}%</span>
            </div>
            <div class="adaptive-item">
              <span class="adaptive-item-label">推荐题数</span>
              <span class="adaptive-item-value">{{ adaptiveConfig.recommendedCount ?? 10 }} 题</span>
            </div>
            <label class="adaptive-switch">
              <input v-model="adaptiveEnabled" type="checkbox" />
              <span class="adaptive-switch-text">{{ adaptiveEnabled ? '已开启自适应' : '已关闭' }}</span>
            </label>
          </div>
        </div>

        <div class="difficulty-selector">
          <label class="selector-label">选择难度</label>
          <div class="difficulty-options">
            <label v-for="d in difficulties" :key="d.value" class="difficulty-option" :class="{ active: selectedDifficulty === d.value }">
              <input v-model="selectedDifficulty" type="radio" :value="d.value" />
              <span class="diff-icon">{{ d.icon }}</span>
              <span class="diff-label">{{ d.label }}</span>
              <span class="diff-desc">{{ d.desc }}</span>
            </label>
          </div>
        </div>
        <div class="welcome-meta">
          <div class="meta-chip"><span class="meta-icon">⏱️</span> 约 15 分钟</div>
          <div class="meta-chip"><span class="meta-icon">📝</span> 10 道题</div>
          <div class="meta-chip"><span class="meta-icon">📊</span> 多维度分析</div>
        </div>
        <div class="action-buttons">
          <button class="btn-start" :disabled="loading || !subjectInput.trim()" @click="startAssessment">
            <span v-if="loading">⏳ 生成题目中...</span>
            <template v-else>
              开始测评
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12" /><polyline points="12 5 19 12 12 5" /></svg>
            </template>
          </button>
          <button class="btn-history" @click="openHistory">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
            历史记录
          </button>
        </div>
      </div>
    </div>

    <!-- ===== 状态二：答题中 ===== -->
    <div v-if="phase === 'quiz'" class="content">
      <div class="quiz-progress-bar">
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: ((currentIndex + 1) / questions.length * 100) + '%' }"></div>
          <div v-for="(_, i) in questions" :key="i" class="progress-dot" :class="{ active: i <= currentIndex, current: i === currentIndex }" :style="{ left: ((i + 0.5) / questions.length * 100) + '%' }"></div>
        </div>
        <div class="progress-meta">
          <span class="progress-text">第 <strong>{{ currentIndex + 1 }}</strong> / {{ questions.length }} 题</span>
          <div class="quiz-tags">
            <span class="quiz-category">{{ subjectLabel }}</span>
            <span class="quiz-difficulty" :class="'diff-' + selectedDifficulty">{{ difficultyLabel }}</span>
          </div>
        </div>
      </div>

      <div class="quiz-card glass-card">
        <div class="question-number">Q{{ currentIndex + 1 }}</div>
        <h3 class="question-text">{{ currentQuestion?.questionText }}</h3>
        <div class="question-options">
          <label
v-for="(opt, i) in (currentQuestion?.options || [])" :key="i" class="option-item" :class="{
            selected: selectedOption === i
          }" @click="selectOption(i)"
>
            <span class="option-letter">{{ ['A','B','C','D'][i] }}</span>
            <span class="option-text">{{ opt }}</span>
            <span v-if="selectedOption === i" class="option-check">✓</span>
          </label>
        </div>
      </div>

      <div class="quiz-actions">
        <button class="btn-prev" :disabled="currentIndex === 0" @click="prevQuestion">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15 18 9 12 15 6" /></svg>
          上一题
        </button>
        <div class="action-right">
          <button v-if="currentIndex < questions.length - 1" class="btn-next" @click="nextQuestion">
            下一题
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6" /></svg>
          </button>
          <button v-else class="btn-submit-all" :disabled="!allAnswered" @click="submitAllAnswers">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12" /></svg>
            提交全部答案
          </button>
        </div>
      </div>

      <div v-if="!allAnswered && currentIndex === questions.length - 1" class="unanswered-tip">
        请完成所有题目后再提交
      </div>
    </div>

    <!-- ===== 状态三：结果页 ===== -->
    <div v-if="phase === 'result'" class="content">
      <div class="result-hero glass-card">
        <div class="result-score-section">
          <span class="result-label">综合得分</span>
          <div class="result-score-wrap">
            <span class="result-score" :style="{ color: scoreColor }">{{ resultData.totalScore }}</span>
            <span class="result-total">/ 100</span>
          </div>
          <span class="result-level" :style="{ background: levelBg, color: scoreColor, borderColor: scoreColor + '30' }">{{ resultData.level }}</span>
        </div>
        <div class="result-quick-stats">
          <div class="quick-stat">
            <span class="qs-value">{{ resultData.correctCount }}</span>
            <span class="qs-label">答对</span>
          </div>
          <div class="quick-stat">
            <span class="qs-value">{{ resultData.wrongCount }}</span>
            <span class="qs-label">答错</span>
          </div>
          <div class="quick-stat">
            <span class="qs-value">{{ resultData.accuracy }}%</span>
            <span class="qs-label">正确率</span>
          </div>
        </div>
      </div>

      <div class="result-card-list glass-card">
        <h3 class="section-title">📝 答题详情</h3>
        <div v-for="(item, i) in resultData.details" :key="i" class="result-question-item" :class="{ correct: item.correct, wrong: !item.correct }">
          <div class="rq-header">
            <span class="rq-number">Q{{ i + 1 }}</span>
            <span class="rq-badge" :class="item.correct ? 'badge-correct' : 'badge-wrong'">{{ item.correct ? '正确' : '错误' }}</span>
          </div>
          <p class="rq-text">{{ item.questionText }}</p>
          <div class="rq-options">
            <div
v-for="(opt, j) in item.options" :key="j" class="rq-option" :class="{
              'rq-correct': j === item.correctAnswer,
              'rq-wrong': j === item.userAnswer && j !== item.correctAnswer
            }"
>
              <span class="rq-opt-letter">{{ ['A','B','C','D'][j] }}</span>
              <span class="rq-opt-text">{{ opt }}</span>
              <span v-if="j === item.correctAnswer" class="rq-mark">✅</span>
              <span v-if="j === item.userAnswer && j !== item.correctAnswer" class="rq-mark">❌</span>
            </div>
          </div>
          <div class="rq-explanation">
            <span class="rq-explain-icon">💡</span>
            <span>{{ item.explanation }}</span>
          </div>
        </div>
      </div>

      <!-- AI 诊断分析入口 -->
      <div class="ai-diagnosis-card glass-card">
        <div class="diagnosis-header">
          <span class="diagnosis-icon">🧠</span>
          <span class="diagnosis-title">AI 诊断分析</span>
        </div>
        <div class="diagnosis-content">
          <p class="diagnosis-message">
            基于你的测评结果，诊断 Agent 将分析你的薄弱点并生成个性化的学习建议。
          </p>
          <div class="diagnosis-stats">
            <span class="diag-stat">📊 测评科目：{{ subjectLabel }}</span>
            <span class="diag-stat">📈 正确率：{{ resultData.accuracy }}%</span>
            <span class="diag-stat">📝 答对：{{ resultData.correctCount }} 题</span>
          </div>
        </div>
        <div class="diagnosis-actions">
          <button class="btn-diagnosis" @click="openDiagnosis">
            <span>🔍</span>
            <span>获取详细诊断报告</span>
          </button>
          <button class="btn-adjust-path" @click="adjustLearningPath">
            <span>📋</span>
            <span>根据结果调整学习路径</span>
          </button>
        </div>
      </div>

      <div class="result-actions">
        <button class="btn-secondary" @click="startAssessment">🔄 重新测评</button>
        <button class="btn-primary" @click="goBack">📋 返回功能列表</button>
      </div>
    </div>
  </div>

  <div class="history-dialog-container">
    <HistoryDialog v-model="historyVisible" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getQuestions, submitAnswers, getSubjects, getAdaptiveConfig, generateAdaptiveQuiz } from '@/api/assessmentApi'
import { ElMessage } from 'element-plus'
import HistoryDialog from '@/components/tools/HistoryDialog.vue'

const router = useRouter()

// ===== 科目数据（自定义输入） =====
const subjectInput = ref('')
const quickSubjects = [
  'Python', 'Java', 'C++', 'JavaScript', '数据结构与算法',
  '数据库', '网络基础', '机器学习', '前端开发', '系统设计'
]

// ===== 难度数据 =====
const difficulties = [
  { value: 'easy', label: '简单', icon: '🌱', desc: '适合初学者' },
  { value: 'medium', label: '中等', icon: '⚡', desc: '适合有一定基础' },
  { value: 'hard', label: '困难', icon: '🔥', desc: '挑战高阶能力' },
]
const selectedDifficulty = ref('medium')

const difficultyLabel = computed(() => {
  const d = difficulties.find(d => d.value === selectedDifficulty.value)
  return d ? d.label : '中等'
})

// ===== 自适应难度（基于历史测评真实数据） =====
const adaptiveEnabled = ref(true)
const adaptiveConfig = ref(null)
const adaptiveLoading = ref(false)
let adaptiveTimer = null

const adaptiveDifficultyLabel = computed(() => {
  const d = difficulties.find(d => d.value === adaptiveConfig.value?.difficulty)
  return d ? d.label : '中等'
})

// ===== AI 智能推荐（基于用户学习记录生成） =====
const aiRecommendation = computed(() => {
  if (!adaptiveConfig.value || !adaptiveEnabled.value) return null

  const config = adaptiveConfig.value
  const subject = subjectInput.value
  const historyCount = config.totalAttempts || 0
  const accuracy = config.historicalAccuracy

  if (historyCount === 0) {
    return {
      message: `检测到你是《${subject}》的新手，基于学习路径推荐从 ${adaptiveDifficultyLabel.value} 难度开始。`,
      reasons: ['新科目', '打好基础']
    }
  }

  if (accuracy < 50) {
    return {
      message: `你的《${subject}》正确率偏低（${accuracy}%），建议从基础题开始巩固，查漏补缺。`,
      reasons: ['正确率偏低', '需要巩固基础', `历史${historyCount}次测评`]
    }
  }

  if (accuracy >= 80 && historyCount >= 3) {
    return {
      message: `你在《${subject}》表现优秀（正确率${accuracy}%），建议挑战更高难度，突破自己的能力边界。`,
      reasons: ['正确率高', '可挑战高难度', `历史${historyCount}次测评`]
    }
  }

  return {
    message: `基于你的 ${historyCount} 次《${subject}》测评历史，推荐 ${adaptiveDifficultyLabel.value} 难度，继续保持学习节奏。`,
    reasons: [`历史${historyCount}次测评`, `正确率${accuracy}%`]
  }
})

const applyAIRecommendation = () => {
  if (adaptiveConfig.value?.difficulty) {
    selectedDifficulty.value = adaptiveConfig.value.difficulty
  }
}

// 科目变化时加载自适应推荐（防抖 400ms）
const loadAdaptiveConfig = async (subject) => {
  if (!subject || !subject.trim()) {
    adaptiveConfig.value = null
    return
  }
  adaptiveLoading.value = true
  try {
    const res = await getAdaptiveConfig(subject.trim())
    adaptiveConfig.value = res?.data || null
  } catch (e) {
    adaptiveConfig.value = null
  } finally {
    adaptiveLoading.value = false
  }
}

watch(subjectInput, (val) => {
  clearTimeout(adaptiveTimer)
  adaptiveTimer = setTimeout(() => loadAdaptiveConfig(val), 400)
})

// ===== 历史记录弹窗 =====
const historyVisible = ref(false)
function openHistory() {
  historyVisible.value = true
}

// ===== 状态管理 =====
const phase = ref('welcome') // 'welcome' | 'quiz' | 'result'
const currentIndex = ref(0)
const selectedOption = ref(null)
const userAnswers = ref({}) // questionId -> answer index
const loading = ref(false)
const resultData = ref(null)

// ===== 题目数据（从 API 获取） =====
const questions = ref([])

const currentQuestion = computed(() => questions.value[currentIndex.value])

const subjectLabel = computed(() => subjectInput.value || '未选择')

const allAnswered = computed(() => {
  return questions.value.length > 0 && questions.value.every(q => userAnswers.value[q.id] !== undefined)
})

// ===== 颜色计算 =====
const scoreColor = computed(() => {
  const s = resultData.value?.totalScore || 0
  return s >= 80 ? '#00f5d4' : s >= 60 ? '#f59e0b' : s >= 40 ? '#fb923c' : '#ef4444'
})
const levelBg = computed(() => {
  const s = resultData.value?.totalScore || 0
  return s >= 80 ? 'rgba(0,245,212,0.1)' : s >= 60 ? 'rgba(245,158,11,0.1)' : s >= 40 ? 'rgba(251,146,60,0.1)' : 'rgba(239,68,68,0.1)'
})

// ===== 测评流程 =====
async function startAssessment() {
  if (!subjectInput.value.trim()) {
    ElMessage.warning('请输入要测评的科目')
    return
  }
  localStorage.setItem('quizSubject', subjectInput.value)
  localStorage.setItem('quizDifficulty', selectedDifficulty.value)
  loading.value = true
  try {
    let list = null
    if (adaptiveEnabled.value) {
      // 自适应模式：后端基于历史测评计算推荐难度
      const res = await generateAdaptiveQuiz(subjectInput.value, true, null, 10)
      list = res?.data || res || []
      if (list && list.length > 0 && adaptiveConfig.value?.difficulty) {
        selectedDifficulty.value = adaptiveConfig.value.difficulty
      }
    } else {
      const res = await getQuestions(subjectInput.value, 10, selectedDifficulty.value)
      list = res?.data || res || []
    }
    if (!list || list.length === 0) {
      ElMessage.warning('未能获取到题目，请稍后重试')
      return
    }
    questions.value = list
    phase.value = 'quiz'
    currentIndex.value = 0
    selectedOption.value = null
    userAnswers.value = {}
    loadCurrentAnswer()
  } catch (e) {
    console.error('获取题目失败:', e)
    ElMessage.error('获取题目失败：' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function openDiagnosis() {
  router.push({
    path: '/agents',
    query: {
      agent: 'diagnosis',
      subject: subjectLabel.value,
      accuracy: resultData.value?.accuracy,
      from: 'assessment'
    }
  })
}

function adjustLearningPath() {
  router.push({
    path: '/learning-path',
    query: {
      from: 'assessment',
      subject: subjectLabel.value,
      accuracy: resultData.value?.accuracy
    }
  })
}

function selectOption(i) {
  if (phase.value !== 'quiz') return
  selectedOption.value = i
  if (currentQuestion.value) {
    userAnswers.value[currentQuestion.value.id] = i
  }
}

function loadCurrentAnswer() {
  if (currentQuestion.value) {
    const saved = userAnswers.value[currentQuestion.value.id]
    selectedOption.value = saved !== undefined ? saved : null
  }
}

function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
    loadCurrentAnswer()
  }
}

function prevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    loadCurrentAnswer()
  }
}

async function submitAllAnswers() {
  if (!allAnswered.value) {
    ElMessage.warning('请完成所有题目后再提交')
    return
  }
  try {
    const res = await submitAnswers(subjectInput.value, userAnswers.value, questions.value, selectedDifficulty.value)
    resultData.value = res?.data || res
    phase.value = 'result'
  } catch (e) {
    console.error('提交答案失败:', e)
    ElMessage.error('提交失败：' + (e.message || '未知错误'))
  }
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/modules')
}

onMounted(async () => {
  const savedSubject = localStorage.getItem('quizSubject')
  const savedDifficulty = localStorage.getItem('quizDifficulty')
  if (savedSubject) subjectInput.value = savedSubject
  if (savedDifficulty) selectedDifficulty.value = savedDifficulty
  try {
    const res = await getSubjects()
    if (res?.data && res.data.length > 0) {
      // 使用服务端科目列表更新快速选择
      const serverSubjects = res.data.map(s => s.label)
      if (serverSubjects.length > 0) {
        quickSubjects.value = [...new Set([...serverSubjects, ...quickSubjects])].slice(0, 10)
      }
    }
  } catch (e) { /* 忽略 */ }
  // 初始科目加载自适应推荐
  loadAdaptiveConfig(subjectInput.value)
})
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;
.assessment-page { min-height: calc(100vh - 68px); background: $bg-primary; position: relative; overflow-x: hidden; }
.bg-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-aurora { position: absolute; inset: 0;
  background: radial-gradient(ellipse at 70% 20%, rgba($accent-primary,0.06) 0%, transparent 50%), radial-gradient(ellipse at 30% 80%, rgba(123,97,255,0.05) 0%, transparent 50%), radial-gradient(ellipse at 50% 50%, rgba(0,85,255,0.04) 0%, transparent 50%);
  animation: auroraDrift 20s ease-in-out infinite;
}
@keyframes auroraDrift { 0%,100% { transform: scale(1) rotate(0deg); } 33% { transform: scale(1.08) rotate(0.8deg); } 66% { transform: scale(0.95) rotate(-0.6deg); } }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba($accent-primary,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(123,97,255,0.03) 1px, transparent 1px); background-size: 40px 40px; animation: gridPulse 8s ease-in-out infinite alternate; }
@keyframes gridPulse { 0% { opacity: 0.3; } 100% { opacity: 0.6; } }

.glass-card { background: rgba($bg-primary,0.5); backdrop-filter: blur(12px); border: 1px solid rgba($accent-secondary,0.08); border-radius: 16px; }

.page-header { position: sticky; top: 0; z-index: 10; display: flex; align-items: center; gap: 16px; padding: 16px 32px; background: rgba($bg-primary,0.85); backdrop-filter: blur(20px); border-bottom: 1px solid rgba($accent-secondary,0.08); }
.back-btn { display: flex; align-items: center; gap: 6px; padding: 8px 14px; background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.1); border-radius: 8px; color: $text-secondary; font-size: 0.82rem; cursor: pointer; transition: all 0.25s ease; &:hover { border-color: rgba($accent-primary,0.2); color: $accent-primary; } }
.page-title { flex: 1; display: flex; align-items: center; gap: 10px; }
.title-icon { font-size: 1.3rem; }
.title-text { font-size: 1.05rem; font-weight: 700; background: linear-gradient(135deg, $accent-primary, $accent-purple); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.retry-btn { display: flex; align-items: center; gap: 6px; padding: 9px 18px; background: linear-gradient(135deg, rgba($accent-primary,0.12), rgba(123,97,255,0.08)); border: 1px solid rgba($accent-primary,0.15); border-radius: 10px; color: $accent-primary; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.25s ease; &:hover { transform: translateY(-1px); box-shadow: 0 0 20px rgba($accent-primary,0.12); } }

.content { max-width: 800px; margin: 0 auto; padding: 24px 32px 60px; position: relative; z-index: 1; }

/* ===== AI 智能推荐卡片 ===== */
.ai-recommendation-card {
  margin-bottom: 20px;
  padding: 20px 24px;
  border: 1px solid rgba($accent-primary, 0.15);
  background: rgba($accent-primary, 0.04);
}
.ai-rec-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.ai-rec-icon { font-size: 1.2rem; }
.ai-rec-title { font-size: 0.95rem; font-weight: 700; color: $text-primary; }
.ai-rec-badge {
  margin-left: auto;
  padding: 3px 10px;
  background: rgba($accent-primary, 0.15);
  border-radius: 12px;
  font-size: 0.72rem;
  color: $accent-primary;
}
.ai-rec-content { margin-bottom: 14px; }
.ai-rec-message {
  font-size: 0.88rem;
  color: $text-secondary;
  line-height: 1.6;
  margin: 0 0 10px;
}
.ai-rec-reasons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.reason-tag {
  padding: 4px 10px;
  background: rgba($accent-secondary, 0.08);
  border-radius: 6px;
  font-size: 0.72rem;
  color: $text-muted;
}
.ai-rec-action-btn {
  width: 100%;
  padding: 10px 0;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 10px;
  color: $accent-primary;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba($accent-primary, 0.15);
  }
}

/* ===== 开始页 ===== */
.welcome-card { text-align: center; padding: 50px 40px; margin-top: 40px; border-radius: 20px; }

/* ===== 科目输入区域 ===== */
.subject-input-area { margin-bottom: 20px; text-align: left; }
.subject-input { width: 100%; max-width: 480px; margin: 0 auto 16px; display: block; }
:deep(.subject-input .el-input__wrapper) { background: rgba($accent-secondary,0.06); border: 1.5px solid rgba($accent-secondary,0.12); border-radius: 12px; box-shadow: none; padding: 12px 18px; }
:deep(.subject-input .el-input__inner) { color: $text-primary; font-size: 0.95rem; height: 32px; line-height: 32px; }
:deep(.subject-input .el-input__inner::placeholder) { color: rgba(192,192,224,0.4); }
:deep(.subject-input.is-focus .el-input__wrapper) { border-color: $accent-primary; box-shadow: 0 0 16px rgba($accent-primary,0.1); }

.quick-subjects { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 12px; }
.quick-tag { cursor: pointer; padding: 6px 14px; border-radius: 16px; font-size: 0.78rem; background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.1); color: $text-secondary; transition: all 0.2s; &:hover { border-color: rgba($accent-primary,0.2); color: $text-primary; } &.active { border-color: $accent-primary; background: rgba($accent-primary,0.08); color: $accent-primary; } }

/* ===== 操作按钮区域 ===== */
.action-buttons { display: flex; gap: 12px; justify-content: center; align-items: center; flex-wrap: wrap; }
.btn-history { display: inline-flex; align-items: center; gap: 6px; padding: 12px 24px; background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.1); border-radius: 12px; color: $text-secondary; font-size: 0.9rem; font-weight: 600; cursor: pointer; transition: all 0.25s ease; &:hover { border-color: rgba(123,97,255,0.3); color: $text-primary; background: rgba(123,97,255,0.06); } }

/* ===== 科目选择器（保留兼容） ===== */
.subject-selector { margin-bottom: 20px; }
.selector-label { display: block; font-size: 0.85rem; color: $text-secondary; margin-bottom: 10px; font-weight: 600; text-align: left; }
.subject-select { width: 280px; }
:deep(.subject-select .el-input__wrapper) { background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.1); border-radius: 10px; box-shadow: none; }
:deep(.subject-select .el-input__inner) { color: $text-primary; font-size: 0.9rem; }
:deep(.subject-select .el-select__caret) { color: $text-secondary; }

/* ===== 难度选择器 ===== */
.difficulty-selector { margin-bottom: 20px; }
.difficulty-selector .selector-label { margin-bottom: 12px; }
.difficulty-options { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }
.difficulty-option { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 14px 20px; background: rgba($accent-secondary,0.03); border: 1.5px solid rgba($accent-secondary,0.08); border-radius: 12px; cursor: pointer; transition: all 0.25s ease; min-width: 110px; position: relative; &:hover { border-color: rgba($accent-secondary,0.2); background: rgba($accent-secondary,0.06); } &.active { border-color: $accent-primary; background: rgba($accent-primary,0.06); .diff-label { color: $accent-primary; } } input { display: none; } }
.diff-icon { font-size: 1.4rem; }
.diff-label { font-size: 0.9rem; font-weight: 600; color: $text-primary; }
.diff-desc { font-size: 0.72rem; color: $text-muted; }

/* ===== 自适应难度推荐 ===== */
.adaptive-recommend {
  margin-bottom: 20px;
  padding: 16px 18px;
  background: rgba($accent-primary, 0.04);
  border: 1px solid rgba($accent-primary, 0.18);
  border-radius: 12px;
  text-align: left;
}

.adaptive-recommend-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.adaptive-icon {
  font-size: 1.2rem;
}

.adaptive-recommend-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.adaptive-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: $accent-primary;
}

.adaptive-desc {
  font-size: 0.72rem;
  color: $text-muted;
}

.adaptive-loading {
  font-size: 0.72rem;
  color: $accent-primary;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.adaptive-recommend-body {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.adaptive-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.adaptive-item-label {
  font-size: 0.68rem;
  color: $text-muted;
}

.adaptive-item-value {
  font-size: 0.95rem;
  font-weight: 700;
  color: $text-primary;
  font-family: 'JetBrains Mono', monospace;
}

.adaptive-switch {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.adaptive-switch input {
  width: 16px;
  height: 16px;
  accent-color: $accent-primary;
  cursor: pointer;
}

.adaptive-switch-text {
  font-size: 0.78rem;
  color: $text-secondary;
}

/* ===== 结果页答题详情 ===== */
.result-card-list { padding: 24px; margin-bottom: 20px; }
.result-question-item { padding: 16px; margin-bottom: 12px; border-radius: 12px; border: 1px solid rgba($accent-secondary,0.08); background: rgba($accent-secondary,0.02); &.correct { border-color: rgba(16,185,129,0.15); background: rgba(16,185,129,0.02); } &.wrong { border-color: rgba(239,68,68,0.15); background: rgba(239,68,68,0.02); } }
.rq-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.rq-number { font-size: 0.75rem; font-weight: 700; color: $text-muted; font-family: 'JetBrains Mono', monospace; }
.rq-badge { padding: 2px 10px; border-radius: 10px; font-size: 0.7rem; font-weight: 600; &.badge-correct { background: rgba(16,185,129,0.1); color: $accent-emerald; } &.badge-wrong { background: rgba(239,68,68,0.1); color: $accent-red; } }
.rq-text { font-size: 0.9rem; color: $text-primary; margin: 0 0 12px; line-height: 1.5; }
.rq-options { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.rq-option { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 8px; font-size: 0.82rem; color: $text-secondary; background: rgba($accent-secondary,0.03); &.rq-correct { background: rgba(16,185,129,0.06); color: $accent-emerald; } &.rq-wrong { background: rgba(239,68,68,0.06); color: $accent-red; } }
.rq-opt-letter { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; border-radius: 6px; background: rgba($accent-secondary,0.06); font-size: 0.72rem; font-weight: 700; flex-shrink: 0; }
.rq-opt-text { flex: 1; }
.rq-mark { font-size: 0.85rem; }
.rq-explanation { display: flex; gap: 8px; padding: 10px 12px; background: rgba(123,97,255,0.04); border: 1px solid rgba(123,97,255,0.08); border-radius: 8px; font-size: 0.78rem; color: $text-secondary; line-height: 1.5; }
.rq-explain-icon { flex-shrink: 0; }
.welcome-icon { font-size: 3rem; margin-bottom: 16px; }
.welcome-title { font-size: 1.5rem; font-weight: 800; color: $text-primary; margin: 0 0 12px; }
.welcome-desc { font-size: 0.9rem; color: $text-secondary; line-height: 1.7; margin: 0 auto 28px; max-width: 500px; }
.welcome-meta { display: flex; gap: 16px; justify-content: center; margin-bottom: 32px; flex-wrap: wrap; }
.meta-chip { display: flex; align-items: center; gap: 6px; padding: 8px 18px; background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.08); border-radius: 20px; font-size: 0.82rem; color: $text-secondary; }
.meta-icon { font-size: 1rem; }
.btn-start { display: inline-flex; align-items: center; gap: 8px; padding: 12px 32px; background: linear-gradient(135deg, $accent-primary, $accent-purple); border: none; border-radius: 12px; color: #fff; font-size: 1rem; font-weight: 700; cursor: pointer; transition: all 0.3s ease; &:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba($accent-primary,0.25); } &:active { transform: translateY(0); } }

/* ===== 答题页 ===== */
.quiz-progress-bar { margin-bottom: 20px; }
.progress-track { position: relative; height: 6px; background: rgba($accent-secondary,0.1); border-radius: 3px; overflow: visible; margin-bottom: 10px; }
.progress-fill { height: 100%; background: linear-gradient(90deg, $accent-primary, $accent-purple); border-radius: 3px; transition: width 0.4s ease; }
.progress-dot { position: absolute; top: 50%; transform: translate(-50%, -50%); width: 12px; height: 12px; border-radius: 50%; background: rgba($accent-secondary,0.15); z-index: 1; transition: all 0.3s ease; &.active { background: $accent-primary; box-shadow: 0 0 8px rgba($accent-primary,0.4); } &.current { width: 16px; height: 16px; box-shadow: 0 0 12px rgba($accent-primary,0.5); } }
.progress-meta { display: flex; align-items: center; justify-content: space-between; }
.progress-text { font-size: 0.82rem; color: $text-secondary; strong { color: $accent-primary; font-family: 'JetBrains Mono', monospace; } }
.quiz-tags { display: flex; gap: 8px; align-items: center; }
.quiz-category { padding: 3px 12px; background: rgba($accent-primary,0.08); border: 1px solid rgba($accent-primary,0.15); border-radius: 12px; font-size: 0.75rem; color: $accent-primary; font-weight: 600; }
.quiz-difficulty { padding: 3px 12px; border-radius: 12px; font-size: 0.75rem; font-weight: 600; &.diff-easy { background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.2); color: $accent-emerald; } &.diff-medium { background: rgba(59,130,246,0.1); border: 1px solid rgba(59,130,246,0.2); color: $accent-blue; } &.diff-hard { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.2); color: $accent-red; } }

.quiz-card { padding: 28px 24px; margin-bottom: 16px; position: relative; }
.question-number { position: absolute; top: 16px; right: 20px; font-size: 0.7rem; font-family: 'JetBrains Mono', monospace; color: rgba($accent-secondary,0.3); font-weight: 700; letter-spacing: 1px; }
.question-text { font-size: 1.1rem; font-weight: 700; color: $text-primary; margin: 0 0 24px; line-height: 1.6; padding-right: 50px; }
.question-options { display: flex; flex-direction: column; gap: 10px; }
.option-item { display: flex; align-items: center; gap: 14px; padding: 16px 18px; background: rgba($accent-secondary,0.03); border: 1.5px solid rgba($accent-secondary,0.08); border-radius: 12px; cursor: pointer; transition: all 0.2s ease; position: relative; &:hover { border-color: rgba($accent-primary,0.15); background: rgba($accent-primary,0.02); } &.selected { border-color: $accent-primary; background: rgba($accent-primary,0.06); .option-letter { background: $accent-primary; color: $bg-primary; } } }
.option-letter { width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 8px; background: rgba($accent-secondary,0.08); font-size: 0.82rem; font-weight: 700; color: $text-secondary; flex-shrink: 0; transition: all 0.2s; }
.option-text { flex: 1; font-size: 0.9rem; color: $text-primary; line-height: 1.4; }
.option-check { font-size: 1rem; position: absolute; right: 16px; color: $accent-primary; }

.unanswered-tip { text-align: center; padding: 12px; margin-top: 12px; font-size: 0.82rem; color: $accent-amber; background: rgba(245,158,11,0.08); border: 1px solid rgba(245,158,11,0.15); border-radius: 10px; }

.quiz-actions { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.action-right { display: flex; gap: 8px; margin-left: auto; }
.btn-prev, .btn-next, .btn-submit-all { display: inline-flex; align-items: center; gap: 6px; padding: 10px 20px; border-radius: 10px; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; }
.btn-prev { background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.1); color: $text-secondary; &:hover:not(:disabled) { background: rgba($accent-secondary,0.1); } &:disabled { opacity: 0.3; cursor: not-allowed; } }
.btn-submit-all { background: linear-gradient(135deg, $accent-emerald, $accent-primary); color: #fff; &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(16,185,129,0.25); } &:disabled { opacity: 0.4; cursor: not-allowed; } }
.btn-next { background: linear-gradient(135deg, $accent-primary, $accent-purple); color: #fff; &:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba($accent-primary,0.2); } }

/* ===== 结果页 ===== */
.result-hero { display: flex; align-items: center; gap: 24px; padding: 28px 32px; margin-bottom: 20px; }
.result-score-section { text-align: center; flex-shrink: 0; }
.result-label { font-size: 0.78rem; color: $text-muted; display: block; margin-bottom: 4px; }
.result-score-wrap { display: flex; align-items: baseline; justify-content: center; gap: 2px; }
.result-score { font-size: 3rem; font-weight: 900; font-family: 'JetBrains Mono', monospace; text-shadow: 0 0 20px currentColor; line-height: 1; }
.result-total { font-size: 1.1rem; color: $text-muted; }
.result-level { display: inline-block; margin-top: 8px; padding: 4px 16px; border-radius: 16px; font-size: 0.85rem; font-weight: 600; }
.result-quick-stats { flex: 1; display: flex; justify-content: space-around; }
.quick-stat { text-align: center; }
.qs-value { display: block; font-size: 1.5rem; font-weight: 800; font-family: 'JetBrains Mono', monospace; color: $text-primary; }
.qs-label { font-size: 0.72rem; color: $text-muted; }

.result-radar { padding: 24px; margin-bottom: 20px; }
.radar-container { height: 300px; width: 100%; }
.radar-chart { width: 100%; height: 100%; }

.btn-start:disabled { opacity: 0.5; cursor: not-allowed; transform: none; box-shadow: none; }

.section-title { font-size: 0.95rem; font-weight: 700; color: $text-primary; margin: 0 0 16px; }

.result-details { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.result-strengths, .result-weaknesses { padding: 20px; }
.strength-item, .weakness-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid rgba($accent-secondary,0.04); &:last-child { border: none; } }
.strength-icon, .weakness-icon { font-size: 0.85rem; }
.strength-name, .weakness-name { flex: 1; font-size: 0.85rem; color: $text-secondary; }
.tag-score { font-size: 0.78rem; font-weight: 700; font-family: 'JetBrains Mono', monospace; color: $accent-primary; &.low { color: $accent-red; } }
.empty-tip { font-size: 0.82rem; color: $text-muted; text-align: center; padding: 16px 0; }

/* ===== AI 诊断分析卡片 ===== */
.ai-diagnosis-card {
  margin-bottom: 20px;
  padding: 20px 24px;
  border: 1px solid rgba($accent-purple, 0.15);
  background: rgba($accent-purple, 0.04);
}
.diagnosis-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.diagnosis-icon { font-size: 1.2rem; }
.diagnosis-title { font-size: 0.95rem; font-weight: 700; color: $text-primary; }
.diagnosis-content { margin-bottom: 14px; }
.diagnosis-message {
  font-size: 0.85rem;
  color: $text-secondary;
  line-height: 1.6;
  margin: 0 0 10px;
}
.diagnosis-stats {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.diag-stat {
  font-size: 0.78rem;
  color: $text-muted;
}
.diagnosis-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.btn-diagnosis, .btn-adjust-path {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-diagnosis {
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba($accent-primary, 0.2);
  color: $accent-primary;
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-primary, 0.15);
  }
}
.btn-adjust-path {
  background: rgba($accent-secondary, 0.06);
  border: 1px solid rgba($accent-secondary, 0.12);
  color: $text-secondary;
  &:hover {
    border-color: rgba($accent-purple, 0.3);
    color: $accent-purple;
  }
}

.result-suggestions { padding: 20px; margin-bottom: 20px; }
.suggestion-item { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px solid rgba($accent-secondary,0.04); &:last-child { border: none; } }
.sug-icon { font-size: 1rem; flex-shrink: 0; margin-top: 1px; }
.suggestion-item p { margin: 0; font-size: 0.82rem; color: $text-secondary; line-height: 1.6; }

.result-actions { display: flex; gap: 12px; justify-content: center; margin-bottom: 24px; }
.btn-secondary, .btn-primary { padding: 10px 24px; border-radius: 10px; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; }
.btn-secondary { background: rgba($accent-secondary,0.06); border: 1px solid rgba($accent-secondary,0.1); color: $text-secondary; &:hover { border-color: rgba($accent-primary,0.2); color: $text-primary; } }
.btn-primary { background: linear-gradient(135deg, $accent-primary, $accent-purple); color: #fff; &:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba($accent-primary,0.2); } }

.result-history { padding: 20px; margin-bottom: 20px; }
.history-chart { display: flex; align-items: flex-end; gap: 12px; height: 120px; padding: 12px 0; }
.history-bar-group { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px; height: 100%; }
.history-bar-wrap { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.history-bar { width: 60%; border-radius: 6px 6px 0 0; background: linear-gradient(180deg, $accent-primary, rgba($accent-primary,0.25)); position: relative; min-height: 4px; transition: height 0.4s ease; &.latest { background: linear-gradient(180deg, $accent-purple, rgba(123,97,255,0.25)); } }
.history-bar-value { position: absolute; top: -18px; left: 50%; transform: translateX(-50%); font-size: 0.65rem; color: $text-secondary; font-family: 'JetBrains Mono', monospace; }
.history-label { font-size: 0.65rem; color: $text-muted; }

@media (max-width: 1024px) { .page-header { padding: 12px 20px; } .content { padding: 20px; } .result-details { grid-template-columns: 1fr; } }
@media (max-width: 640px) {
  .page-header { padding: 10px 12px; gap: 10px; flex-wrap: wrap; }
  .back-btn span { display: none; }
  .content { padding: 12px; }
  .welcome-card { padding: 30px 20px; margin-top: 20px; }
  .welcome-title { font-size: 1.2rem; }
  .welcome-meta { gap: 10px; }
  .meta-chip { padding: 6px 14px; font-size: 0.78rem; }
  .quiz-card { padding: 20px 16px; }
  .question-text { font-size: 1rem; padding-right: 30px; }
  .question-number { font-size: 0.6rem; }
  .option-item { padding: 12px 14px; }
  .option-text { font-size: 0.82rem; }
  .result-hero { flex-direction: column; padding: 20px; gap: 16px; }
  .result-score { font-size: 2.2rem; }
  .radar-container { height: 240px; }
  .result-actions { flex-direction: column; }
}

.history-dialog-container { display: contents; }
</style>