<template>
  <transition name="drawer-slide">
    <div v-show="visible" class="detail-drawer-overlay" @click.self="$emit('close')">
      <div class="detail-drawer">
        <div class="drawer-header">
          <span class="drawer-title">详细信息</span>
          <button class="drawer-close" @click="$emit('close')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>
        <div class="drawer-body">
          <!-- 知识库引用 -->
          <div class="drawer-section">
            <div class="section-head">
              <span class="section-icon">📚</span>
              <span class="section-label">知识库引用</span>
            </div>
            <div class="section-body">
              <div v-if="knowledgeRefs.length === 0" class="empty-text">暂无引用</div>
              <div v-for="ref in knowledgeRefs" :key="ref.id" class="ref-card">
                <div class="ref-source">{{ ref.source }}</div>
                <div class="ref-content">{{ ref.content }}</div>
                <div class="ref-relevance">
                  <div class="relevance-bar">
                    <div class="relevance-fill" :style="{ width: (ref.relevance * 100) + '%' }"></div>
                  </div>
                  <span class="relevance-pct">{{ (ref.relevance * 100).toFixed(0) }}%</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 联网搜索 -->
          <div class="drawer-section">
            <div class="section-head">
              <span class="section-icon">🌐</span>
              <span class="section-label">联网搜索</span>
              <button class="search-toggle" :class="{ active: webSearch }" @click="$emit('toggleSearch', !webSearch)">
                {{ webSearch ? '开' : '关' }}
              </button>
            </div>
            <div v-if="webSearch" class="section-body">
              <div v-if="searchResults.length === 0" class="empty-text">暂无搜索结果</div>
              <div v-for="result in searchResults" :key="result.id" class="result-card">
                <div class="result-title">{{ result.title }}</div>
                <div class="result-url">{{ result.url }}</div>
                <div class="result-snippet">{{ result.snippet }}</div>
              </div>
            </div>
          </div>

          <!-- 活跃技能 -->
          <div class="drawer-section">
            <div class="section-head">
              <span class="section-icon">⚡</span>
              <span class="section-label">活跃技能</span>
            </div>
            <div class="section-body">
              <div v-for="skill in skills" :key="skill.id" class="skill-row">
                <span class="skill-name">{{ skill.name }}</span>
                <div class="skill-bar-wrap">
                  <div class="skill-bar">
                    <div class="skill-bar-fill" :style="{ width: skill.weight + '%' }"></div>
                  </div>
                  <span class="skill-pct">{{ skill.weight }}%</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 上下文用量 -->
          <div class="drawer-section">
            <div class="section-head">
              <span class="section-icon">📊</span>
              <span class="section-label">上下文用量</span>
            </div>
            <div class="section-body">
              <div class="ctx-row">
                <span class="ctx-label">已用</span>
                <span class="ctx-value">{{ contextTokens.current }}</span>
                <span class="ctx-sep">/</span>
                <span class="ctx-total">{{ contextTokens.total }}</span>
              </div>
              <div class="ctx-bar">
                <div class="ctx-bar-fill" :style="{ width: (contextTokens.current / contextTokens.total * 100) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  knowledgeRefs: { type: Array, default: () => [] },
  searchResults: { type: Array, default: () => [] },
  skills: { type: Array, default: () => [] },
  contextTokens: { type: Object, default: () => ({ current: 0, total: 30720 }) },
  webSearch: { type: Boolean, default: false }
})

defineEmits(['close', 'toggleSearch'])
</script>

<style scoped>
.detail-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 900;
  background: rgba(0,0,0,0.35);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.detail-drawer {
  width: 100%;
  max-height: 42vh;
  background: rgba(10, 14, 26, 0.96);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid rgba(0,229,255,0.15);
  border-radius: 16px 16px 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(0,229,255,0.06);
  flex-shrink: 0;
}
.drawer-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.drawer-close {
  width: 28px;
  height: 28px;
  border: 1px solid rgba(100,100,180,0.1);
  border-radius: 6px;
  background: transparent;
  color: #94A3B8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.drawer-close:hover {
  background: rgba(100,100,180,0.08);
  color: var(--text-primary);
}
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px 16px;
  display: flex;
  gap: 16px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0,229,255,0.15) transparent;
}
.drawer-body::-webkit-scrollbar { width: 4px; }
.drawer-body::-webkit-scrollbar-track { background: transparent; }
.drawer-body::-webkit-scrollbar-thumb { background: rgba(0,229,255,0.15); border-radius: 2px; }

.drawer-section {
  flex: 1;
  min-width: 0;
}
.section-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 0 8px;
  border-bottom: 1px solid rgba(100,100,180,0.06);
  margin-bottom: 8px;
  position: relative;
  padding-left: 12px;
}
.section-head::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #7c6bf5, #5b4bd5);
  box-shadow: 0 0 8px rgba(124, 107, 245, 0.4);
}
.section-icon { font-size: 14px; }
.section-label {
  flex: 1;
  font-size: 12px;
  font-weight: 600;
  color: var(--title-section);
}
.section-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.empty-text {
  font-size: 12px;
  color: var(--text-sub);
  text-align: center;
  padding: 12px 0;
}

/* 知识库引用 */
.ref-card {
  padding: 8px 10px;
  background: rgba(100,100,180,0.04);
  border: 1px solid rgba(100,100,180,0.06);
  border-radius: 8px;
}
.ref-source {
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  color: var(--accent-primary);
  margin-bottom: 4px;
}
.ref-content {
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 6px;
}
.ref-relevance {
  display: flex;
  align-items: center;
  gap: 8px;
}
.relevance-bar {
  flex: 1;
  height: 3px;
  background: rgba(100,100,180,0.08);
  border-radius: 2px;
  overflow: hidden;
}
.relevance-fill {
  height: 100%;
  background: linear-gradient(90deg, #10B981, #00E5FF);
  border-radius: 2px;
  transition: width 0.5s ease;
}
.relevance-pct {
  font-size: 10px;
  color: var(--accent-primary);
  font-weight: 500;
}

/* 联网搜索 */
.search-toggle {
  padding: 4px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid var(--border-ctrl);
  background: #2a3040;
  color: #9aa0b0;
  cursor: pointer;
  transition: all 0.2s;
}
.search-toggle.active {
  background: #7c6bf5;
  color: #ffffff;
  border-color: #7c6bf5;
  box-shadow: 0 0 8px rgba(124, 107, 245, 0.35);
}
.result-card {
  padding: 8px 10px;
  background: rgba(100,100,180,0.04);
  border: 1px solid rgba(100,100,180,0.06);
  border-radius: 8px;
}
.result-title {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 2px;
}
.result-url {
  font-size: 10px;
  font-family: 'JetBrains Mono', monospace;
  color: var(--accent-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.result-snippet {
  font-size: 11px;
  color: var(--text-sub);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 技能 */
.skill-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
}
.skill-name {
  font-size: 12px;
  color: var(--text-secondary);
  width: 72px;
  flex-shrink: 0;
}
.skill-bar-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}
.skill-bar {
  flex: 1;
  height: 4px;
  background: rgba(100,100,180,0.08);
  border-radius: 2px;
  overflow: hidden;
}
.skill-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-primary), var(--accent-secondary));
  border-radius: 2px;
  transition: width 0.5s ease;
}
.skill-pct {
  font-size: 11px;
  font-weight: 600;
  color: var(--accent-primary);
  min-width: 30px;
  text-align: right;
}

/* 上下文用量 */
.ctx-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-size: 12px;
  margin-bottom: 6px;
}
.ctx-label { color: var(--text-sub); }
.ctx-value { color: var(--title-status); font-weight: 700; font-family: 'JetBrains Mono', monospace; }
.ctx-sep { color: var(--text-placeholder); }
.ctx-total { color: var(--text-placeholder); font-family: 'JetBrains Mono', monospace; }
.ctx-bar {
  height: 4px;
  background: rgba(100,100,180,0.1);
  border-radius: 2px;
  overflow: hidden;
}
.ctx-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #7c6bf5, #5b4bd5);
  border-radius: 2px;
  transition: width 0.5s ease;
}

/* 动画 */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: opacity 0.3s ease;
}
.drawer-slide-enter-active .detail-drawer,
.drawer-slide-leave-active .detail-drawer {
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
.drawer-slide-enter-from,
.drawer-slide-leave-to {
  opacity: 0;
}
.drawer-slide-enter-from .detail-drawer {
  transform: translateY(100%);
}
.drawer-slide-leave-to .detail-drawer {
  transform: translateY(100%);
}

@media (max-width: 1024px) {
  .drawer-body {
    flex-wrap: wrap;
  }
  .drawer-section {
    min-width: 200px;
  }
}
</style>