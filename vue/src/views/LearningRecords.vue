<template>
  <div class="records-page">
    <!-- 顶部导航栏 -->
    <header class="records-header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <ArrowLeft :size="20" />
          <span>返回</span>
        </button>
        <h1 class="header-title">
          <ListTodo :size="24" class="header-icon" />
          学习记录
        </h1>
      </div>
      <span class="header-total">共 {{ total }} 条记录</span>
    </header>

    <div class="records-content">
      <!-- 筛选区 -->
      <div class="filter-bar">
        <div class="filter-group">
          <select v-model="filters.status" class="filter-select" @change="reload">
            <option value="all">全部状态</option>
            <option value="completed">✅ 已完成</option>
            <option value="in_progress">⏳ 进行中</option>
            <option value="pending">📌 未完成</option>
            <option value="skipped">⏭️ 已跳过</option>
          </select>
          <input v-model="filters.startDate" type="date" class="filter-date" @change="reload" />
          <span class="date-sep">~</span>
          <input v-model="filters.endDate" type="date" class="filter-date" @change="reload" />
          <input
            v-model="filters.keyword"
            class="filter-search"
            placeholder="🔍 搜索任务名称..."
            @keyup.enter="reload"
          />
          <button class="filter-btn" @click="reload">搜索</button>
          <button class="filter-btn ghost" @click="resetFilters">重置</button>
        </div>
      </div>

      <!-- 加载失败 -->
      <div v-if="loadError" class="load-error">
        <span>⚠️ 加载学习记录失败，请检查网络后重试</span>
        <button class="retry-btn" @click="loadData">🔄 重试</button>
      </div>

      <!-- 空态 -->
      <div v-else-if="records.length === 0" class="empty-state">
        <ListTodo :size="48" class="empty-icon" />
        <p v-if="hasFilter">{{ '没有符合筛选条件的学习记录，试试调整筛选条件' }}</p>
        <p v-else>完成第一个学习任务后，这里将显示你的学习记录</p>
      </div>

      <!-- 记录列表 -->
      <div v-else class="records-list">
        <div v-for="record in records" :key="record.id" class="record-item">
          <div class="record-date">
            <span class="record-day">{{ formatDate(record.date) }}</span>
          </div>
          <div class="record-type" :class="record.source === 'record' ? 'learn' : 'task'">
            {{ record.source === 'record' ? '📖' : typeIcon(record.type) }}
          </div>
          <div class="record-info">
            <span class="record-title">{{ record.title }}</span>
            <span class="record-meta">
              <span class="record-duration">⏱ {{ record.duration }}min</span>
              <span v-if="record.pathId" class="record-path-link" @click.stop="viewPath(record.pathId)">查看路径 →</span>
            </span>
          </div>
          <div class="record-status" :class="record.status">
            <span v-if="record.status === 'completed'">✅ 已完成</span>
            <span v-else-if="record.status === 'skipped'">⏭️ 已跳过</span>
            <span v-else-if="record.status === 'in_progress'">⏳ 进行中</span>
            <span v-else>📌 未完成</span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" :disabled="page <= 1" @click="changePage(page - 1)">←</button>
        <span class="page-info">{{ page }} / {{ totalPages }}</span>
        <button class="page-btn" :disabled="page >= totalPages" @click="changePage(page + 1)">→</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ListTodo } from 'lucide-vue-next'
import { getProgressRecords } from '@/api/statsApi'

const router = useRouter()
const records = ref([])
const total = ref(0)
const totalPages = ref(0)
const page = ref(1)
const loadError = ref(false)

const filters = reactive({
  status: 'all',
  keyword: '',
  startDate: '',
  endDate: ''
})

const hasFilter = computed(() => {
  return filters.status !== 'all' || filters.keyword.trim() !== '' || filters.startDate || filters.endDate
})

const goBack = () => {
  router.push('/capability/progress')
}

const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const typeIcon = (type) => {
  const map = { read: '📄', video: '🎬', practice: '✏️', review: '🔁', learn: '📖' }
  return map[type] || '📋'
}

const viewPath = (pathId) => {
  router.push(`/learning-path/${pathId}`)
}

const loadData = async () => {
  loadError.value = false
  try {
    const res = await getProgressRecords({
      page: page.value,
      size: 10,
      status: filters.status,
      keyword: filters.keyword.trim(),
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined
    })
    const d = res?.data || {}
    records.value = (d.records || []).map(r => ({
      id: r.id,
      date: r.date,
      title: r.title,
      duration: r.duration || 0,
      status: r.status || 'pending',
      type: r.type || '',
      pathId: r.pathId || '',
      source: r.source || 'task'
    }))
    total.value = d.total || 0
    totalPages.value = d.totalPages || 1
  } catch (e) {
    records.value = []
    total.value = 0
    totalPages.value = 0
    loadError.value = true
  }
}

const reload = () => {
  page.value = 1
  loadData()
}

const changePage = (p) => {
  page.value = p
  loadData()
}

const resetFilters = () => {
  filters.status = 'all'
  filters.keyword = ''
  filters.startDate = ''
  filters.endDate = ''
  reload()
}

loadData()
</script>

<style scoped>
.records-page {
  min-height: 100vh;
  background: #0a0a1a;
  padding-bottom: 80px;
}

.records-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 48px;
  background: rgba(17, 17, 39, 0.8);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(100, 100, 180, 0.12);
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
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 8px;
  color: #94A3B8;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-back:hover {
  color: #00E5FF;
  border-color: rgba(0, 229, 255, 0.2);
  background: rgba(0, 229, 255, 0.04);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.4rem;
  font-weight: 700;
  color: #F1F5F9;
  margin: 0;
}

.header-icon {
  color: #00E5FF;
}

.header-total {
  font-size: 0.85rem;
  color: #94A3B8;
  font-family: 'JetBrains Mono', monospace;
}

.records-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 筛选区 */
.filter-bar {
  background: rgba(17, 17, 39, 0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 16px;
  padding: 20px 24px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-select,
.filter-date,
.filter-search {
  padding: 8px 12px;
  background: rgba(30, 38, 56, 0.6);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 8px;
  color: #C0C0E0;
  font-size: 0.85rem;
  font-family: inherit;
  outline: none;
  transition: all 0.2s;
}

.filter-select:hover,
.filter-date:hover,
.filter-search:hover {
  border-color: rgba(0, 229, 255, 0.3);
}

.filter-search {
  flex: 1;
  min-width: 180px;
}

.filter-search::placeholder {
  color: #64748B;
}

.filter-date {
  color-scheme: dark;
}

.date-sep {
  color: #64748B;
  font-size: 0.85rem;
}

.filter-btn {
  padding: 8px 18px;
  background: rgba(0, 229, 255, 0.08);
  border: 1px solid rgba(0, 229, 255, 0.2);
  border-radius: 8px;
  color: #00E5FF;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
}

.filter-btn:hover {
  background: rgba(0, 229, 255, 0.14);
  border-color: rgba(0, 229, 255, 0.35);
}

.filter-btn.ghost {
  background: transparent;
  border-color: rgba(100, 100, 180, 0.2);
  color: #94A3B8;
}

.filter-btn.ghost:hover {
  color: #E2E8F0;
  border-color: rgba(100, 100, 180, 0.35);
}

/* 空态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 72px 20px;
  color: #64748B;
  font-size: 0.9rem;
  text-align: center;
  background: rgba(17, 17, 39, 0.6);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 16px;
}

.empty-icon {
  opacity: 0.4;
}

/* 加载失败 */
.load-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 12px;
  color: #FCA5A5;
  font-size: 0.9rem;
}

.retry-btn {
  padding: 6px 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  color: #FCA5A5;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
}

.retry-btn:hover {
  background: rgba(239, 68, 68, 0.18);
}

/* 记录列表 */
.records-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  background: rgba(17, 17, 39, 0.6);
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 12px;
  transition: all 0.25s ease;
}

.record-item:hover {
  background: rgba(0, 229, 255, 0.02);
  border-color: rgba(0, 229, 255, 0.15);
  transform: translateX(4px);
}

.record-date {
  min-width: 96px;
}

.record-day {
  font-size: 0.82rem;
  color: #94A3B8;
  font-family: 'JetBrains Mono', monospace;
}

.record-type {
  font-size: 1.1rem;
  flex-shrink: 0;
}

.record-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.record-title {
  font-size: 0.92rem;
  font-weight: 500;
  color: #F1F5F9;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-meta {
  display: flex;
  align-items: center;
  gap: 14px;
}

.record-duration {
  font-size: 0.8rem;
  color: #94A3B8;
}

.record-path-link {
  font-size: 0.78rem;
  color: #00E5FF;
  cursor: pointer;
}

.record-path-link:hover {
  text-decoration: underline;
}

.record-status {
  font-size: 0.82rem;
  font-weight: 500;
  flex-shrink: 0;
}

.record-status.completed {
  color: #10B981;
}

.record-status.skipped {
  color: #94A3B8;
}

.record-status.in_progress {
  color: #F59E0B;
}

.record-status.pending {
  color: #64748B;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px 0;
}

.page-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.15);
  border-radius: 8px;
  color: #94A3B8;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.page-btn:hover:not(:disabled) {
  color: #00E5FF;
  border-color: rgba(0, 229, 255, 0.3);
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-info {
  font-size: 0.85rem;
  color: #94A3B8;
  font-family: 'JetBrains Mono', monospace;
}

@media (max-width: 768px) {
  .records-header {
    padding: 16px 20px;
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  .records-content { padding: 20px; }
  .record-item { flex-wrap: wrap; }
  .record-status { width: 100%; padding-left: 112px; }
}
</style>
