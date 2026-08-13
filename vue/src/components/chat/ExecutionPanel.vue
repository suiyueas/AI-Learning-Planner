<template>
  <div class="execution-panel">
    <!-- AI 状态 -->
    <div class="panel-section">
      <div class="section-header">
        <span class="section-icon">⚡</span>
        <span class="section-title">AI 状态</span>
      </div>
      <div class="section-body">
        <div class="info-row">
          <span class="info-label">状态</span>
          <span class="info-value">
            <span class="status-dot" :class="aiReady ? 'green' : 'yellow'"></span>
            {{ aiStatusText }}
          </span>
        </div>
      </div>
    </div>

    <!-- 联网搜索 -->
    <div class="panel-section">
      <div class="section-header">
        <span class="section-icon">🌐</span>
        <span class="section-title">联网搜索</span>
      </div>
      <div class="section-body">
        <div class="info-row">
          <span class="info-label">Tavily API</span>
          <div class="web-toggle" :class="{ on: webSearchEnabled }" @click="$emit('toggle-web-search')">
            <span class="toggle-track">
              <span class="toggle-thumb"></span>
            </span>
            <span class="toggle-text">{{ webSearchEnabled ? '已开启' : '已关闭' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 知识库状态（API 数据，点击弹出选择面板） -->
    <div class="panel-section clickable-section" @click="$emit('open-knowledge')">
      <div class="section-header">
        <span class="section-icon">📚</span>
        <span class="section-title">知识库</span>
        <span v-if="contextStore.selectedDocCount > 0" class="sel-badge">{{ contextStore.selectedDocCount }} 已选</span>
      </div>
      <div class="section-body">
        <div class="info-row">
          <span class="info-label">状态</span>
          <span class="info-value">
            <span class="status-dot" :class="knowledgeStatus.connected ? 'green' : 'gray'"></span>
            {{ knowledgeStatus.connected ? '已接入' : '未接入' }}
          </span>
        </div>
        <div class="info-row">
          <span class="info-label">文档数</span>
          <span class="info-value"><span class="num-green">{{ knowledgeStatus.documentCount || 0 }}</span> 个</span>
        </div>
        <div class="info-row">
          <span class="info-label">片段数</span>
          <span class="info-value">{{ knowledgeStatus.chunkCount || 0 }}</span>
        </div>
        <div v-if="currentKnowledgeRef" class="current-ref">
          <span class="ref-label">当前引用</span>
          <span class="ref-value">{{ currentKnowledgeRef }}</span>
        </div>
      </div>
    </div>

    <!-- MCP 服务状态（API 数据，点击弹出选择面板） -->
    <div class="panel-section clickable-section" @click="$emit('open-tools')">
      <div class="section-header">
        <span class="section-icon">🔌</span>
        <span class="section-title">MCP 服务</span>
        <span v-if="contextStore.selectedToolCount > 0" class="sel-badge">{{ contextStore.selectedToolCount }} 已选</span>
      </div>
      <div class="section-body">
        <div class="info-row">
          <span class="info-label">可用工具</span>
          <span class="info-value">{{ mcpStatus.availableCount || 0 }} 个</span>
        </div>
        <div class="info-row">
          <span class="info-label">调用次数</span>
          <span class="info-value"><span class="num-blue">{{ realTotalCalls }}</span> 次</span>
        </div>
      </div>
    </div>

    <!-- Token 用量 -->
    <div class="panel-section">
      <div class="section-header">
        <span class="section-icon">📊</span>
        <span class="section-title">Token 用量</span>
      </div>
      <div class="section-body">
        <div class="token-row">
          <span class="token-used">{{ tokens.current }}</span>
          <span class="token-sep">/</span>
          <span class="token-total">{{ tokens.total }}</span>
        </div>
        <div class="token-bar">
          <div class="token-bar-fill" :style="{ width: tokenPercent + '%' }"></div>
        </div>
        <div class="token-pct">{{ tokenPercent.toFixed(1) }}%</div>
      </div>
    </div>
</div>
</template>

<script setup>
import { computed } from 'vue'
import { useContextStore } from '@/stores/contextStore'
import { useToolsStore } from '@/stores/toolsStore'

const contextStore = useContextStore()
const toolsStore = useToolsStore()

const props = defineProps({
  knowledgeStatus: { type: Object, default: () => ({ connected: false, documentCount: 0, chunkCount: 0 }) },
  mcpStatus: { type: Object, default: () => ({ availableCount: 0, totalCalls: 0 }) },
  tokens: { type: Object, default: () => ({ current: 0, total: 30720 }) },
  currentKnowledgeRef: { type: String, default: '' },
  aiStatusText: { type: String, default: '就绪' },
  webSearchEnabled: { type: Boolean, default: false }
})

defineEmits(['toggle-web-search', 'open-knowledge', 'open-tools'])

const aiReady = computed(() => !['thinking', 'generating', 'searching', 'executing'].includes(props.aiStatusText))

const tokenPercent = computed(() => {
  if (!props.tokens.total) return 0
  return Math.min((props.tokens.current / props.tokens.total) * 100, 100)
})

// 真实的工具调用总次数（从 tool_call_stats 表统计）
const realTotalCalls = computed(() =>
  Object.values(toolsStore.toolStats).reduce((sum, s) => sum + (s.totalCalls || 0), 0)
)
</script>

<style lang="scss" scoped>
.execution-panel {
  width: 100%;
  height: 100%;
  background: rgba(18, 18, 42, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(100,100,180,0.08);
  padding: 12px 10px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  scrollbar-width: thin;
  scrollbar-color: rgba(0,245,212,0.12) transparent;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(0,245,212,0.12); border-radius: 2px; }
}

.panel-section {
  background: var(--bg-card-soft);
  border: 1px solid var(--border-card-soft);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03), inset 0 0 12px rgba(0, 0, 0, 0.15);
}

.clickable-section {
  cursor: pointer;
  transition: border-color 0.2s;
  &:hover {
    border-color: rgba(0, 245, 212, 0.15);
  }
}

.sel-badge {
  margin-left: auto;
  font-size: 10px;
  font-weight: 700;
  color: #ffffff;
  background: var(--badge-bg);
  padding: 2px 8px;
  border-radius: 8px;
  white-space: nowrap;
  box-shadow: 0 0 8px rgba(124, 107, 245, 0.2);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 10px;
  border-bottom: 1px solid rgba(100,100,180,0.06);
}

.section-icon { font-size: 11px; opacity: 0.7; }

.section-title {
  font-size: 12px;
  font-weight: 500;
  color: var(--title-status);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.section-body {
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.info-label {
  color: var(--text-sub);
  font-weight: 500;
}

.info-value {
  color: var(--text-input);
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 数据数字高亮 */
.num-green {
  color: var(--text-data-green);
  font-weight: 700;
}

.num-blue {
  color: var(--text-data-blue);
  font-weight: 700;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
  &.green { background: var(--status-ready-text); box-shadow: 0 0 6px rgba(52,211,153,0.5); animation: pulse 2s ease-in-out infinite; }
  &.yellow { background: #F59E0B; box-shadow: 0 0 6px rgba(245,158,11,0.4); animation: pulse 1.5s infinite; }
  &.gray { background: var(--text-placeholder); }
}

.current-ref {
  font-size: 11px;
  color: var(--text-sub);
  padding: 4px 6px;
  background: rgba(124,107,245,0.06);
  border: 1px solid rgba(124,107,245,0.12);
  border-radius: 4px;
  margin-top: 2px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ref-label {
  display: block;
  font-size: 10px;
  color: var(--text-placeholder);
  margin-bottom: 2px;
}

.ref-value {
  font-size: 11px;
  color: #c8d0e8;
}

/* MCP 服务 */
.mcp-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 3px 0;
  font-size: 12px;
}

.mcp-name {
  color: #a0a0c8;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Token */
.token-row {
  display: flex;
  align-items: baseline;
  gap: 3px;
  font-size: 12px;
}

.token-used {
  color: var(--title-status);
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

.token-sep { color: var(--text-placeholder); }

.token-total {
  color: var(--text-placeholder);
  font-family: 'JetBrains Mono', monospace;
}

.token-bar {
  height: 4px;
  background: rgba(100,100,180,0.1);
  border-radius: 2px;
  overflow: hidden;
  margin-top: 4px;
}

.token-bar-fill {
  height: 100%;
  background: var(--bar-gradient);
  border-radius: 2px;
  transition: width 0.5s ease;
}

.token-pct {
  font-size: 10px;
  color: var(--text-sub);
  text-align: right;
  margin-top: 2px;
}

/* 联网搜索开关 */
.web-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}
.toggle-track {
  position: relative;
  width: 32px;
  height: 18px;
  background: #2a3040;
  border: 1px solid var(--border-ctrl);
  border-radius: 9px;
  transition: all 0.3s ease;
}
.web-toggle.on .toggle-track {
  background: var(--border-ctrl-hover);
  border-color: var(--border-ctrl-hover);
  box-shadow: 0 0 8px rgba(124, 107, 245, 0.35);
}
.toggle-thumb {
  position: absolute;
  top: 1px;
  left: 1px;
  width: 14px;
  height: 14px;
  background: #9aa0b0;
  border-radius: 50%;
  transition: all 0.3s ease;
}
.web-toggle.on .toggle-thumb {
  left: 15px;
  background: #ffffff;
  box-shadow: 0 0 6px rgba(255, 255, 255, 0.5);
}
.toggle-text {
  font-size: 11px;
  color: #9aa0b0;
  font-weight: 500;
}
.web-toggle.on .toggle-text {
  color: #ffffff;
  font-weight: 600;
}

/* 底部声明 */
.panel-footer {
  margin-top: auto;
  padding: 10px 8px;
  font-size: 10px;
  color: #606090;
  text-align: center;
  line-height: 1.5;
  border-top: 1px solid rgba(100,100,180,0.06);
  opacity: 0.7;
}

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
</style>
