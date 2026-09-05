<template>
  <div class="capability-dashboard">
    <UTabs :tabs="tabs" v-model="activeTab" />

    <div class="cd-panel">
      <!-- 这里引用现有 capability 视图组件的核心逻辑 -->
      <div class="cd-placeholder">
        <p class="cd-ph-text">选择上方标签页查看详细数据</p>
        <p class="cd-ph-hint">功能模块将从独立页面迁移至此</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { UTabs } from '@/components/ui'

defineProps({
  session: { type: Object, default: () => ({}) }
})

const tabs = [
  { id: 'diagnosis', label: '诊断详情', icon: '🔍' },
  { id: 'weakness', label: '薄弱点', icon: '⚠️' },
  { id: 'progress', label: '进度', icon: '📈' },
  { id: 'planning', label: '规划', icon: '🗺️' },
  { id: 'adaptive', label: '自适应', icon: '🧠' },
  { id: 'report', label: '报告', icon: '📊' }
]

const activeTab = ref('diagnosis')
</script>

<style lang="scss" scoped>
@use '../../../styles/variables' as *;

.capability-dashboard {
  padding: $space-4;
  display: flex;
  flex-direction: column;
  gap: $space-4;
}

.cd-panel {
  flex: 1;
  overflow-y: auto;
}

.cd-placeholder {
  text-align: center;
  padding: $space-8;
}

.cd-ph-text {
  font-size: $text-sm;
  color: $text-muted;
  margin: 0 0 $space-1;
}

.cd-ph-hint {
  font-size: $text-xs;
  color: $text-placeholder;
  margin: 0;
}
</style>
