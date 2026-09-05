<template>
  <div class="plan-preview">
    <div class="pp-header">
      <h2 class="pp-title">🗺️ 学习规划</h2>
      <p class="pp-desc">基于你的诊断结果，AI 为你生成了以下学习路径</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="!plan" class="pp-loading">
      <div class="pp-spinner"></div>
      <p>正在生成学习路径...</p>
    </div>

    <!-- 规划概览统计 -->
    <div v-else-if="plan" class="pp-stats">
      <div class="pp-stat">
        <span class="pp-stat-value">{{ plan.nodes?.length || 0 }}</span>
        <span class="pp-stat-label">学习阶段</span>
      </div>
      <div class="pp-stat">
        <span class="pp-stat-value">{{ totalDuration }}</span>
        <span class="pp-stat-label">预计总时长</span>
      </div>
      <div class="pp-stat">
        <span class="pp-stat-value">{{ plan.difficulty || '中级' }}</span>
        <span class="pp-stat-label">难度等级</span>
      </div>
    </div>

    <!-- 可视化时间线 -->
    <div v-if="plan" class="pp-timeline">
      <div
        v-for="(node, idx) in plan.nodes"
        :key="idx"
        class="pp-timeline-node"
        :class="{
          current: idx === plan.currentNode,
          completed: idx < plan.currentNode,
          expanded: expandedNode === idx
        }"
        @click="toggleNode(idx)"
      >
        <!-- 时间线左侧标记 -->
        <div class="pp-timeline-marker">
          <div class="pp-timeline-dot">
            <span v-if="idx < plan.currentNode" class="pp-timeline-check">✓</span>
            <span v-else-if="idx === plan.currentNode" class="pp-timeline-num">{{ idx + 1 }}</span>
            <span v-else class="pp-timeline-num">{{ idx + 1 }}</span>
          </div>
          <div v-if="idx < plan.nodes.length - 1" class="pp-timeline-line"></div>
        </div>

        <!-- 节点卡片 -->
        <div class="pp-timeline-card">
          <div class="pp-timeline-header">
            <span class="pp-timeline-name">{{ node.name }}</span>
            <div class="pp-timeline-tags">
              <span class="pp-tag pp-tag--duration">{{ node.duration || '1-2周' }}</span>
              <span
                v-if="node.difficulty"
                class="pp-tag"
                :class="`pp-tag--${node.difficulty === '初级' ? 'easy' : node.difficulty === '中级' ? 'medium' : 'hard'}`"
              >
                {{ node.difficulty }}
              </span>
            </div>
          </div>

          <p class="pp-timeline-desc">{{ node.description }}</p>

          <!-- 展开后的详细信息 -->
          <Transition name="pp-expand">
            <div v-if="expandedNode === idx" class="pp-timeline-detail">
              <div v-if="node.prerequisites?.length" class="pp-detail-section">
                <span class="pp-detail-label">前置知识</span>
                <div class="pp-detail-tags">
                  <span v-for="(pr, pi) in node.prerequisites" :key="pi" class="pp-detail-tag">
                    {{ pr }}
                  </span>
                </div>
              </div>

              <div v-if="node.topics?.length" class="pp-detail-section">
                <span class="pp-detail-label">核心知识点</span>
                <ul class="pp-detail-list">
                  <li v-for="(topic, ti) in node.topics" :key="ti">{{ topic }}</li>
                </ul>
              </div>

              <div v-if="node.resources?.length" class="pp-detail-section">
                <span class="pp-detail-label">推荐资源</span>
                <div class="pp-detail-resources">
                  <a
                    v-for="(res, ri) in node.resources"
                    :key="ri"
                    :href="res.url"
                    target="_blank"
                    class="pp-detail-resource"
                  >
                    <span class="pp-resource-icon">{{ res.type === 'video' ? '🎬' : res.type === 'article' ? '📄' : '📚' }}</span>
                    <span class="pp-resource-title">{{ res.title }}</span>
                  </a>
                </div>
              </div>
            </div>
          </Transition>

          <!-- 展开按钮 -->
          <button class="pp-expand-btn">
            <span v-if="expandedNode === idx">收起详情</span>
            <span v-else>查看详情</span>
            <span class="pp-expand-arrow" :class="{ rotated: expandedNode === idx }">▾</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div v-if="plan" class="pp-actions">
      <button class="pp-confirm-btn" @click="$emit('confirm')">
        确认并开始学习
      </button>
      <button class="pp-adjust-btn" @click="$emit('adjust')">
        调整规划
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  session: { type: Object, default: () => ({}) }
})

defineEmits(['confirm', 'adjust'])

const plan = computed(() => props.session.plan || null)
const expandedNode = ref(null)

// 计算总时长
const totalDuration = computed(() => {
  if (!plan.value?.nodes) return '未知'
  const total = plan.value.nodes.reduce((sum, n) => {
    const match = String(n.duration || '').match(/(\d+)/)
    return sum + (match ? parseInt(match[1]) : 2)
  }, 0)
  return `约${total}周`
})

const toggleNode = (idx) => {
  expandedNode.value = expandedNode.value === idx ? null : idx
}
</script>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.plan-preview {
  max-width: 720px;
  margin: 0 auto;
  padding-bottom: $space-4;
}

// ===== 头部 =====
.pp-header {
  text-align: center;
  margin-bottom: $space-6;
}

.pp-title {
  font-size: $text-2xl;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 $space-2;
}

.pp-desc {
  color: $text-muted;
  margin: 0;
  font-size: $text-sm;
}

// ===== 加载 =====
.pp-loading {
  text-align: center;
  padding: $space-12;
  color: $text-muted;
}

.pp-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid $border-default;
  border-top-color: $accent-indigo;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto $space-3;
}

@keyframes spin { to { transform: rotate(360deg); } }

// ===== 统计概览 =====
.pp-stats {
  display: flex;
  gap: $space-4;
  margin-bottom: $space-6;
}

.pp-stat {
  flex: 1;
  text-align: center;
  padding: $space-4;
  background: rgba($bg-surface, 0.5);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  transition: all $transition-normal;

  &:hover {
    border-color: rgba($accent-indigo, 0.2);
    transform: translateY(-1px);
  }
}

.pp-stat-value {
  display: block;
  font-family: $font-data;
  font-size: $text-xl;
  font-weight: 700;
  color: $accent-indigo;
  margin-bottom: $space-1;
}

.pp-stat-label {
  font-size: $text-xs;
  color: $text-muted;
}

// ===== 时间线 =====
.pp-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-bottom: $space-6;
}

.pp-timeline-node {
  display: flex;
  gap: $space-4;
  cursor: pointer;
  transition: all $transition-fast;

  &.completed {
    opacity: 0.65;
    &:hover { opacity: 0.85; }
  }
}

// ===== 时间线标记 =====
.pp-timeline-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 32px;
  flex-shrink: 0;
  padding-top: 4px;
}

.pp-timeline-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $text-xs;
  font-weight: 600;
  transition: all $transition-fast;
  flex-shrink: 0;

  .pp-timeline-node.completed & {
    background: rgba($color-success, 0.15);
    color: $color-success;
    border: 2px solid rgba($color-success, 0.3);
  }

  .pp-timeline-node.current & {
    background: $accent-indigo;
    color: white;
    box-shadow: 0 0 0 4px rgba($accent-indigo, 0.15);
  }

  .pp-timeline-node:not(.completed):not(.current) & {
    background: rgba($bg-elevated, 0.5);
    color: $text-muted;
    border: 2px solid $border-subtle;
  }
}

.pp-timeline-check {
  font-size: 12px;
}

.pp-timeline-num {
  font-size: 11px;
}

.pp-timeline-line {
  width: 2px;
  flex: 1;
  min-height: 24px;
  background: linear-gradient(to bottom, $border-subtle, rgba($border-subtle, 0.3));

  .pp-timeline-node.completed & {
    background: linear-gradient(to bottom, rgba($color-success, 0.3), rgba($color-success, 0.1));
  }
}

// ===== 节点卡片 =====
.pp-timeline-card {
  flex: 1;
  padding: $space-4;
  background: rgba($bg-surface, 0.4);
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  margin-bottom: $space-3;
  transition: all $transition-fast;

  .pp-timeline-node.current & {
    border-color: rgba($accent-indigo, 0.3);
    background: rgba($accent-indigo, 0.04);
  }

  .pp-timeline-node.expanded & {
    border-color: rgba($accent-indigo, 0.2);
  }

  &:hover {
    border-color: rgba($accent-indigo, 0.15);
  }
}

.pp-timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $space-3;
  margin-bottom: $space-2;
}

.pp-timeline-name {
  font-size: $text-base;
  font-weight: 600;
  color: $text-primary;
}

.pp-timeline-tags {
  display: flex;
  gap: $space-1;
  flex-shrink: 0;
}

.pp-tag {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: $radius-full;
  font-weight: 500;
  white-space: nowrap;

  &--duration {
    background: rgba($accent-indigo, 0.1);
    color: $accent-indigo;
  }

  &--easy {
    background: rgba($color-success, 0.1);
    color: $color-success;
  }

  &--medium {
    background: rgba($color-warning, 0.1);
    color: $color-warning;
  }

  &--hard {
    background: rgba($color-danger, 0.1);
    color: $color-danger;
  }
}

.pp-timeline-desc {
  font-size: $text-sm;
  color: $text-secondary;
  line-height: 1.5;
  margin: 0;
}

// ===== 展开详情 =====
.pp-expand-enter-active,
.pp-expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}

.pp-expand-enter-from,
.pp-expand-leave-to {
  opacity: 0;
  max-height: 0;
  margin-top: 0;
}

.pp-expand-enter-to,
.pp-expand-leave-from {
  opacity: 1;
  max-height: 500px;
  margin-top: $space-3;
}

.pp-timeline-detail {
  border-top: 1px solid $border-subtle;
  padding-top: $space-3;
}

.pp-detail-section {
  margin-bottom: $space-3;

  &:last-child { margin-bottom: 0; }
}

.pp-detail-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: $text-muted;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: $space-2;
}

.pp-detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $space-1;
}

.pp-detail-tag {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba($bg-elevated, 0.5);
  border: 1px solid $border-subtle;
  border-radius: $radius-full;
  color: $text-secondary;
}

.pp-detail-list {
  margin: 0;
  padding-left: $space-4;
  font-size: $text-sm;
  color: $text-secondary;
  line-height: 1.7;
}

.pp-detail-resources {
  display: flex;
  flex-direction: column;
  gap: $space-1;
}

.pp-detail-resource {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: $space-2 $space-3;
  background: rgba($bg-elevated, 0.3);
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  text-decoration: none;
  transition: all $transition-fast;
  font-size: $text-sm;
  color: $text-secondary;

  &:hover {
    background: rgba($bg-elevated, 0.5);
    border-color: rgba($accent-indigo, 0.2);
    color: $accent-indigo;
  }
}

.pp-resource-icon {
  flex-shrink: 0;
}

.pp-resource-title {
  font-weight: 500;
}

// ===== 展开按钮 =====
.pp-expand-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: $space-2;
  padding: 0;
  background: none;
  border: none;
  color: $text-muted;
  font-size: 11px;
  cursor: pointer;
  font-family: $font-sans;
  transition: color $transition-fast;

  &:hover {
    color: $accent-indigo;
  }
}

.pp-expand-arrow {
  font-size: 10px;
  transition: transform $transition-fast;

  &.rotated {
    transform: rotate(180deg);
  }
}

// ===== 操作按钮 =====
.pp-actions {
  display: flex;
  gap: $space-3;
  justify-content: center;
  padding: $space-4 0 $space-2;
}

.pp-confirm-btn {
  padding: 10px 28px;
  background: rgba($accent-indigo, 0.15);
  border: 1px solid rgba($accent-indigo, 0.3);
  border-radius: $radius-md;
  color: $accent-indigo;
  font-size: $text-base;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;

  &:hover {
    background: rgba($accent-indigo, 0.25);
    transform: translateY(-1px);
  }
}

.pp-adjust-btn {
  padding: 10px 28px;
  background: transparent;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-secondary;
  font-size: $text-base;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  font-family: $font-sans;

  &:hover {
    border-color: $border-medium;
    color: $text-primary;
  }
}
</style>