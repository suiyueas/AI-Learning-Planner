<template>
  <div class="kg-result">
    <div v-if="summary" class="result-summary">
      <span class="summary-icon">🔍</span>
      <span class="summary-text">{{ summary }}</span>
    </div>

    <!-- 节点列表 -->
    <div v-if="nodes && nodes.length > 0" class="node-list">
      <div
        v-for="(node, idx) in nodes"
        :key="idx"
        class="node-card"
        @click="toggleExpand(idx)"
      >
        <div class="node-header">
          <span class="node-icon">{{ getNodeIcon(node.type || node.category) }}</span>
          <span class="node-name">{{ node.name || node.label || node.title || '未命名节点' }}</span>
          <span v-if="node.difficulty" class="node-difficulty">
            {{ getDifficultyLabel(node.difficulty) }}
          </span>
          <span class="node-toggle">{{ expandedItems[idx] ? '▼' : '▶' }}</span>
        </div>

        <div v-if="node.description" class="node-desc">
          {{ node.description }}
        </div>

        <div v-if="expandedItems[idx]" class="node-body">
          <!-- 关联关系 -->
          <div v-if="node.relations && node.relations.length > 0" class="node-relations">
            <div class="node-section-title">🔗 关联关系</div>
            <div v-for="(rel, ri) in node.relations" :key="ri" class="relation-item">
              <span class="rel-type" :class="getRelationClass(rel.type)">{{ rel.type || '关联' }}</span>
              <span class="rel-target">{{ rel.target || rel.name || rel.label }}</span>
            </div>
          </div>

          <!-- 前置知识 -->
          <div v-if="node.prerequisites && node.prerequisites.length > 0" class="node-prereqs">
            <div class="node-section-title">📋 前置知识</div>
            <div class="prereq-list">
              <span v-for="(pre, pi) in node.prerequisites" :key="pi" class="prereq-tag">
                {{ pre.name || pre.label || pre }}
              </span>
            </div>
          </div>

          <!-- 后续知识 -->
          <div v-if="node.nextSteps && node.nextSteps.length > 0" class="node-next">
            <div class="node-section-title">➡️ 后续学习</div>
            <div class="next-list">
              <span v-for="(next, ni) in node.nextSteps" :key="ni" class="next-tag">
                {{ next.name || next.label || next }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="showRaw && rawContent" class="raw-content">
      <div class="raw-label">返回数据</div>
      <pre class="raw-json">{{ rawContent }}</pre>
    </div>

    <div v-else class="result-empty">⚠️ 未查询到知识图谱数据</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  data: {
    type: [Object, Array],
    default: null
  }
})

const expandedItems = ref({})

const nodes = computed(() => {
  if (!props.data) return null
  if (Array.isArray(props.data)) return props.data
  if (props.data.nodes) return props.data.nodes
  if (props.data.results) return props.data.results
  if (props.data.data && Array.isArray(props.data.data)) return props.data.data
  if (props.data.items) return props.data.items
  return null
})

const summary = computed(() => {
  if (!props.data) return null
  if (props.data.summary) return props.data.summary
  if (props.data.message) return props.data.message
  if (nodes.value && nodes.value.length > 0) {
    return `查询到 ${nodes.value.length} 个知识点节点`
  }
  return null
})

const showRaw = ref(false)
const rawContent = ref('')

onMounted(() => {
  if (!nodes.value && props.data) {
    showRaw.value = true
    try {
      rawContent.value = typeof props.data === 'string'
        ? props.data
        : JSON.stringify(props.data, null, 2)
    } catch {
      rawContent.value = String(props.data)
    }
  }
})

function getNodeIcon(type) {
  const map = {
    'concept': '🧩',
    'skill': '⚡',
    'topic': '📚',
    'course': '🎓',
    '知识点': '🧩',
    '技能': '⚡',
    '主题': '📚'
  }
  return map[type] || '🔵'
}

function getDifficultyLabel(val) {
  if (typeof val === 'number') {
    if (val <= 2) return '🟢 简单'
    if (val <= 4) return '🟡 中等'
    return '🔴 困难'
  }
  const map = { '简单': '🟢 简单', '中等': '🟡 中等', '困难': '🔴 困难', 'beginner': '🟢 简单', 'intermediate': '🟡 中等', 'advanced': '🔴 困难' }
  return map[val] || `📊 ${val}`
}

function getRelationClass(type) {
  const map = {
    '前置': 'pre',
    '后续': 'next',
    '包含': 'contains',
    '关联': 'related'
  }
  return map[type] || 'related'
}

function toggleExpand(idx) {
  expandedItems.value[idx] = !expandedItems.value[idx]
}
</script>

<style lang="scss" scoped>
.kg-result {
  padding: 4px 0;
}

.result-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(58, 134, 255, 0.04);
  border: 1px solid rgba(58, 134, 255, 0.08);
  border-radius: 10px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #b0b0d8;
  .summary-icon { font-size: 1.1rem; }
  .summary-text { font-weight: 500; }
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.node-card {
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(58, 134, 255, 0.15);
    transform: translateY(-1px);
  }
}

.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.node-icon { font-size: 1.1rem; flex-shrink: 0; }

.node-name {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: #f0f0ff;
  font-family: 'JetBrains Mono', monospace;
}

.node-difficulty {
  flex-shrink: 0;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(100, 100, 180, 0.08);
  font-family: 'JetBrains Mono', monospace;
}

.node-toggle {
  flex-shrink: 0;
  color: #606090;
  font-size: 10px;
}

.node-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #8080a8;
  line-height: 1.4;
}

.node-body {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.04);
}

.node-section-title {
  font-size: 11px;
  font-weight: 600;
  color: #8080a8;
  margin-bottom: 8px;
}

.node-relations {
  margin-bottom: 12px;
}

.relation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: rgba(100, 100, 180, 0.04);
  border-radius: 6px;
  margin-bottom: 4px;
  font-size: 12px;

  &:last-child { margin-bottom: 0; }
}

.rel-type {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;

  &.pre { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
  &.next { background: rgba(0, 245, 212, 0.1); color: #00f5d4; }
  &.contains { background: rgba(123, 97, 255, 0.1); color: #7b61ff; }
  &.related { background: rgba(100, 100, 180, 0.1); color: #8080a8; }
}

.rel-target {
  color: #b0b0d0;
}

.node-prereqs, .node-next {
  margin-bottom: 12px;
  &:last-child { margin-bottom: 0; }
}

.prereq-list, .next-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.prereq-tag, .next-tag {
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
}

.prereq-tag {
  background: rgba(245, 158, 11, 0.06);
  border: 1px solid rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.next-tag {
  background: rgba(0, 245, 212, 0.06);
  border: 1px solid rgba(0, 245, 212, 0.1);
  color: #00f5d4;
}

.raw-content {
  margin-top: 8px;
  .raw-label {
    font-size: 12px;
    font-weight: 600;
    color: #8080a8;
    margin-bottom: 8px;
  }
  .raw-json {
    padding: 12px;
    background: rgba(100, 100, 180, 0.04);
    border: 1px solid rgba(100, 100, 180, 0.08);
    border-radius: 8px;
    font-size: 12px;
    line-height: 1.6;
    color: #c0c0e0;
    overflow-x: auto;
    font-family: 'JetBrains Mono', monospace;
    white-space: pre-wrap;
    word-break: break-all;
    margin: 0;
    max-height: 300px;
    overflow-y: auto;
  }
}

.result-empty {
  text-align: center;
  padding: 24px;
  color: #8080a8;
  font-size: 13px;
}
</style>
