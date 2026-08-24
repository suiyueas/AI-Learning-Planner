<template>
  <div class="quick-scenarios">
    <div class="scenarios-header">
      <span class="scenarios-label">快捷场景</span>
    </div>
    <div class="scenarios-list">
      <button
        v-for="scenario in scenarios"
        :key="scenario.id"
        class="scenario-btn"
        @click="$emit('select', scenario)"
      >
        <span class="scenario-icon">{{ scenario.icon }}</span>
        <span class="scenario-label">{{ scenario.label }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  scenarios: {
    type: Array,
    default: () => [
      { id: 1, icon: '📝', label: '制定计划', prompt: '请为我制定一个关于「在此输入学习目标」的详细学习计划，包含阶段划分、时间节点和具体任务。' },
      { id: 2, icon: '💡', label: '解释概念', prompt: '请用通俗易懂的方式解释「在此输入概念名称」，并给出实际应用举例。' },
      { id: 3, icon: '✍️', label: '生成习题', prompt: '请围绕「在此输入主题」生成一套由浅入深的练习题（5-10道），附带详细解析。' },
      { id: 4, icon: '🔍', label: '推荐资源', prompt: '请推荐关于「在此输入主题」的优质学习资源，包括书籍、视频和在线课程。' }
    ]
  }
})

defineEmits(['select'])
</script>

<style lang="scss" scoped>
.quick-scenarios {
  padding: 12px 16px;
  background: rgba(17, 17, 39, 0.5);
  border-bottom: 1px solid rgba(100, 100, 180, 0.1);
}

.scenarios-header {
  margin-bottom: 10px;
}

.scenarios-label {
  font-size: 0.78rem;
  color: var(--text-muted, #8b8ba8);
  font-weight: 500;
}

.scenarios-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.scenario-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
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

  .scenario-icon {
    font-size: 0.9rem;
  }

  .scenario-label {
    font-weight: 500;
  }
}
</style>