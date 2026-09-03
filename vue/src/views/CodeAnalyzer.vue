<template>
  <div class="analyzer-page">
    <!-- 页面标题区 -->
    <header class="page-header">
      <div class="header-row">
        <div class="header-left">
          <h1 class="page-title">
            <span class="title-glyph">🔍</span>
            <span>代码解析</span>
            <span class="title-sub">智能代码审查与优化建议</span>
          </h1>
        </div>
        <div class="header-right">
          <button class="analyze-btn" :disabled="analyzing || !code.trim()" @click="runAnalyze">
            {{ analyzing ? '⏳ 分析中...' : '开始分析' }}
          </button>
        </div>
      </div>
    </header>

    <div class="analyzer-content">
      <!-- 加载失败重试 -->
      <div v-if="loadError" class="load-error">
        <span>⚠️ 代码分析失败，请检查网络后重试</span>
        <button class="retry-btn" @click="runAnalyze">🔄 重试</button>
      </div>

      <!-- 输入区 -->
      <section class="input-section glass-card">
        <div class="section-header">
          <span class="section-title">📄 待分析代码</span>
          <div class="language-select">
            <select v-model="language" class="language-dropdown">
              <option value="">自动检测</option>
              <option value="python">Python</option>
              <option value="java">Java</option>
              <option value="javascript">JavaScript</option>
              <option value="cpp">C++</option>
              <option value="rust">Rust</option>
              <option value="go">Go</option>
            </select>
          </div>
        </div>
        <textarea
          v-model="code"
          class="code-input"
          placeholder="粘贴你要分析的代码，支持 Python / Java / JavaScript / C++ / Rust / Go..."
          spellcheck="false"
        ></textarea>
        <div class="input-meta">
          <span>{{ code.length }} 字符</span>
          <span class="sample-link" @click="loadSample">📋 载入示例代码</span>
        </div>
      </section>

      <!-- 结果区 -->
      <section v-if="result" class="result-section">
        <!-- 概览 -->
        <div class="result-summary glass-card">
          <div class="summary-main">
            <span class="summary-label">总体评价</span>
            <p class="summary-text">{{ result.summary }}</p>
          </div>
          <div class="summary-chips">
            <div class="summary-chip">
              <span class="chip-label">复杂度</span>
              <span class="chip-value">{{ result.complexity }}</span>
            </div>
            <div class="summary-chip">
              <span class="chip-label">问题数</span>
              <span class="chip-value">{{ result.issues.length }}</span>
            </div>
            <div class="summary-chip">
              <span class="chip-label">建议数</span>
              <span class="chip-value">{{ result.suggestions.length }}</span>
            </div>
          </div>
        </div>

        <!-- 问题列表 -->
        <div v-if="result.issues.length > 0" class="issues-card glass-card">
          <h3 class="card-title">🚨 发现的问题（{{ result.issues.length }}）</h3>
          <div v-for="(issue, i) in result.issues" :key="i" class="issue-item" :class="issue.severity">
            <span class="issue-severity" :class="issue.severity">{{ severityText(issue.severity) }}</span>
            <span class="issue-type">{{ issue.type }}</span>
            <span v-if="issue.line" class="issue-line">L{{ issue.line }}</span>
            <p class="issue-message">{{ issue.message }}</p>
          </div>
        </div>

        <!-- 优化建议 -->
        <div v-if="result.suggestions.length > 0" class="suggestions-card glass-card">
          <h3 class="card-title">💡 优化建议</h3>
          <ul class="suggestion-list">
            <li v-for="(sug, i) in result.suggestions" :key="i" class="suggestion-item">{{ sug }}</li>
          </ul>
        </div>

        <!-- 优化代码 -->
        <div v-if="result.optimizedCode" class="optimized-card glass-card">
          <div class="card-header-row">
            <h3 class="card-title">✨ 优化后的代码</h3>
            <button class="copy-btn" @click="copyOptimizedCode">📋 复制</button>
          </div>
          <pre class="optimized-code">{{ result.optimizedCode }}</pre>
        </div>
      </section>

      <!-- 空态 -->
      <section v-if="!result && !analyzing && !loadError" class="empty-state glass-card">
        <div class="empty-icon">🔍</div>
        <h3 class="empty-title">分析你的代码质量</h3>
        <p class="empty-desc">粘贴代码后点击「开始分析」，AI 将检测性能、安全、可维护性问题并给出优化建议</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import { analyzeCode } from '@/api/chatApi'
import { ElMessage } from 'element-plus'

const router = useRouter()

const code = ref('')
const language = ref('')
const analyzing = ref(false)
const loadError = ref(false)
const result = ref(null)

const sampleCode = `def fibonacci(n):
    # 计算斐波那契数列第 n 项
    if n <= 1:
        return n
    else:
        return fibonacci(n - 1) + fibonacci(n - 2)

def process_data(data):
    result = []
    for i in range(len(data)):
        for j in range(len(data)):
            if data[i] == data[j] and i != j:
                result.append(data[i])
    return result

# 注意：这段代码存在性能问题（重复递归、O(n^2) 双重循环）`

const loadSample = () => {
  code.value = sampleCode
  language.value = 'python'
}

const severityText = (severity) => {
  const map = { critical: '严重', warning: '警告', info: '提示' }
  return map[severity] || severity || '提示'
}

const runAnalyze = async () => {
  if (!code.value.trim()) {
    ElMessage.warning('请先粘贴要分析的代码')
    return
  }
  analyzing.value = true
  loadError.value = false
  try {
    const res = await analyzeCode(code.value, language.value || null)
    result.value = res?.data || null
    if (!result.value) {
      ElMessage.warning('未能获取分析结果，请稍后重试')
    }
  } catch (e) {
    loadError.value = true
    result.value = null
  } finally {
    analyzing.value = false
  }
}

const copyOptimizedCode = async () => {
  try {
    await navigator.clipboard.writeText(result.value.optimizedCode)
    ElMessage.success('优化代码已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}
</script>



<style lang="scss" scoped>
@use '../styles/variables' as *;

.analyzer-page {
  min-height: calc(100vh - 68px);
  position: relative;
}

.page-header { @include page-header-base; }
.page-title { @include page-title-base; }
.title-sub { font-size: 0.82rem; font-weight: 400; color: $text-muted; margin-left: 4px; -webkit-text-fill-color: initial; }

.back-btn {
  @include page-header-btn-ghost;
  background: rgba($bg-surface, 0.6);
  color: $text-secondary;
  border: 1px solid rgba($accent-indigo, 0.15);
  
  &:hover {
    border-color: rgba($accent-indigo, 0.3);
    color: $accent-indigo-light;
    background: rgba($accent-indigo, 0.08);
    transform: translateY(-1px);
  }
}

.analyze-btn {
  @include page-header-btn-primary;
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.25);
  color: $accent-indigo;
  
  &:hover:not(:disabled) {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.4);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
}

.analyze-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.analyzer-content {
  max-width: 860px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.glass-card {
  background: rgba($bg-surface, 0.45);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-cyan, 0.12);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s ease;
  box-shadow: $shadow-sm;
  
  &:hover {
    border-color: rgba($accent-cyan, 0.25);
    box-shadow: $shadow-md, 0 0 20px rgba($accent-cyan, 0.06);
  }
}

/* 输入区 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba($accent-cyan, 0.08);
}

.section-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: $text-primary;
  display: flex;
  align-items: center;
  gap: 8px;
  
  &::before {
    content: '';
    width: 3px;
    height: 14px;
    background: linear-gradient(135deg, $accent-indigo, $accent-cyan);
    border-radius: 2px;
    box-shadow: 0 0 6px rgba($accent-indigo, 0.4);
  }
}

.language-dropdown {
  padding: 6px 12px;
  background: rgba($bg-surface, 0.4);
  border: 1px solid rgba($accent-cyan, 0.15);
  border-radius: 8px;
  color: $text-secondary;
  font-size: 0.8rem;
  outline: none;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    border-color: rgba($accent-cyan, 0.3);
    background: rgba($accent-cyan, 0.05);
  }
  
  &:focus {
    border-color: rgba($accent-cyan, 0.4);
    box-shadow: 0 0 0 2px rgba($accent-indigo, 0.1);
  }
}

.code-input {
  width: 100%;
  min-height: 260px;
  padding: 16px;
  background: rgba($bg-surface, 0.35);
  border: 1px solid rgba($accent-cyan, 0.12);
  border-radius: 12px;
  color: $text-primary;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 0.85rem;
  line-height: 1.7;
  resize: vertical;
  outline: none;
  transition: all 0.3s ease;
  
  &:focus {
    border-color: rgba($accent-cyan, 0.3);
    box-shadow: 0 0 0 3px rgba($accent-cyan, 0.1);
  }
  
  &::placeholder {
    color: $text-placeholder;
  }
}

.input-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  font-size: 0.75rem;
  color: $text-muted;
  padding-top: 10px;
  border-top: 1px solid rgba($accent-indigo, 0.06);
}

.sample-link {
  color: $text-primary;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 4px 8px;
  border-radius: 6px;
  
  &:hover {
    color: $accent-indigo-light;
    background: rgba($accent-indigo, 0.08);
    text-shadow: 0 0 10px rgba($accent-indigo, 0.5);
  }
}

/* 结果区 */
.result-summary {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
  padding: 24px;
  background: rgba($bg-surface, 0.45);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-cyan, 0.12);
  border-radius: 16px;
  transition: all 0.3s ease;
  box-shadow: $shadow-sm;
  
  &:hover {
    border-color: rgba($accent-cyan, 0.25);
    box-shadow: $shadow-md, 0 0 20px rgba($accent-cyan, 0.06);
  }
}

.summary-main {
  flex: 1;
  min-width: 260px;
}

.summary-label {
  font-size: 0.72rem;
  color: $text-muted;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-weight: 600;
}

.summary-text {
  font-size: 0.9rem;
  color: $text-primary;
  line-height: 1.7;
  margin: 6px 0 0;
  background: linear-gradient(135deg, $text-primary 0%, $accent-indigo-light 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.summary-chips {
  display: flex;
  gap: 12px;
}

.summary-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 18px;
  background: rgba($accent-indigo, 0.08);
  border: 1px solid rgba($accent-indigo, 0.15);
  border-radius: 10px;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba($accent-indigo, 0.15);
    border-color: rgba($accent-indigo, 0.25);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.15);
  }
}

.chip-label {
  font-size: 0.68rem;
  color: $text-muted;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.chip-value {
  font-size: 1rem;
  font-weight: 700;
  color: $accent-indigo;
  font-family: 'JetBrains Mono', monospace;
  background: linear-gradient(135deg, $accent-indigo 0%, $accent-cyan 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 8px rgba($accent-indigo, 0.3));
}

.card-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  
  &::before {
    content: '';
    width: 3px;
    height: 16px;
    background: linear-gradient(135deg, $accent-indigo, $accent-cyan);
    border-radius: 2px;
    box-shadow: 0 0 8px rgba($accent-indigo, 0.5);
  }
}

/* 问题列表 */
.issue-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  border-radius: 10px;
  background: rgba($accent-indigo, 0.05);
  border-left: 3px solid #94a3b8;
  flex-wrap: wrap;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba($accent-indigo, 0.1);
    transform: translateX(3px);
  }
}

.issue-item.critical {
  border-left-color: $color-danger;
}

.issue-item.warning {
  border-left-color: $color-warning;
}

.issue-item.info {
  border-left-color: $accent-indigo;
}

.issue-severity {
  font-size: 0.68rem;
  padding: 2px 8px;
  border-radius: 8px;
  font-weight: 600;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.issue-severity.critical {
  background: rgba($color-danger, 0.12);
  color: $color-danger;
}

.issue-severity.warning {
  background: rgba($color-warning, 0.12);
  color: $color-warning;
}

.issue-severity.info {
  background: rgba($accent-indigo, 0.12);
  color: $accent-indigo;
}

.issue-type {
  font-size: 0.72rem;
  color: $accent-indigo;
  background: rgba($accent-indigo, 0.1);
  padding: 2px 8px;
  border-radius: 8px;
  border: 1px solid rgba($accent-indigo, 0.15);
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba($accent-indigo, 0.18);
    border-color: rgba($accent-indigo, 0.3);
  }
}

.issue-line {
  font-size: 0.7rem;
  color: $text-muted;
  font-family: 'JetBrains Mono', monospace;
  background: rgba($bg-elevated, 0.5);
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid rgba($accent-indigo, 0.08);
}

.issue-message {
  flex: 1;
  min-width: 200px;
  font-size: 0.85rem;
  color: $text-secondary;
  margin: 0;
}

/* 建议 */
.suggestion-list {
  margin: 0;
  padding-left: 20px;
  list-style: none;
  
  li {
    position: relative;
    
    &::before {
      content: '•';
      color: $accent-indigo;
      font-weight: bold;
      position: absolute;
      left: -15px;
    }
  }
}

.suggestion-item {
  font-size: 0.85rem;
  color: $text-secondary;
  line-height: 1.7;
  margin-bottom: 6px;
  padding: 8px 12px;
  background: rgba($accent-indigo, 0.03);
  border-radius: 8px;
  border-left: 2px solid rgba($accent-indigo, 0.2);
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba($accent-indigo, 0.08);
    border-left-color: rgba($accent-indigo, 0.4);
    transform: translateX(3px);
  }
}

/* 优化代码 */
.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba($accent-indigo, 0.08);
}

.copy-btn {
  padding: 5px 12px;
  background: rgba($accent-indigo, 0.1);
  border: 1px solid rgba($accent-indigo, 0.2);
  border-radius: 8px;
  color: $accent-indigo;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba($accent-indigo, 0.2);
    border-color: rgba($accent-indigo, 0.35);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($accent-indigo, 0.2);
  }
}

.optimized-code {
  max-height: 320px;
  overflow: auto;
  padding: 16px;
  background: rgba($bg-base, 0.7);
  border: 1px solid rgba($accent-indigo, 0.1);
  border-radius: 10px;
  color: $color-success;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 0.82rem;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  transition: all 0.3s ease;
  
  &:hover {
    border-color: rgba($accent-indigo, 0.2);
    box-shadow: 0 0 15px rgba($accent-indigo, 0.05);
  }
  
  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  
  &::-webkit-scrollbar-thumb {
    background: rgba($accent-indigo, 0.2);
    border-radius: 3px;
    
    &:hover {
      background: rgba($accent-indigo, 0.35);
    }
  }
}

/* 空态 / 错误 */
.empty-state {
  text-align: center;
  padding: 60px 24px;
  background: rgba($bg-surface, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: 16px;
  transition: all 0.3s ease;
  
  &:hover {
    border-color: rgba($accent-indigo, 0.25);
    box-shadow: 0 0 20px rgba($accent-indigo, 0.06);
  }
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 12px;
  filter: drop-shadow(0 0 10px rgba($accent-indigo, 0.3));
}

.empty-title {
  font-size: 1.1rem;
  font-weight: 700;
  background: linear-gradient(135deg, $text-primary 0%, $accent-indigo-light 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 0.85rem;
  color: $text-muted;
  margin: 0;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.55;
}

.load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  background: rgba($color-danger, 0.08);
  border: 1px solid rgba($color-danger, 0.25);
  border-radius: 12px;
  color: $color-danger;
  font-size: 0.9rem;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  
  &:hover {
    border-color: rgba($color-danger, 0.4);
    box-shadow: 0 0 15px rgba($color-danger, 0.1);
  }
}

.retry-btn {
  padding: 6px 14px;
  background: rgba($color-danger, 0.12);
  border: 1px solid rgba($color-danger, 0.3);
  border-radius: 8px;
  color: $color-danger;
  font-size: 0.85rem;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba($color-danger, 0.2);
    border-color: rgba($color-danger, 0.45);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba($color-danger, 0.2);
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-wrap: wrap;
    gap: 10px;
  }
  .back-btn span {
    display: none;
  }
  .result-summary {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>