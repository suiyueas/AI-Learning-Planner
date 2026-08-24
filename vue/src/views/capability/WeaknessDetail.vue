<template>
  <div class="weakness-page">
    <!-- 顶部导航栏 -->
    <header class="detail-header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <ArrowLeft :size="20" />
          <span>返回</span>
        </button>
        <h1 class="header-title">
          <AlertTriangle :size="24" class="header-icon" />
          薄弱知识点专项分析
        </h1>
      </div>
      <button class="btn-action" @click="retakeAssessment">
        <RefreshCw :size="16" />
        重新测评
      </button>
    </header>

    <div class="detail-content">
      <!-- 测评来源说明 -->
      <section class="intro-card">
        <div class="intro-icon">🔍</div>
        <div class="intro-text">
          <p class="intro-title">基于最近测评（{{ lastAssess }}）发现以下薄弱环节</p>
          <p class="intro-desc">针对每个薄弱知识点，系统已为您分析核心问题并推荐学习资源，点击「开始学习」即可补足短板。</p>
        </div>
      </section>

      <!-- 薄弱点详情列表 -->
      <section
        v-for="(item, index) in weaknessItems"
        :key="item.name"
        class="weakness-card"
        :class="{ resolved: isResolved(item.name) }"
        :style="{ animationDelay: (index * 0.08) + 's' }"
      >
        <div class="weakness-header">
          <div class="weakness-title-row">
            <span class="weakness-icon">📘</span>
            <span class="weakness-name">{{ item.name }}</span>
            <span class="weakness-badge" :class="resolvedClass(item)">{{ resolvedText(item) }}</span>
          </div>
          <div class="weakness-score">
            <span class="score-value" :class="resolvedClass(item)">{{ item.percentage }}%</span>
            <span class="score-label">掌握度</span>
          </div>
        </div>

        <div class="weakness-progress">
          <div class="progress-track">
            <div class="progress-fill" :class="resolvedClass(item)" :style="{ width: item.percentage + '%' }"></div>
          </div>
          <span class="progress-text" :class="resolvedClass(item)">{{ item.percentage }}%</span>
        </div>

        <!-- 核心问题 -->
        <div class="issue-block">
          <div class="block-label">核心问题</div>
          <ul class="issue-list">
            <li v-for="(issue, i) in item.coreIssues" :key="i" class="issue-item">
              <span class="issue-dot"></span>
              {{ issue }}
            </li>
          </ul>
        </div>

        <!-- 推荐学习资源 -->
        <div class="resource-block">
          <div class="block-label">推荐学习资源</div>
          <div v-for="(res, i) in item.resources" :key="i" class="resource-item">
            <span class="resource-icon" :class="res.type">{{ res.typeIcon }}</span>
            <div class="resource-info">
              <span class="resource-title">{{ res.title }}</span>
              <span class="resource-duration">预计 {{ res.duration }}</span>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="weakness-actions">
          <button class="btn-start" @click="startLearn(item)">
            <Compass :size="16" />
            开始学习
            <ArrowRight :size="14" />
          </button>
          <button class="btn-resolve" :class="{ resolved: isResolved(item.name) }" @click="toggleResolved(item)">
            <CheckCircle2 :size="16" />
            {{ isResolved(item.name) ? '已解决 ✓' : '标记已解决' }}
          </button>
        </div>
      </section>

      <!-- 学习建议 -->
      <section class="advice-card">
        <div class="advice-title">
          <Lightbulb :size="18" />
          学习建议
        </div>
        <p class="advice-text">
          建议优先攻克「<span class="advice-strong">{{ priorityAdvice.name }}</span>」，因为它是「{{ priorityAdvice.prerequisite }}」的前置知识。
          完成后预计整体掌握度可提升至 <span class="advice-strong">{{ expectedMastery }}%</span>。
        </p>
        <div class="advice-paths">
          <div v-for="(advice, index) in adviceData" :key="index" class="advice-item">
            <span class="advice-num">{{ index + 1 }}</span>
            <div class="advice-info">
              <span class="advice-name">{{ advice.name }}</span>
              <span class="advice-course">推荐课程：{{ advice.course }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 底部操作 -->
      <div class="bottom-actions">
        <button class="btn-bottom primary" @click="viewFullPath">
          <Map :size="16" />
          查看完整学习路径
        </button>
        <button class="btn-bottom" @click="retakeAssessment">
          <RefreshCw :size="16" />
          重新测评
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft, RefreshCw, AlertTriangle, ArrowRight,
  Lightbulb, Compass, CheckCircle2, Map
} from 'lucide-vue-next'

const router = useRouter()

// 测评信息与薄弱点数据（与智能诊断页保持一致，后续可接入真实接口）
const lastAssess = '2026-07-15'

const weaknessItems = ref([
  {
    name: 'Python 面向对象编程',
    percentage: 45,
    level: 'danger',
    coreIssues: [
      '类和对象的定义不够熟练，经常混淆 self 的含义',
      '继承和多态的概念理解不够深入',
      '魔法方法（__init__、__str__）使用较少'
    ],
    resources: [
      { type: 'read', typeIcon: '📖', title: '阅读：Python 官方教程第9章（类和对象）', duration: '30min' },
      { type: 'video', typeIcon: '📺', title: '视频：Python 面向对象编程精讲', duration: '15min' },
      { type: 'practice', typeIcon: '✍️', title: '练习：自定义一个 Student 类并实现方法', duration: '20min' }
    ]
  },
  {
    name: '机器学习调参技巧',
    percentage: 52,
    level: 'warning',
    coreIssues: [
      '网格搜索和随机搜索的适用场景不清晰',
      '超参数对模型性能的影响理解不足'
    ],
    resources: [
      { type: 'read', typeIcon: '📖', title: '阅读：Scikit-learn 调参指南', duration: '25min' },
      { type: 'practice', typeIcon: '✍️', title: '练习：使用 GridSearchCV 优化 SVM', duration: '30min' }
    ]
  },
  {
    name: '数据库索引优化',
    percentage: 48,
    level: 'danger',
    coreIssues: [
      '联合索引的最左前缀原则理解不透彻',
      '索引失效场景（函数、隐式转换）掌握不全'
    ],
    resources: [
      { type: 'read', typeIcon: '📖', title: '阅读：MySQL 索引优化实战', duration: '40min' },
      { type: 'video', typeIcon: '📺', title: '视频：Explain 执行计划分析', duration: '20min' }
    ]
  },
  {
    name: 'RESTful API 设计',
    percentage: 55,
    level: 'warning',
    coreIssues: [
      '资源命名与 HTTP 方法映射不规范',
      '错误码与状态码使用混淆'
    ],
    resources: [
      { type: 'read', typeIcon: '📖', title: '阅读：RESTful API 设计最佳实践', duration: '30min' },
      { type: 'practice', typeIcon: '✍️', title: '练习：设计一个图书管理 API', duration: '25min' }
    ]
  },
  {
    name: '数据结构与算法',
    percentage: 60,
    level: 'warning',
    coreIssues: [
      '二叉树遍历的递归与迭代实现不熟练',
      '动态规划的状态转移方程建立困难'
    ],
    resources: [
      { type: 'video', typeIcon: '📺', title: '视频：二叉树遍历精讲', duration: '25min' },
      { type: 'practice', typeIcon: '✍️', title: '练习：LeetCode 二叉树 5 题', duration: '40min' }
    ]
  }
])

const adviceData = [
  { name: 'Python 面向对象编程', course: 'Python 进阶' },
  { name: '机器学习调参技巧', course: '机器学习实战' }
]

const priorityAdvice = { name: 'Python 面向对象编程', prerequisite: '机器学习调参' }
const expectedMastery = 88

// 标记已解决状态（本地持久化）
const resolvedKey = (name) => `weakness_resolved_${name}`
const isResolved = (name) => localStorage.getItem(resolvedKey(name)) === '1'
const toggleResolved = (item) => {
  if (isResolved(item.name)) {
    localStorage.removeItem(resolvedKey(item.name))
  } else {
    localStorage.setItem(resolvedKey(item.name), '1')
  }
}
const resolvedClass = (item) => isResolved(item.name) ? 'resolved' : item.level
const resolvedText = (item) => {
  if (isResolved(item.name)) return '已解决'
  return item.level === 'danger' ? '需加强' : '待提升'
}

const goBack = () => {
  router.push('/capability/diagnosis')
}

const retakeAssessment = () => {
  router.push('/assessment')
}

// 开始学习：携带薄弱点上下文跳转学习路径，由列表页展示针对性推荐
const startLearn = (item) => {
  router.push({
    path: '/learning-path',
    query: { from: 'weakness', subject: item.name }
  })
}

// 查看完整学习路径：携带全部薄弱点上下文
const viewFullPath = () => {
  const subjects = weaknessItems.value.map(w => w.name).join(',')
  router.push({
    path: '/learning-path',
    query: { from: 'weakness', subjects }
  })
}
</script>

<style scoped>
@use '../styles/variables' as *;
.weakness-page {
  min-height: 100vh;
  background: $bg-primary;
  padding-bottom: 80px;
}

/* ===== 顶部导航 ===== */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 48px;
  background: rgba($bg-primary, 0.8);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba($accent-secondary, 0.12);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 8px;
  color: $text-secondary;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-back:hover {
  color: $accent-primary;
  border-color: rgba($accent-primary, 0.2);
  background: rgba($accent-primary, 0.04);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.4rem;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.header-icon {
  color: #EF4444;
}

.btn-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  color: $accent-primary;
  border: 1px solid rgba($accent-primary, 0.25);
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 24px rgba($accent-primary, 0.15);
  border-color: rgba($accent-primary, 0.4);
}

/* ===== 内容区 ===== */
.detail-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 测评来源说明 */
.intro-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: rgba(239, 68, 68, 0.05);
  border: 1px solid rgba(239, 68, 68, 0.15);
  border-radius: 14px;
  animation: fadeInUp 0.5s ease both;
}

.intro-icon {
  font-size: 1.8rem;
  flex-shrink: 0;
}

.intro-title {
  font-size: 1rem;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
}

.intro-desc {
  font-size: 0.85rem;
  color: $text-secondary;
  line-height: 1.6;
}

/* 薄弱点卡片 */
.weakness-card {
  background: rgba($bg-primary, 0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 16px;
  padding: 28px;
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease both;

  &:hover {
    border-color: rgba($accent-primary, 0.15);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  }

  &.resolved {
    border-color: rgba(16, 185, 129, 0.25);
  }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.weakness-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.weakness-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.weakness-icon {
  font-size: 1.2rem;
}

.weakness-name {
  font-size: 1.05rem;
  font-weight: 600;
  color: $text-primary;
}

.weakness-badge {
  font-size: 0.75rem;
  padding: 2px 10px;
  border-radius: 20px;
  font-weight: 500;

  &.danger {
    background: rgba(239, 68, 68, 0.1);
    color: #EF4444;
    border: 1px solid rgba(239, 68, 68, 0.2);
  }

  &.warning {
    background: rgba(245, 158, 11, 0.1);
    color: #F59E0B;
    border: 1px solid rgba(245, 158, 11, 0.2);
  }

  &.resolved {
    background: rgba(16, 185, 129, 0.1);
    color: #10B981;
    border: 1px solid rgba(16, 185, 129, 0.2);
  }
}

.weakness-score {
  text-align: right;
}

.score-value {
  display: block;
  font-size: 1.4rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  line-height: 1;

  &.danger {
    color: #EF4444;
    text-shadow: 0 0 15px rgba(239, 68, 68, 0.3);
  }

  &.warning {
    color: #F59E0B;
    text-shadow: 0 0 15px rgba(245, 158, 11, 0.3);
  }

  &.resolved {
    color: #10B981;
    text-shadow: 0 0 15px rgba(16, 185, 129, 0.3);
  }
}

.score-label {
  font-size: 0.75rem;
  color: $text-secondary;
}

/* 进度条 */
.weakness-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.progress-track {
  flex: 1;
  height: 6px;
  background: rgba($accent-secondary, 0.06);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 1s ease;

  &.danger { background: linear-gradient(90deg, #EF4444, #DC2626); }
  &.warning { background: linear-gradient(90deg, #F59E0B, #D97706); }
  &.resolved { background: linear-gradient(90deg, #10B981, #059669); }
}

.progress-text {
  font-size: 0.85rem;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  color: $text-secondary;
  min-width: 40px;
  text-align: right;

  &.danger { color: #EF4444; }
  &.warning { color: #F59E0B; }
  &.resolved { color: #10B981; }
}

/* 核心问题 */
.issue-block {
  margin-bottom: 20px;
}

.block-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: $accent-primary;
  margin-bottom: 10px;
}

.issue-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.issue-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 0.88rem;
  color: #CBD5E1;
  line-height: 1.6;
}

.issue-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #EF4444;
  flex-shrink: 0;
  margin-top: 8px;
}

/* 推荐资源 */
.resource-block {
  margin-bottom: 20px;
}

.resource-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: rgba($accent-secondary, 0.03);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 10px;
  margin-bottom: 8px;
  transition: all 0.25s ease;

  &:hover {
    border-color: rgba($accent-primary, 0.15);
    background: rgba($accent-primary, 0.03);
  }
}

.resource-icon {
  font-size: 1.1rem;
  flex-shrink: 0;
}

.resource-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  gap: 12px;
}

.resource-title {
  font-size: 0.88rem;
  color: #E2E8F0;
}

.resource-duration {
  font-size: 0.78rem;
  color: $accent-primary;
  white-space: nowrap;
}

/* 操作按钮 */
.weakness-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid rgba($accent-secondary, 0.08);
}

.btn-start {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid rgba($accent-primary, 0.25);
  border-radius: 10px;
  color: $accent-primary;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 0 24px rgba($accent-primary, 0.15);
    border-color: rgba($accent-primary, 0.4);

    svg:last-child { transform: translateX(4px); }
  }

  svg:last-child { transition: transform 0.3s ease; }
}

.btn-resolve {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  background: transparent;
  border: 1px solid rgba($accent-secondary, 0.15);
  border-radius: 10px;
  color: $text-secondary;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(16, 185, 129, 0.3);
    color: #10B981;
  }

  &.resolved {
    border-color: rgba(16, 185, 129, 0.3);
    color: #10B981;
    background: rgba(16, 185, 129, 0.06);
  }
}

/* 学习建议 */
.advice-card {
  background: rgba($bg-primary, 0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 16px;
  padding: 28px;
  animation: fadeInUp 0.5s ease 0.4s both;
}

.advice-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 14px;

  svg { color: #F59E0B; }
}

.advice-text {
  font-size: 0.9rem;
  color: #CBD5E1;
  line-height: 1.7;
  margin-bottom: 18px;
}

.advice-strong {
  color: $accent-primary;
  font-weight: 600;
}

.advice-paths {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.advice-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  background: rgba($accent-secondary, 0.03);
  border: 1px solid rgba($accent-secondary, 0.08);
  border-radius: 10px;
}

.advice-num {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($accent-primary, 0.1);
  color: $accent-primary;
  border-radius: 50%;
  font-size: 0.8rem;
  font-weight: 700;
  flex-shrink: 0;
}

.advice-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.advice-name {
  font-size: 0.9rem;
  font-weight: 500;
  color: $text-primary;
}

.advice-course {
  font-size: 0.8rem;
  color: $accent-primary;
}

/* 底部操作 */
.bottom-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  animation: fadeInUp 0.5s ease 0.5s both;
}

.btn-bottom {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  background: transparent;
  border: 1px solid rgba($accent-secondary, 0.15);
  border-radius: 10px;
  color: $text-secondary;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba($accent-primary, 0.25);
    color: $accent-primary;
    background: rgba($accent-primary, 0.04);
  }

  &.primary {
    background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(0, 85, 255, 0.1));
    border-color: rgba($accent-primary, 0.25);
    color: $accent-primary;
    font-weight: 600;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 0 24px rgba($accent-primary, 0.15);
    }
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .detail-header {
    padding: 16px 20px;
    flex-direction: column;
    gap: 12px;
  }
  .header-left {
    width: 100%;
  }
  .btn-action {
    width: 100%;
    justify-content: center;
  }
  .detail-content {
    padding: 20px;
  }
  .weakness-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .weakness-score {
    text-align: left;
  }
  .weakness-actions {
    flex-direction: column;
    align-items: stretch;
  }
  .btn-start, .btn-resolve {
    justify-content: center;
  }
  .bottom-actions {
    flex-direction: column;
    align-items: stretch;
  }
  .btn-bottom {
    justify-content: center;
  }
  .resource-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }
}
</style>
