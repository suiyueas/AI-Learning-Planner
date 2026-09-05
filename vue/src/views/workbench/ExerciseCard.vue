<template>
  <div class="exercise-card">
    <div class="ec-header">
      <h2 class="ec-title">✏️ 习题练习</h2>
      <p class="ec-desc">针对你的薄弱点进行专项练习，巩固所学知识</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="exercises.length === 0" class="ec-loading">
      <div class="ec-spinner"></div>
      <p>正在生成习题...</p>
    </div>

    <template v-else>
      <!-- 进度指示器 -->
      <div class="ec-progress">
        <div class="ec-progress-info">
          <span class="ec-progress-label">答题进度</span>
          <span class="ec-progress-count">{{ answeredCount }} / {{ exercises.length }}</span>
        </div>
        <div class="ec-progress-track">
          <div
            class="ec-progress-fill"
            :style="{ width: `${(answeredCount / exercises.length) * 100}%` }"
          ></div>
        </div>
      </div>

      <!-- 习题列表 -->
      <div class="ec-list">
        <div
          v-for="(ex, idx) in exercises"
          :key="ex.id || idx"
          class="ec-item"
          :class="{
            'ec-item--answered': localAnswers[ex.id] !== undefined,
            'ec-item--correct': submitted && ex.correctIndex !== undefined && localAnswers[ex.id] === ex.correctIndex,
            'ec-item--wrong': submitted && ex.correctIndex !== undefined && localAnswers[ex.id] !== undefined && localAnswers[ex.id] !== ex.correctIndex,
            'ec-item--active': currentQuestion === idx
          }"
          @click="currentQuestion = idx"
        >
          <div class="ec-item-header">
            <span class="ec-item-num">{{ idx + 1 }}</span>
            <span class="ec-item-type">{{ ex.type === 'choice' ? '选择题' : '问答题' }}</span>
            <span v-if="localAnswers[ex.id] !== undefined && !submitted" class="ec-item-dot">●</span>
            <span v-if="submitted && ex.correctIndex !== undefined" class="ec-item-result">
              {{ localAnswers[ex.id] === ex.correctIndex ? '✓' : '✗' }}
            </span>
          </div>
          <p class="ec-item-content">{{ ex.content }}</p>

          <!-- 选择题选项 -->
          <div v-if="ex.type === 'choice'" class="ec-options">
            <button
              v-for="(opt, oi) in ex.options"
              :key="oi"
              class="ec-option"
              :class="{
                selected: localAnswers[ex.id] === oi,
                correct: submitted && ex.correctIndex === oi,
                wrong: submitted && localAnswers[ex.id] === oi && ex.correctIndex !== oi,
                disabled: submitted
              }"
              :disabled="submitted"
              @click.stop="selectAnswer(ex.id, oi)"
            >
              <span class="ec-opt-letter">{{ ['A', 'B', 'C', 'D'][oi] }}</span>
              <span class="ec-opt-text">{{ opt }}</span>
              <span v-if="submitted && ex.correctIndex === oi" class="ec-opt-check">✓</span>
            </button>
          </div>

          <!-- 问答题 -->
          <textarea
            v-else
            v-model="localAnswers[ex.id]"
            class="ec-textarea"
            placeholder="请输入答案..."
            rows="2"
            :disabled="submitted"
            @click.stop
          ></textarea>

          <!-- 提交后解析 -->
          <div v-if="submitted && ex.explanation" class="ec-explanation">
            <span class="ec-exp-label">解析</span>
            <p>{{ ex.explanation }}</p>
          </div>
        </div>
      </div>

      <!-- 提交按钮 -->
      <div v-if="!submitted" class="ec-actions">
        <button
          class="ec-submit-btn"
          :disabled="answeredCount < exercises.length"
          @click="handleSubmit"
        >
          提交习题（{{ answeredCount }}/{{ exercises.length }}）
        </button>
      </div>

      <!-- 提交结果 -->
      <div v-else class="ec-result">
        <div class="ec-result-header">
          <span class="ec-result-title">📊 答题结果</span>
        </div>
        <div class="ec-result-stats">
          <div class="ec-result-stat">
            <span class="ec-result-value ec-result-value--correct">{{ correctCount }}</span>
            <span class="ec-result-label">正确</span>
          </div>
          <div class="ec-result-stat">
            <span class="ec-result-value ec-result-value--wrong">{{ wrongCount }}</span>
            <span class="ec-result-label">错误</span>
          </div>
          <div class="ec-result-stat">
            <span class="ec-result-value">{{ correctRate }}%</span>
            <span class="ec-result-label">正确率</span>
          </div>
        </div>
        <button class="ec-retry-btn" @click="resetAll">
          重新练习
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  session: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['submit'])

const exercises = computed(() => props.session.exercises || [])
const localAnswers = ref({})
const submitted = ref(false)
const currentQuestion = ref(0)

const answeredCount = computed(() => {
  return Object.keys(localAnswers.value).length
})

const correctCount = computed(() => {
  let count = 0
  exercises.value.forEach(ex => {
    if (ex.correctIndex !== undefined && localAnswers.value[ex.id] === ex.correctIndex) {
      count++
    }
  })
  return count
})

const wrongCount = computed(() => {
  return answeredCount.value - correctCount.value
})

const correctRate = computed(() => {
  if (answeredCount.value === 0) return 0
  return Math.round((correctCount.value / answeredCount.value) * 100)
})

const selectAnswer = (questionId, optionIndex) => {
  if (submitted.value) return
  localAnswers.value[questionId] = optionIndex
}

const handleSubmit = () => {
  submitted.value = true
  emit('submit', { ...localAnswers.value })
}

const resetAll = () => {
  localAnswers.value = {}
  submitted.value = false
  currentQuestion.value = 0
}
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.exercise-card {
  max-width: 700px;
  margin: 0 auto;
  padding-bottom: $space-4;
}

// ===== 头部 =====
.ec-header {
  margin-bottom: $space-5;
}

.ec-title {
  font-size: $text-2xl;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 $space-1;
}

.ec-desc {
  color: $text-muted;
  margin: 0;
  font-size: $text-sm;
}

// ===== 加载 =====
.ec-loading {
  text-align: center;
  padding: $space-12;
  color: $text-muted;
}

.ec-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid $border-default;
  border-top-color: $accent-indigo;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto $space-3;
}

@keyframes spin { to { transform: rotate(360deg); } }

// ===== 进度条 =====
.ec-progress {
  margin-bottom: $space-5;
  padding: $space-3 $space-4;
  background: rgba($bg-surface, 0.4);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
}

.ec-progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-2;
}

.ec-progress-label {
  font-size: $text-xs;
  font-weight: 600;
  color: $text-secondary;
}

.ec-progress-count {
  font-size: $text-xs;
  color: $text-muted;
  font-family: $font-data;
}

.ec-progress-track {
  height: 6px;
  background: rgba($bg-elevated, 0.5);
  border-radius: 3px;
  overflow: hidden;
}

.ec-progress-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, $accent-indigo, $accent-cyan);
  transition: width 0.5s ease;
}

// ===== 习题项 =====
.ec-item {
  background: rgba($bg-surface, 0.4);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  padding: $space-5;
  margin-bottom: $space-4;
  transition: all $transition-fast;
  cursor: pointer;

  &--active {
    border-color: rgba($accent-indigo, 0.25);
  }

  &--answered {
    border-color: rgba($accent-indigo, 0.15);
  }

  &--correct {
    border-color: rgba($color-success, 0.3);
    background: rgba($color-success, 0.03);
  }

  &--wrong {
    border-color: rgba($color-danger, 0.3);
    background: rgba($color-danger, 0.03);
  }
}

.ec-item-header {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-bottom: $space-3;
}

.ec-item-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba($accent-indigo, 0.12);
  color: $accent-indigo;
  font-size: $text-xs;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ec-item-type {
  font-size: $text-xs;
  color: $text-muted;
}

.ec-item-dot {
  font-size: 8px;
  color: $accent-indigo;
  margin-left: auto;
}

.ec-item-result {
  margin-left: auto;
  font-size: $text-sm;
  font-weight: 700;

  .ec-item--correct & { color: $color-success; }
  .ec-item--wrong & { color: $color-danger; }
}

.ec-item-content {
  font-size: $text-base;
  color: $text-primary;
  line-height: 1.6;
  margin: 0 0 $space-4;
}

// ===== 选项 =====
.ec-options {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.ec-option {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-4;
  background: rgba($bg-elevated, 0.4);
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  text-align: left;
  font-family: $font-sans;
  color: $text-primary;
  font-size: $text-sm;
  position: relative;

  &:hover:not(.disabled) {
    border-color: rgba($accent-indigo, 0.3);
    background: rgba($accent-indigo, 0.05);
  }

  &.selected {
    border-color: $accent-indigo;
    background: rgba($accent-indigo, 0.1);
  }

  &.correct {
    border-color: rgba($color-success, 0.4);
    background: rgba($color-success, 0.08);
  }

  &.wrong {
    border-color: rgba($color-danger, 0.4);
    background: rgba($color-danger, 0.08);
  }

  &.disabled {
    cursor: default;
    opacity: 0.85;
  }
}

.ec-opt-letter {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba($text-muted, 0.1);
  font-size: $text-xs;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .ec-option.selected & { background: $accent-indigo; color: white; }
  .ec-option.correct & { background: $color-success; color: white; }
  .ec-option.wrong & { background: $color-danger; color: white; }
}

.ec-opt-text {
  flex: 1;
}

.ec-opt-check {
  font-size: $text-sm;
  color: $color-success;
  font-weight: 700;
}

// ===== 问答题 =====
.ec-textarea {
  width: 100%;
  padding: $space-3;
  background: rgba($bg-elevated, 0.4);
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  color: $text-primary;
  font-size: $text-sm;
  font-family: $font-sans;
  resize: vertical;
  outline: none;

  &:focus { border-color: $accent-indigo; }
  &::placeholder { color: $text-placeholder; }
  &:disabled { opacity: 0.6; cursor: not-allowed; }
}

// ===== 解析 =====
.ec-explanation {
  margin-top: $space-3;
  padding: $space-3;
  background: rgba($accent-indigo, 0.04);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: $radius-md;

  p {
    margin: $space-1 0 0;
    font-size: $text-sm;
    color: $text-secondary;
    line-height: 1.6;
  }
}

.ec-exp-label {
  font-size: 11px;
  font-weight: 600;
  color: $accent-indigo;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

// ===== 提交按钮 =====
.ec-actions {
  text-align: center;
  padding: $space-4 0;
}

.ec-submit-btn {
  padding: 10px 32px;
  background: rgba($accent-indigo, 0.15);
  border: 1px solid rgba($accent-indigo, 0.3);
  border-radius: $radius-md;
  color: $accent-indigo;
  font-size: $text-base;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;

  &:hover:not(:disabled) {
    background: rgba($accent-indigo, 0.25);
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

// ===== 结果 =====
.ec-result {
  margin-top: $space-4;
  padding: $space-5;
  background: rgba($bg-surface, 0.5);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  text-align: center;
}

.ec-result-header {
  margin-bottom: $space-4;
}

.ec-result-title {
  font-size: $text-lg;
  font-weight: 700;
  color: $text-primary;
}

.ec-result-stats {
  display: flex;
  gap: $space-4;
  margin-bottom: $space-5;
}

.ec-result-stat {
  flex: 1;
  text-align: center;
}

.ec-result-value {
  display: block;
  font-family: $font-data;
  font-size: $text-2xl;
  font-weight: 700;
  color: $accent-indigo;
  margin-bottom: $space-1;

  &--correct { color: $color-success; }
  &--wrong { color: $color-danger; }
}

.ec-result-label {
  font-size: $text-xs;
  color: $text-muted;
}

.ec-retry-btn {
  padding: 8px 24px;
  background: transparent;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-secondary;
  font-size: $text-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;

  &:hover {
    border-color: $border-medium;
    color: $text-primary;
  }
}
</style>