<template>
  <div class="quiz-result">
    <div v-if="!data || !data.questions || data.questions.length === 0" class="qr-empty">
      暂无题目数据
    </div>
    <div v-else class="qr-list">
      <div v-for="(q, i) in data.questions" :key="i" class="qr-item">
        <div class="qr-header">
          <span class="qr-number">第 {{ i + 1 }} 题</span>
          <span v-if="q.type" class="qr-type">{{ q.type }}</span>
        </div>
        <p class="qr-question">{{ q.question || q.content || q.title }}</p>
        <div v-if="q.options && q.options.length" class="qr-options">
          <div
            v-for="(opt, oi) in q.options"
            :key="oi"
            class="qr-option"
            :class="{
              correct: q.correctAnswer !== undefined && oi === q.correctAnswer,
              selected: q.userAnswer !== undefined && oi === q.userAnswer
            }"
          >
            <span class="qr-opt-label">{{ String.fromCharCode(65 + oi) }}</span>
            <span class="qr-opt-text">{{ opt }}</span>
            <span v-if="q.correctAnswer !== undefined && oi === q.correctAnswer" class="qr-opt-mark correct">✓</span>
            <span v-if="q.userAnswer !== undefined && oi === q.userAnswer && oi !== q.correctAnswer" class="qr-opt-mark wrong">✗</span>
          </div>
        </div>
        <div v-if="q.answer" class="qr-answer">
          <span class="qr-answer-label">答案：</span>{{ q.answer }}
        </div>
        <div v-if="q.explanation" class="qr-explanation">
          <span class="qr-explanation-label">解析：</span>{{ q.explanation }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  data: { type: Object, default: null }
})
</script>

<style lang="scss" scoped>
.quiz-result {
  padding: 16px;
}

.qr-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.qr-item {
  padding: 16px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 10px;
}

.qr-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.qr-number {
  font-size: 12px;
  font-weight: 600;
  color: #00f5d4;
}

.qr-type {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba(100, 100, 180, 0.15);
  border-radius: 4px;
  color: #a0a0d0;
}

.qr-question {
  font-size: 14px;
  color: #d0d0e8;
  line-height: 1.6;
  margin: 0 0 12px;
}

.qr-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.qr-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(100, 100, 180, 0.05);
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 6px;
  font-size: 13px;
  color: #c0c0e0;
  transition: all 0.2s;

  &.correct {
    background: rgba(0, 245, 212, 0.1);
    border-color: rgba(0, 245, 212, 0.3);
  }

  &.selected {
    background: rgba(255, 107, 107, 0.1);
    border-color: rgba(255, 107, 107, 0.3);
  }
}

.qr-opt-label {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(100, 100, 180, 0.15);
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #a0a0d0;
  flex-shrink: 0;
}

.qr-opt-text {
  flex: 1;
}

.qr-opt-mark {
  font-size: 14px;
  font-weight: 700;

  &.correct { color: #00f5d4; }
  &.wrong { color: #ff6b6b; }
}

.qr-answer, .qr-explanation {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: #b0b0d0;
}

.qr-answer-label, .qr-explanation-label {
  font-weight: 600;
  color: #a0a0d0;
}

.qr-empty {
  text-align: center;
  padding: 24px;
  color: #606090;
  font-size: 13px;
}
</style>