<template>
  <div class="workflow-selector">
    <div
      v-for="flow in workflows"
      :key="flow.id"
      class="workflow-item"
      :class="{ active: currentWorkflow === flow.id }"
      @click="$emit('select', flow.id)"
    >
      <span class="flow-icon">{{ flow.icon }}</span>
      <span class="flow-label">{{ flow.label }}</span>
      <span class="flow-desc">{{ flow.desc }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  workflows: {
    type: Array,
    default: () => [
      {
        id: 'retrieve',
        icon: '📖',
        label: '智能检索',
        desc: '查找知识库中相关内容'
      },
      {
        id: 'extract',
        icon: '✍️',
        label: '知识点提取',
        desc: '从文档中提取核心知识点'
      },
      {
        id: 'quiz',
        icon: '📝',
        label: '生成测验题',
        desc: '基于知识库内容生成测验题'
      },
      {
        id: 'summarize',
        icon: '📋',
        label: '文档总结',
        desc: '一键生成文档精华摘要'
      }
    ]
  },
  currentWorkflow: {
    type: String,
    default: 'retrieve'
  }
})

defineEmits(['select'])
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.workflow-selector {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 16px;
  background: rgba($bg-surface, 0.4);
  border-bottom: 1px solid rgba($border-subtle, 0.5);
}

.workflow-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background: rgba($bg-elevated, 0.3);
  border: 1px solid rgba($border-default, 0.5);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  text-align: center;

  &:hover {
    background: rgba($accent-indigo, 0.08);
    border-color: rgba($accent-indigo, 0.2);
    transform: translateY(-2px);
  }

  &.active {
    background: rgba($accent-indigo, 0.12);
    border-color: rgba($accent-indigo, 0.4);
    box-shadow: 0 0 20px rgba($accent-indigo, 0.12);

    .flow-icon {
      transform: scale(1.1);
    }

    .flow-label {
      color: $accent-indigo;
    }
  }
}

.flow-icon {
  font-size: 1.8rem;
  transition: transform 0.25s ease;
}

.flow-label {
  font-size: 0.88rem;
  font-weight: 600;
  color: $text-primary;
  transition: color 0.25s ease;
}

.flow-desc {
  font-size: 0.75rem;
  color: $text-muted;
  line-height: 1.3;
}
</style>