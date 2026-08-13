<template>
  <div class="analyzer-page">
    <!-- 顶部导航 -->
    <header class="page-header">
      <button class="back-btn" @click="goBack">
        <ArrowLeft :size="18" />
        <span>返回</span>
      </button>
      <h1 class="page-title">
        <span class="title-icon">🔍</span>
        <span class="title-text">代码解析</span>
      </h1>
      <button class="analyze-btn" :disabled="analyzing || !code.trim()" @click="runAnalyze">
        <span v-if="analyzing">⏳ 分析中...</span>
        <template v-else>开始分析</template>
      </button>
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

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/modules')
}
</script>

<style scoped>
.analyzer-page {
  min-height: calc(100vh - 68px);
  background: #0a0a1a;
  position: relative;
}

.page-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 32px;
  background: rgba(10, 10, 26, 0.85);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 8px;
  color: #c0c0e0;
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.back-btn:hover {
  border-color: rgba(0, 245, 212, 0.2);
  color: #00f5d4;
}

.page-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  font-size: 1.3rem;
}

.title-text {
  font-size: 1.05rem;
  font-weight: 700;
  background: linear-gradient(135deg, #00f5d4, #7b61ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.analyze-btn {
  padding: 9px 20px;
  background: linear-gradient(135deg, rgba(0, 245, 212, 0.15), rgba(123, 97, 255, 0.1));
  border: 1px solid rgba(0, 245, 212, 0.2);
  border-radius: 10px;
  color: #00f5d4;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}

.analyze-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 0 20px rgba(0, 245, 212, 0.12);
}

.analyze-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.analyzer-content {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 32px 60px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.glass-card {
  background: rgba(17, 17, 39, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 16px;
  padding: 24px;
}

/* 输入区 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #e8e8ff;
}

.language-dropdown {
  padding: 6px 12px;
  background: rgba(10, 10, 26, 0.6);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 8px;
  color: #c0c0e0;
  font-size: 0.8rem;
  outline: none;
  cursor: pointer;
}

.code-input {
  width: 100%;
  min-height: 260px;
  padding: 16px;
  background: rgba(10, 10, 26, 0.7);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 12px;
  color: #e8e8ff;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 0.85rem;
  line-height: 1.7;
  resize: vertical;
  outline: none;
  transition: border-color 0.25s ease;
}

.code-input:focus {
  border-color: rgba(0, 245, 212, 0.3);
}

.input-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  font-size: 0.75rem;
  color: #8080a8;
}

.sample-link {
  color: #3a86ff;
  cursor: pointer;
  transition: color 0.2s;
}

.sample-link:hover {
  color: #00f5d4;
}

/* 结果区 */
.result-summary {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.summary-main {
  flex: 1;
  min-width: 260px;
}

.summary-label {
  font-size: 0.72rem;
  color: #8080a8;
}

.summary-text {
  font-size: 0.9rem;
  color: #e8e8ff;
  line-height: 1.7;
  margin: 6px 0 0;
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
  background: rgba(100, 100, 180, 0.05);
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 10px;
}

.chip-label {
  font-size: 0.68rem;
  color: #8080a8;
}

.chip-value {
  font-size: 1rem;
  font-weight: 700;
  color: #00f5d4;
  font-family: 'JetBrains Mono', monospace;
}

.card-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #e8e8ff;
  margin: 0 0 16px;
}

/* 问题列表 */
.issue-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  border-radius: 10px;
  background: rgba(100, 100, 180, 0.03);
  border-left: 3px solid #94a3b8;
  flex-wrap: wrap;
}

.issue-item.critical {
  border-left-color: #ef4444;
}

.issue-item.warning {
  border-left-color: #f59e0b;
}

.issue-item.info {
  border-left-color: #3a86ff;
}

.issue-severity {
  font-size: 0.68rem;
  padding: 2px 8px;
  border-radius: 8px;
  font-weight: 600;
  flex-shrink: 0;
}

.issue-severity.critical {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}

.issue-severity.warning {
  background: rgba(245, 158, 11, 0.12);
  color: #f59e0b;
}

.issue-severity.info {
  background: rgba(58, 134, 255, 0.12);
  color: #3a86ff;
}

.issue-type {
  font-size: 0.72rem;
  color: #a78bfa;
  background: rgba(123, 97, 255, 0.08);
  padding: 2px 8px;
  border-radius: 8px;
}

.issue-line {
  font-size: 0.7rem;
  color: #9090b8;
  font-family: 'JetBrains Mono', monospace;
}

.issue-message {
  flex: 1;
  min-width: 200px;
  font-size: 0.85rem;
  color: #c0c0e0;
  margin: 0;
}

/* 建议 */
.suggestion-list {
  margin: 0;
  padding-left: 20px;
}

.suggestion-item {
  font-size: 0.85rem;
  color: #c0c0e0;
  line-height: 1.7;
  margin-bottom: 6px;
}

/* 优化代码 */
.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.copy-btn {
  padding: 5px 12px;
  background: rgba(0, 245, 212, 0.08);
  border: 1px solid rgba(0, 245, 212, 0.2);
  border-radius: 8px;
  color: #00f5d4;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.copy-btn:hover {
  background: rgba(0, 245, 212, 0.15);
}

.optimized-code {
  max-height: 320px;
  overflow: auto;
  padding: 16px;
  background: rgba(10, 10, 26, 0.7);
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 10px;
  color: #10b981;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 0.82rem;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 空态 / 错误 */
.empty-state {
  text-align: center;
  padding: 60px 24px;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 12px;
}

.empty-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: #e8e8ff;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 0.85rem;
  color: #9090b8;
  margin: 0;
}

.load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 12px;
  color: #ef4444;
  font-size: 0.9rem;
}

.retry-btn {
  padding: 6px 14px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.25);
  border-radius: 8px;
  color: #ef4444;
  font-size: 0.85rem;
  cursor: pointer;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .page-header {
    padding: 10px 12px;
    gap: 10px;
    flex-wrap: wrap;
  }
  .back-btn span {
    display: none;
  }
  .analyzer-content {
    padding: 16px;
  }
  .result-summary {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
