<template>
  <div class="history-page">
    <!-- 顶部导航栏 -->
    <header class="history-header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <ArrowLeft :size="20" />
          <span>返回</span>
        </button>
        <h1 class="header-title">
          <History :size="24" class="header-icon" />
          自适应调整历史
        </h1>
      </div>
      <div v-if="total > 0" class="header-total">
        共 <span class="total-num">{{ total }}</span> 次调整
      </div>
    </header>

    <div class="history-content">
      <!-- 加载失败重试 -->
      <div v-if="loadError" class="load-error">
        <span>⚠️ 加载调整历史失败，请检查网络后重试</span>
        <button class="retry-btn" @click="loadData">🔄 重试</button>
      </div>

      <!-- 类型筛选 -->
      <div class="filter-bar">
        <button
          v-for="opt in typeOptions"
          :key="opt.value"
          class="filter-btn"
          :class="{ active: currentType === opt.value }"
          @click="switchType(opt.value)"
        >
{{ opt.label }}
</button>
      </div>

      <!-- 调整时间线 -->
      <section class="info-section">
        <div v-if="records.length === 0 && !loadError" class="chart-empty">
          <History :size="40" class="empty-icon" />
          <p>暂无调整记录，系统检测到学习偏差后会在此展示真实的自适应调整轨迹</p>
        </div>
        <div v-else class="adjust-timeline">
          <div v-for="item in records" :key="item.id" class="adjust-entry">
            <div class="adjust-entry-dot" :class="item.type"></div>
            <div class="adjust-entry-content">
              <div class="adjust-entry-header">
                <span class="adjust-entry-date">{{ formatDateTime(item.createdAt) }}</span>
                <span class="adjust-entry-tag" :class="item.type">{{ item.typeLabel }}</span>
                <span v-if="item.pathName" class="adjust-entry-path">{{ item.pathName }}</span>
              </div>
              <p class="adjust-entry-desc">{{ item.triggerReason }}</p>
              <div v-if="detailText(item) || item.effect" class="adjust-entry-meta">
                <span v-if="detailText(item)" class="meta-chip">🔍 {{ detailText(item) }}</span>
                <span v-if="item.effect" class="meta-chip effect">📈 {{ item.effect }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="totalPages > 1" class="pagination">
          <button class="page-btn" :disabled="page === 0" @click="switchPage(page - 1)">‹ 上一页</button>
          <span class="page-info">{{ page + 1 }} / {{ totalPages }}</span>
          <button class="page-btn" :disabled="page >= totalPages - 1" @click="switchPage(page + 1)">下一页 ›</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, History } from 'lucide-vue-next'
import { getAdaptiveAdjustments } from '@/api/adaptiveApi'

const router = useRouter()

const typeOptions = [
  { value: 'all', label: '全部' },
  { value: 'review_insert', label: '复习插入' },
  { value: 'advance_recommend', label: '进阶推荐' },
  { value: 'plan_adjust', label: '计划调整' },
  { value: 'resource_recommend', label: '资源推荐' },
  { value: 'difficulty_adjust', label: '难度调整' }
]

const records = ref([])
const total = ref(0)
const page = ref(0)
const totalPages = ref(0)
const currentType = ref('all')
const loadError = ref(false)
const PAGE_SIZE = 10

const loadData = async () => {
  loadError.value = false
  try {
    const res = await getAdaptiveAdjustments({
      page: page.value,
      size: PAGE_SIZE,
      type: currentType.value
    })
    const data = res?.data || {}
    records.value = data.content || []
    total.value = data.totalElements || 0
    totalPages.value = data.totalPages || 0
  } catch (e) {
    loadError.value = true
  }
}

const switchType = (type) => {
  currentType.value = type
  page.value = 0
  loadData()
}

const switchPage = (p) => {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  loadData()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 调整详情摘要：优先展示 detail 中的关键字段
const detailText = (item) => {
  const d = item.detail || {}
  if (d.subject) return `科目：${d.subject}`
  if (d.pathName) return `路径：${d.pathName}`
  if (d.latestScore !== undefined && d.previousScore !== undefined) {
    return `得分 ${d.previousScore} → ${d.latestScore}`
  }
  if (d.completionRate !== undefined) return `完成率：${d.completionRate}%`
  return ''
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const goBack = () => {
  router.push('/capability/adaptive')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
@use '../styles/variables' as *;
.history-page {
  min-height: 100vh;
  background: $bg-primary;
  padding-bottom: 80px;
}

.history-header {
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
  color: $accent-primary;
}

.header-total {
  font-size: 0.9rem;
  color: $text-secondary;
}

.total-num {
  color: $accent-primary;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

.history-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-btn {
  padding: 8px 18px;
  background: rgba($accent-secondary, 0.04);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 20px;
  color: $text-secondary;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.filter-btn:hover {
  color: $accent-primary;
  border-color: rgba($accent-primary, 0.2);
}

.filter-btn.active {
  background: rgba($accent-primary, 0.1);
  color: $accent-primary;
  border-color: rgba($accent-primary, 0.3);
}

/* 时间线 */
.info-section {
  background: rgba($bg-primary, 0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba($accent-secondary, 0.12);
  border-radius: 16px;
  padding: 28px;
}

.adjust-timeline {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-left: 20px;
}

.adjust-timeline::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 8px;
  bottom: 8px;
  width: 1px;
  background: rgba($accent-secondary, 0.12);
}

.adjust-entry {
  display: flex;
  gap: 16px;
  position: relative;
}

.adjust-entry-dot {
  position: absolute;
  left: -20px;
  top: 4px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid;
  background: rgba($bg-primary, 0.8);
  z-index: 1;
}

.adjust-entry-dot.review_insert { border-color: #EF4444; }
.adjust-entry-dot.advance_recommend { border-color: $accent-primary; }
.adjust-entry-dot.plan_adjust { border-color: #F59E0B; }
.adjust-entry-dot.resource_recommend { border-color: #A855F7; }
.adjust-entry-dot.difficulty_adjust { border-color: #10B981; }

.adjust-entry-content {
  flex: 1;
}

.adjust-entry-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.adjust-entry-date {
  font-size: 0.8rem;
  color: $text-secondary;
  font-family: 'JetBrains Mono', monospace;
}

.adjust-entry-tag {
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.adjust-entry-tag.review_insert { background: rgba(239, 68, 68, 0.1); color: #EF4444; }
.adjust-entry-tag.advance_recommend { background: rgba($accent-primary, 0.1); color: $accent-primary; }
.adjust-entry-tag.plan_adjust { background: rgba(245, 158, 11, 0.1); color: #F59E0B; }
.adjust-entry-tag.resource_recommend { background: rgba(168, 85, 247, 0.1); color: #A855F7; }
.adjust-entry-tag.difficulty_adjust { background: rgba(16, 185, 129, 0.1); color: #10B981; }

.adjust-entry-path {
  font-size: 0.75rem;
  color: #a0a0c8;
  background: rgba($accent-secondary, 0.06);
  padding: 2px 10px;
  border-radius: 10px;
}

.adjust-entry-desc {
  font-size: 0.9rem;
  color: $text-primary;
  margin: 0 0 6px;
}

.adjust-entry-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-chip {
  font-size: 0.75rem;
  padding: 3px 10px;
  border-radius: 10px;
  background: rgba($accent-secondary, 0.06);
  border: 1px solid rgba($accent-secondary, 0.1);
  color: $text-secondary;
}

.meta-chip.effect {
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.15);
  color: #10B981;
}

/* 空态 / 错误 */
.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  color: rgba($accent-secondary, 0.4);
}

.chart-empty p {
  font-size: 0.9rem;
  color: $text-secondary;
  margin: 0;
  max-width: 420px;
  line-height: 1.6;
}

.load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 12px;
  color: #EF4444;
  font-size: 0.9rem;
}

.retry-btn {
  padding: 6px 14px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.25);
  border-radius: 8px;
  color: #EF4444;
  font-size: 0.85rem;
  cursor: pointer;
  white-space: nowrap;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba($accent-secondary, 0.08);
}

.page-btn {
  padding: 7px 16px;
  background: rgba($accent-primary, 0.06);
  border: 1px solid rgba($accent-primary, 0.15);
  border-radius: 8px;
  color: $accent-primary;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.page-btn:hover:not(:disabled) {
  background: rgba($accent-primary, 0.12);
  border-color: rgba($accent-primary, 0.3);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 0.85rem;
  color: $text-secondary;
  font-family: 'JetBrains Mono', monospace;
}

@media (max-width: 768px) {
  .history-header {
    padding: 16px 20px;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .history-content {
    padding: 20px;
  }
  .info-section {
    padding: 20px;
  }
}
</style>
