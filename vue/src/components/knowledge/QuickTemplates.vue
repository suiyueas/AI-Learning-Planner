<template>
  <div class="quick-templates">
    <span class="template-label">🚀 快速开始</span>
    <div class="template-bubbles">
      <span
        v-for="template in templates"
        :key="template.id"
        class="template-bubble"
        @click="$emit('run', template)"
      >
        {{ template.icon }} {{ template.label }}
      </span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  templates: {
    type: Array,
    default: () => [
      { id: 't1', icon: '📊', label: '这个文档讲了什么？', workflow: 'summarize' },
      { id: 't2', icon: '🔑', label: '提取关键知识点', workflow: 'extract' },
      { id: 't3', icon: '❓', label: '出5道题考考我', workflow: 'quiz' }
    ]
  }
})

defineEmits(['run'])
</script>

<style lang="scss" scoped>
.quick-templates {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(17, 17, 39, 0.4);
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);
}

.template-label {
  font-size: 0.78rem;
  color: var(--text-muted, #8b8ba8);
  font-weight: 500;
  white-space: nowrap;
}

.template-bubbles {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.template-bubble {
  padding: 6px 14px;
  background: rgba(124, 97, 255, 0.08);
  border: 1px solid rgba(124, 97, 255, 0.15);
  border-radius: 20px;
  color: #c4c4d4;
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(124, 97, 255, 0.18);
    border-color: rgba(124, 97, 255, 0.3);
    color: #fff;
    transform: translateY(-1px);
  }
}
</style>