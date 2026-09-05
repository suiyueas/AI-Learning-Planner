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
@use '../../styles/variables' as *;

.quick-templates {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba($bg-elevated, 0.25);
  border-bottom: 1px solid rgba($border-subtle, 0.3);
}

.template-label {
  font-size: 0.78rem;
  color: $text-muted;
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
  background: rgba($accent-indigo, 0.06);
  border: 1px solid rgba($accent-indigo, 0.12);
  border-radius: 20px;
  color: $text-secondary;
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba($accent-indigo, 0.12);
    border-color: rgba($accent-indigo, 0.25);
    color: $accent-indigo;
    transform: translateY(-1px);
  }
}
</style>