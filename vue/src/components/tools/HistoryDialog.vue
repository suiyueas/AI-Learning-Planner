<template>
  <el-dialog
    v-model="visible"
    title="📜 测评历史记录"
    width="720px"
    :close-on-click-modal="true"
    class="history-dialog"
  >
    <div v-loading="loading">
      <div class="history-stats">
        <span>共 <strong>{{ total }}</strong> 条记录</span>
        <el-select v-if="subjectOptions.length > 0" v-model="filterSubject" placeholder="筛选科目" clearable size="small" class="subject-filter">
          <el-option v-for="s in subjectOptions" :key="s" :label="s" :value="s" />
        </el-select>
      </div>

      <div v-if="records.length === 0" class="empty-history">
        <span class="empty-icon">📋</span>
        <p>暂无测评记录</p>
        <p class="empty-tip">开始一次测评后会显示在这里</p>
      </div>

      <div v-else class="history-list">
        <div
          v-for="record in records"
          :key="record.id"
          class="history-item"
          @click="viewDetail(record)"
        >
          <div class="history-info">
            <span class="history-subject">{{ record.subject }}</span>
            <span class="history-score">{{ record.score }} / {{ record.total }}</span>
          </div>
          <div class="history-meta">
            <span class="history-date">{{ formatDate(record.createdAt) }}</span>
            <span class="history-difficulty" :class="'diff-' + record.difficulty">{{ getDifficultyLabel(record.difficulty) }}</span>
            <span class="history-status" :class="getStatusClass(record)">
              {{ getStatusText(record) }}
            </span>
          </div>
          <el-button
            class="history-delete-btn"
            type="danger"
            size="small"
            circle
            title="删除记录"
            @click.stop="handleDelete(record)"
          >
✕
</el-button>
          <span class="history-arrow">›</span>
        </div>
      </div>

      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="page"
        :total="total"
        :page-size="pageSize"
        :page-count="5"
        layout="prev, pager, next"
        class="history-pagination"
        @current-change="loadHistory"
      />
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="detailVisible"
    :title="`📋 测评详情 - ${detailData?.subject || ''}`"
    width="680px"
    :close-on-click-modal="true"
  >
    <div v-if="detailData" class="detail-content">
      <div class="detail-header">
        <div class="detail-summary">
          <span class="detail-score" :style="{ color: scoreColor }">{{ detailData.score }}</span>
          <span class="detail-total">/ {{ detailData.total }}</span>
          <span class="detail-accuracy">{{ detailData.accuracy }}% 正确率</span>
        </div>
        <span class="detail-date">{{ formatDate(detailData.createdAt) }}</span>
      </div>

      <div class="detail-questions">
        <div v-for="(q, idx) in detailData.details" :key="idx" class="detail-question-item" :class="{ correct: q.correct, wrong: !q.correct }">
          <div class="dq-header">
            <span class="dq-number">{{ idx + 1 }}</span>
            <span class="dq-badge" :class="q.correct ? 'badge-correct' : 'badge-wrong'">{{ q.correct ? '✅ 正确' : '❌ 错误' }}</span>
          </div>
          <p class="dq-text">{{ q.questionText }}</p>
          <div class="dq-options">
            <div
v-for="(opt, j) in q.options" :key="j" class="dq-option" :class="{
              'dq-correct': j === q.correctAnswer,
              'dq-user-wrong': j === q.userAnswer && j !== q.correctAnswer
            }"
>
              <span class="dq-opt-letter">{{ ['A','B','C','D'][j] }}</span>
              <span class="dq-opt-text">{{ opt }}</span>
              <span v-if="j === q.correctAnswer" class="dq-mark">✓ 正确答案</span>
              <span v-if="j === q.userAnswer && j !== q.correctAnswer" class="dq-mark">✗ 你的选择</span>
            </div>
          </div>
          <div v-if="q.explanation" class="dq-explanation">
            <span class="dq-exp-icon">💡</span>
            <span>{{ q.explanation }}</span>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 删除记录确认对话框 -->
  <DeleteConfirmDialog
    :visible="showDeleteDialog"
    title="删除记录"
    :message="deleteDialogMessage"
    type="warning"
    :show-soft-delete="false"
    @cancel="showDeleteDialog = false"
    @hard-delete="confirmDeleteRecord"
  />
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { getHistory, getHistoryDetail, deleteHistory } from '@/api/assessmentApi'
import { ElMessage } from 'element-plus'
import DeleteConfirmDialog from '@/components/common/DeleteConfirmDialog.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const records = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const filterSubject = ref('')
const subjectOptions = ref([])

const detailVisible = ref(false)
const detailData = ref(null)
const showDeleteDialog = ref(false)
const deletingRecord = ref(null)
const deleteDialogMessage = ref('')

const scoreColor = computed(() => {
  const s = detailData.value?.accuracy || 0
  return s >= 80 ? '#00f5d4' : s >= 60 ? '#f59e0b' : '#ef4444'
})

watch(filterSubject, () => {
  page.value = 1
  loadHistory()
})

async function loadHistory() {
  loading.value = true
  try {
    const res = await getHistory(page.value, pageSize.value, filterSubject.value || undefined)
    const data = res?.data || res || {}
    records.value = data.records || []
    total.value = data.total || 0
    if (page.value === 1 && records.value.length > 0) {
      subjectOptions.value = [...new Set(records.value.map(r => r.subject))]
    }
  } catch (e) {
    console.error('加载历史记录失败:', e)
    records.value = []
  } finally {
    loading.value = false
  }
}

async function viewDetail(record) {
  try {
    const res = await getHistoryDetail(record.id)
    detailData.value = res?.data || res
    detailVisible.value = true
  } catch (e) {
    console.error('加载详情失败:', e)
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function getDifficultyLabel(diff) {
  const map = { easy: '简单', medium: '中等', hard: '困难' }
  return map[diff] || diff || '中等'
}

function getStatusClass(record) {
  const rate = record.total > 0 ? record.score / record.total : 0
  if (rate >= 0.8) return 'status-mastered'
  if (rate >= 0.6) return 'status-progress'
  return 'status-weak'
}

function getStatusText(record) {
  const rate = record.total > 0 ? record.score / record.total : 0
  if (rate >= 0.8) return '已掌握'
  if (rate >= 0.6) return '进步中'
  return '需加强'
}

watch(visible, (val) => {
  if (val) {
    page.value = 1
    filterSubject.value = ''
    loadHistory()
  }
})

async function handleDelete(record) {
  deletingRecord.value = record
  deleteDialogMessage.value = `确定删除这条 ${record.subject} 测评记录吗？`
  showDeleteDialog.value = true
}

async function confirmDeleteRecord() {
  try {
    await deleteHistory(deletingRecord.value.id)
    ElMessage.success('删除成功')
    loadHistory()
  } catch (e) {
    console.error('删除失败:', e)
    ElMessage.error(e?.response?.data?.message || '删除失败')
  } finally {
    showDeleteDialog.value = false
    deletingRecord.value = null
  }
}
</script>

<style scoped>
:deep(.history-dialog .el-dialog) {
  background: rgba(17, 17, 39, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(100, 100, 180, 0.12);
  border-radius: 16px;
}

:deep(.history-dialog .el-dialog__header) {
  border-bottom: 1px solid rgba(100, 100, 180, 0.08);
  padding: 16px 20px;
}

:deep(.history-dialog .el-dialog__title) {
  font-size: 1.1rem;
  font-weight: 700;
  color: #e8e8ff;
}

:deep(.history-dialog .el-dialog__body) {
  padding: 20px;
}

.history-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 0.85rem;
  color: #c0c0e0;
}

.history-stats strong {
  color: #00f5d4;
}

.subject-filter {
  width: 160px;
}

:deep(.subject-filter .el-input__wrapper) {
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid rgba(100, 100, 180, 0.1);
  border-radius: 8px;
  box-shadow: none;
}

:deep(.subject-filter .el-input__inner) {
  color: #e8e8ff;
  font-size: 0.82rem;
}

.empty-history {
  text-align: center;
  padding: 40px 20px;
  color: #9090b8;
}

.empty-icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-history p {
  margin: 0;
  font-size: 0.9rem;
}

.empty-tip {
  font-size: 0.78rem;
  margin-top: 6px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.history-list::-webkit-scrollbar {
  width: 6px;
}

.history-list::-webkit-scrollbar-track {
  background: rgba(100, 100, 180, 0.05);
  border-radius: 3px;
}

.history-list::-webkit-scrollbar-thumb {
  background: rgba(100, 100, 180, 0.15);
  border-radius: 3px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(100, 100, 180, 0.03);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item:hover {
  border-color: rgba(0, 245, 212, 0.2);
  background: rgba(0, 245, 212, 0.02);
}

.history-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-subject {
  font-size: 0.9rem;
  font-weight: 600;
  color: #e8e8ff;
}

.history-score {
  font-size: 0.82rem;
  color: #00f5d4;
  font-family: 'JetBrains Mono', monospace;
}

.history-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.75rem;
  color: #9090b8;
}

.history-date {
  color: #9090b8;
}

.history-difficulty {
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 0.7rem;
  font-weight: 600;
}

.history-difficulty.diff-easy {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.history-difficulty.diff-medium {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.history-difficulty.diff-hard {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.history-status {
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 0.7rem;
  font-weight: 600;
}

.status-mastered {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.status-progress {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.status-weak {
  background: rgba(255, 0, 110, 0.1);
  color: #ff006e;
}

.history-arrow {
  font-size: 1.2rem;
  color: rgba(100, 100, 180, 0.3);
  transition: all 0.2s;
}

.history-item:hover .history-arrow {
  color: #00f5d4;
  transform: translateX(2px);
}

.history-delete-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  min-width: 28px;
  padding: 0;
  font-size: 11px;
  opacity: 0;
  transition: opacity 0.2s;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.history-item:hover .history-delete-btn {
  opacity: 1;
}

.history-delete-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: #ef4444;
  color: #fff;
}

.history-pagination {
  margin-top: 16px;
  justify-content: center;
}

:deep(.history-pagination .el-pager li) {
  background: transparent;
  color: #c0c0e0;
  border-radius: 8px;
}

:deep(.history-pagination .el-pager li.is-active) {
  background: rgba(0, 245, 212, 0.1);
  color: #00f5d4;
}

:deep(.history-pagination .el-pager li:hover) {
  color: #00f5d4;
}

:deep(.history-pagination .btn-prev),
:deep(.history-pagination .btn-next) {
  background: transparent;
  color: #c0c0e0;
  border-radius: 8px;
}

/* Detail Dialog Styles */
.detail-content {
  max-height: 60vh;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 12px;
  margin-bottom: 20px;
}

.detail-summary {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.detail-score {
  font-size: 2rem;
  font-weight: 900;
  font-family: 'JetBrains Mono', monospace;
}

.detail-total {
  font-size: 1rem;
  color: #9090b8;
}

.detail-accuracy {
  margin-left: 12px;
  padding: 4px 12px;
  background: rgba(0, 245, 212, 0.08);
  border-radius: 12px;
  font-size: 0.82rem;
  color: #00f5d4;
}

.detail-date {
  font-size: 0.82rem;
  color: #9090b8;
}

.detail-questions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-question-item {
  padding: 16px;
  border: 1px solid rgba(100, 100, 180, 0.08);
  border-radius: 12px;
  background: rgba(100, 100, 180, 0.02);
}

.detail-question-item.correct {
  border-color: rgba(16, 185, 129, 0.15);
  background: rgba(16, 185, 129, 0.02);
}

.detail-question-item.wrong {
  border-color: rgba(239, 68, 68, 0.15);
  background: rgba(239, 68, 68, 0.02);
}

.dq-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.dq-number {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(100, 100, 180, 0.1);
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 700;
  color: #9090b8;
  font-family: 'JetBrains Mono', monospace;
}

.dq-badge {
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 0.7rem;
  font-weight: 600;
}

.badge-correct {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.badge-wrong {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.dq-text {
  font-size: 0.88rem;
  color: #e8e8ff;
  margin: 0 0 12px;
  line-height: 1.5;
}

.dq-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

.dq-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 0.8rem;
  color: #c0c0e0;
  background: rgba(100, 100, 180, 0.03);
}

.dq-option.dq-correct {
  background: rgba(16, 185, 129, 0.08);
  color: #10b981;
}

.dq-option.dq-user-wrong {
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
}

.dq-opt-letter {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(100, 100, 180, 0.08);
  border-radius: 6px;
  font-size: 0.7rem;
  font-weight: 700;
  flex-shrink: 0;
}

.dq-opt-text {
  flex: 1;
}

.dq-mark {
  font-size: 0.72rem;
  font-weight: 600;
  margin-left: auto;
}

.dq-explanation {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  background: rgba(123, 97, 255, 0.04);
  border: 1px solid rgba(123, 97, 255, 0.08);
  border-radius: 8px;
  font-size: 0.78rem;
  color: #c0c0e0;
  line-height: 1.5;
}

.dq-exp-icon {
  flex-shrink: 0;
}
</style>