<template>
  <div class="diagnosis-quiz">
    <div class="dq-header">
      <h2 class="dq-title">🔍 能力诊断</h2>
      <p class="dq-desc">AI 正在分析你的知识水平，请回答以下问题</p>
    </div>

    <div v-if="questions.length === 0 && !loadingTimeout" class="dq-loading">
      <div class="dq-spinner"></div>
      <p>正在生成诊断题目...</p>
    </div>

    <div v-else-if="loadingTimeout" class="dq-loading dq-loading--empty">
      <div class="dq-empty-icon">📭</div>
      <p>暂无诊断题目</p>
      <p class="dq-empty-hint">请检查网络连接或重新开始学习</p>
    </div>

    <div v-else class="dq-questions">
      <div
        v-for="(q, idx) in questions"
        :key="q.id"
        class="dq-question"
        :class="{ answered: answers[q.id] !== undefined }"
      >
        <div class="dq-q-header">
          <span class="dq-q-num">{{ idx + 1 }}</span>
          <span class="dq-q-type">{{ q.type === 'choice' ? '选择题' : '问答题' }}</span>
        </div>
        <p class="dq-q-content">{{ q.content }}</p>

        <!-- 选择题 -->
        <div v-if="q.type === 'choice'" class="dq-options">
          <button
            v-for="(opt, oi) in q.options"
            :key="oi"
            class="dq-option"
            :class="{
              selected: answers[q.id] === oi,
              correct: showResults && q.correctIndex === oi,
              wrong: showResults && answers[q.id] === oi && q.correctIndex !== oi
            }"
            :disabled="showResults"
            @click="selectAnswer(q.id, oi)"
          >
            <span class="dq-opt-letter">{{ ['A', 'B', 'C', 'D'][oi] }}</span>
            <span class="dq-opt-text">{{ opt }}</span>
          </button>
        </div>

        <!-- 问答题 -->
        <textarea
          v-else
          v-model="answers[q.id]"
          class="dq-textarea"
          placeholder="请输入你的回答..."
          rows="3"
          :disabled="showResults"
        ></textarea>

        <!-- 解析 -->
        <div v-if="showResults && q.explanation" class="dq-explanation">
          <span class="dq-exp-label">解析：</span>
          {{ q.explanation }}
        </div>
      </div>
    </div>

    <div v-if="questions.length > 0 && !showResults" class="dq-actions">
      <button
        class="dq-submit-btn"
        :disabled="Object.keys(answers).length < questions.length"
        @click="submitAll"
      >
        提交诊断
      </button>
    </div>

    <div v-if="showResults" class="dq-result">
      <p class="dq-result-text">诊断完成！正在分析你的能力水平...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  session: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['answer', 'complete'])

const questions = ref(props.session.questions || [])
const answers = ref({})
const showResults = ref(false)
const loadingTimeout = ref(false)

// 监听 session.questions 变化，SSE 推送题目后自动更新视图
watch(() => props.session.questions, (newQuestions) => {
  if (Array.isArray(newQuestions) && newQuestions.length > 0) {
    questions.value = newQuestions
    loadingTimeout.value = false
  }
}, { deep: true })

// 15 秒超时，防止无限加载
onMounted(() => {
  setTimeout(() => {
    if (questions.value.length === 0) {
      loadingTimeout.value = true
    }
  }, 15000)
})

const selectAnswer = (questionId, optionIndex) => {
  answers.value[questionId] = optionIndex
  emit('answer', questionId, optionIndex)
}

const submitAll = () => {
  showResults.value = true
  setTimeout(() => emit('complete'), 1000)
}
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.diagnosis-quiz {
  max-width: 700px;
  margin: 0 auto;
}

.dq-header {
  text-align: center;
  margin-bottom: $space-6;
}

.dq-title {
  font-size: $text-2xl;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 $space-2;
}

.dq-desc {
  color: $text-muted;
  margin: 0;
}

.dq-loading {
  text-align: center;
  padding: $space-12;
  color: $text-muted;
}

.dq-loading--empty {
  .dq-empty-icon {
    font-size: 48px;
    margin-bottom: $space-3;
  }
  p { margin: $space-1 0; }
  .dq-empty-hint {
    font-size: $text-sm;
    color: $text-muted;
    opacity: 0.7;
  }
}

.dq-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid $border-default;
  border-top-color: $accent-indigo;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto $space-3;
}

@keyframes spin { to { transform: rotate(360deg); } }

.dq-question {
  background: rgba($bg-surface, 0.5);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  padding: $space-5;
  margin-bottom: $space-4;
  transition: border-color $transition-fast;

  &.answered { border-color: rgba($accent-indigo, 0.3); }
}

.dq-q-header {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-bottom: $space-3;
}

.dq-q-num {
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

.dq-q-type {
  font-size: $text-xs;
  color: $text-muted;
}

.dq-q-content {
  font-size: $text-base;
  color: $text-primary;
  line-height: 1.6;
  margin: 0 0 $space-4;
}

.dq-options {
  display: flex;
  flex-direction: column;
  gap: $space-2;
}

.dq-option {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3 $space-4;
  background: rgba($bg-elevated, 0.5);
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  text-align: left;
  font-family: $font-sans;

  &:hover:not(:disabled) {
    border-color: rgba($accent-indigo, 0.3);
    background: rgba($accent-indigo, 0.05);
  }

  &.selected {
    border-color: $accent-indigo;
    background: rgba($accent-indigo, 0.1);
  }

  &.correct {
    border-color: $color-success;
    background: rgba($color-success, 0.08);
  }

  &.wrong {
    border-color: $color-danger;
    background: rgba($color-danger, 0.08);
  }
}

.dq-opt-letter {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba($text-muted, 0.1);
  color: $text-secondary;
  font-size: $text-xs;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .dq-option.selected & {
    background: $accent-indigo;
    color: white;
  }
}

.dq-opt-text {
  font-size: $text-sm;
  color: $text-primary;
}

.dq-textarea {
  width: 100%;
  padding: $space-3;
  background: rgba($bg-elevated, 0.5);
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  color: $text-primary;
  font-size: $text-sm;
  font-family: $font-sans;
  resize: vertical;
  outline: none;

  &:focus { border-color: $accent-indigo; }
  &::placeholder { color: $text-placeholder; }
}

.dq-explanation {
  margin-top: $space-3;
  padding: $space-3;
  background: rgba($accent-indigo, 0.05);
  border-radius: $radius-md;
  font-size: $text-sm;
  color: $text-secondary;
  line-height: 1.6;
}

.dq-exp-label {
  font-weight: 600;
  color: $accent-indigo;
}

.dq-actions {
  text-align: center;
  padding: $space-4 0;
}

.dq-submit-btn {
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

  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.dq-result {
  text-align: center;
  padding: $space-6;
}

.dq-result-text {
  color: $accent-indigo;
  font-size: $text-md;
  font-weight: 500;
}
</style>