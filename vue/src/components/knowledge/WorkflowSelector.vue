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
.workflow-selector {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 16px;
  background: rgba(17, 17, 39, 0.4);
  border-bottom: 1px solid rgba(100, 100, 180, 0.1);
}

.workflow-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  text-align: center;

  &:hover {
    background: rgba(124, 97, 255, 0.1);
    border-color: rgba(124, 97, 255, 0.25);
    transform: translateY(-2px);
  }

  &.active {
    background: rgba(124, 97, 255, 0.15);
    border-color: rgba(124, 97, 255, 0.4);
    box-shadow: 0 0 20px rgba(124, 97, 255, 0.15);

    .flow-icon {
      transform: scale(1.1);
    }

    .flow-label {
      color: #fff;
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
  color: var(--text-primary, #f0f0ff);
  transition: color 0.25s ease;
}

.flow-desc {
  font-size: 0.75rem;
  color: var(--text-muted, #8b8ba8);
  line-height: 1.3;
}
</style>