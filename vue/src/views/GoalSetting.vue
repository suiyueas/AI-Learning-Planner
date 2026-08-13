<template>
  <div class="goal-page">
    <div class="bg-layer">
      <div class="bg-aurora">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <div class="bg-grid"></div>
    </div>

    <header class="page-header">
      <button class="back-btn" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" /></svg><span>返回</span>
      </button>
      <h1 class="page-title">
        <span class="title-icon">🎯</span>
        <span class="title-text">目标设定</span>
      </h1>
      <button class="add-btn" @click="openAddModal">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
        <span>新建目标</span>
      </button>
    </header>

    <div class="content">
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-icon">🎯</span>
          <div class="stat-info"><span class="stat-num">{{ goals.length }}</span><span class="stat-label">总目标</span></div>
        </div>
        <div class="stat-card">
          <span class="stat-icon">✅</span>
          <div class="stat-info"><span class="stat-num">{{ completedCount }}</span><span class="stat-label">已完成</span></div>
        </div>
        <div class="stat-card">
          <span class="stat-icon">🔄</span>
          <div class="stat-info"><span class="stat-num">{{ inProgressCount }}</span><span class="stat-label">进行中</span></div>
        </div>
        <div class="stat-card">
          <span class="stat-icon">📊</span>
          <div class="stat-info"><span class="stat-num">{{ avgProgress }}%</span><span class="stat-label">平均进度</span></div>
        </div>
      </div>

      <div class="filter-bar">
        <button v-for="f in filters" :key="f.id" class="filter-btn" :class="{ active: activeFilter === f.id }" @click="activeFilter = f.id">
          <span class="filter-dot" :class="f.color"></span><span>{{ f.label }}</span><span class="filter-count">{{ f.count }}</span>
        </button>
      </div>

      <TransitionGroup name="goal-list" tag="div" class="goal-list">
        <div v-for="goal in filteredGoals" :key="goal.id" class="goal-card" :class="{ expanded: expandedId === goal.id }" @click="toggleExpand(goal.id)">
          <div class="goal-top">
            <span class="goal-category" :style="{ color: goal.color }">{{ goal.category }}</span>
            <span v-if="goal.deadline" class="goal-deadline">📅 周期 {{ goal.deadline }}</span>
            <div class="goal-actions" @click.stop>
              <button class="icon-btn edit" title="编辑" @click="openEditModal(goal)">✏️</button>
              <button class="icon-btn delete" title="删除" @click="confirmDelete(goal)">🗑️</button>
            </div>
          </div>
          <h3 class="goal-title">{{ goal.title }}</h3>
          <p v-if="expandedId === goal.id" class="goal-desc">{{ goal.description }}</p>
          <div class="goal-progress">
            <div class="goal-progress-bar">
              <div class="goal-progress-fill" :style="{ width: goal.progress + '%', background: goal.color }"></div>
            </div>
            <span class="goal-progress-text">{{ goal.progress }}%</span>
          </div>
          <div v-if="expandedId === goal.id && goal.milestones?.length" class="goal-milestones">
            <div v-for="(m, i) in goal.milestones" :key="i" class="milestone-item" :class="{ done: m.done }">
              <span class="milestone-check">{{ m.done ? '✅' : '⬜' }}</span>
              <span class="milestone-text">{{ m.text }}</span>
              <button v-if="!m.done" class="milestone-btn" @click.stop="completeMilestone(goal.id, i)">标记完成</button>
            </div>
          </div>
          <div class="goal-footer">
            <span class="goal-status" :class="goal.status">
              {{ goal.status === 'completed' ? '✅ 已完成' : goal.status === 'in_progress' ? '🔄 进行中' : '📌 待开始' }}
            </span>
            <span class="goal-created">创建于 {{ formatDate(goal.createdAt) }}</span>
          </div>
        </div>
      </TransitionGroup>

      <div v-if="filteredGoals.length === 0" class="empty-state">
        <div class="empty-icon">🎯</div>
        <h3 class="empty-title">{{ goals.length === 0 ? '还没有学习目标' : '没有匹配的目标' }}</h3>
        <p class="empty-desc">{{ goals.length === 0 ? '点击上方「新建目标」开始设定你的学习计划' : '试试其他筛选条件' }}</p>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
          <div class="modal-panel">
            <div class="modal-header">
              <h2 class="modal-title">{{ isEditing ? '✏️ 编辑目标' : '🎯 新建目标' }}</h2>
              <button class="modal-close" @click="closeModal"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-group">
                <label class="form-label">学习目标</label>
                <input ref="titleInput" v-model="form.title" class="form-input" placeholder="例如：学习 Python 数据分析" maxlength="50" :disabled="isEditing" @keyup.enter="saveGoal" />
                <p v-if="isEditing" class="form-tip">目标由学习路径驱动，如需修改目标请删除后重建</p>
              </div>
              <div v-if="!isEditing" class="form-row">
                <div class="form-group flex-1">
                  <label class="form-label">目标领域</label>
                  <select v-model="form.category" class="form-select">
                    <option v-for="c in categories" :key="c.value" :value="c.value">{{ c.label }}</option>
                  </select>
                </div>
                <div class="form-group flex-1">
                  <label class="form-label">学习周期（周）</label>
                  <input v-model.number="form.weeks" type="number" min="4" max="52" class="form-input" />
                </div>
              </div>
              <div class="form-group">
                <div class="form-note">
                  <span class="note-icon">🤖</span>
                  <span class="note-text">保存后将由 AI 自动生成学习大纲（章节 → 周 → 任务），进度、里程碑与学习路径实时同步</span>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn-cancel" @click="closeModal">取消</button>
              <button class="btn-save" :disabled="!form.title.trim() || saving" @click="saveGoal">{{ saving ? '生成中...' : (isEditing ? '保存修改' : '创建目标') }}</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 删除确认 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showDelete" class="modal-overlay" @click.self="showDelete = false">
          <div class="delete-dialog">
            <div class="delete-icon">🗑️</div>
            <h3 class="delete-title">确认删除</h3>
            <p class="delete-desc">确定要删除目标「{{ deletingGoal?.title }}」吗？</p>
            <div class="delete-actions">
              <button class="btn-cancel" @click="showDelete = false">取消</button>
              <button class="btn-delete-confirm" @click="executeDelete">删除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPathList, getPathProgress, generatePath, updateLearningPath, deletePath, completeTask } from '@/api/learningPath'

const router = useRouter()
const goals = ref([])
const loading = ref(false)
const saving = ref(false)
const showModal = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const showDelete = ref(false)
const deletingGoal = ref(null)
const expandedId = ref(null)
const activeFilter = ref('all')
const titleInput = ref(null)

const categories = [
  { value: '编程', label: '💻 编程' }, { value: '算法', label: '🧮 算法' },
  { value: '数据库', label: '🗄️ 数据库' }, { value: '前端', label: '🎨 前端' },
  { value: '系统设计', label: '🏗️ 系统设计' }, { value: '英语', label: '🌍 英语' },
  { value: '数学', label: '📐 数学' }, { value: '其他', label: '📌 其他' }
]

const categoryColors = { '编程': '#00f5d4', '算法': '#7b61ff', '数据库': '#3a86ff', '前端': '#f59e0b', '系统设计': '#ec4899', '英语': '#10b981', '数学': '#ef4444', '其他': '#9090b8' }

const form = ref({ title: '', category: '编程', weeks: 12 })

const filters = computed(() => [
  { id: 'all', label: '全部', count: goals.value.length, color: 'cyan' },
  { id: 'in_progress', label: '进行中', count: goals.value.filter(g => g.status === 'in_progress').length, color: 'blue' },
  { id: 'pending', label: '待开始', count: goals.value.filter(g => g.status === 'pending').length, color: 'gray' },
  { id: 'completed', label: '已完成', count: goals.value.filter(g => g.status === 'completed').length, color: 'green' },
])

const filteredGoals = computed(() => {
  if (activeFilter.value === 'all') return goals.value
  return goals.value.filter(g => g.status === activeFilter.value)
})

const completedCount = computed(() => goals.value.filter(g => g.status === 'completed').length)
const inProgressCount = computed(() => goals.value.filter(g => g.status === 'in_progress').length)
const avgProgress = computed(() => {
  if (!goals.value.length) return 0
  return Math.round(goals.value.reduce((s, g) => s + g.progress, 0) / goals.value.length)
})

// ============= 数据层：目标 = 用户学习路径（后端真实数据，进度来自 learning_record） =============

// 去掉名称中的【领域】前缀
const stripFieldPrefix = (name) => (name || '').replace(/^【[^】]*】/, '')

// 从名称/领域前缀推断目标分类（用于卡片配色与筛选）
const FIELD_MAP = [
  { keys: ['编程', 'python', 'java', '后端', 'c++', 'golang', 'go'], cat: '编程' },
  { keys: ['前端', 'web', 'vue', 'react'], cat: '前端' },
  { keys: ['算法'], cat: '算法' },
  { keys: ['数据库', 'sql', 'mysql'], cat: '数据库' },
  { keys: ['英语'], cat: '英语' },
  { keys: ['数学'], cat: '数学' },
  { keys: ['系统设计'], cat: '系统设计' }
]
const inferCategory = (name) => {
  const prefix = (name || '').match(/^【([^】]+)】/)
  if (prefix) {
    const direct = categories.find(c => prefix[1].includes(c.value) || c.value.includes(prefix[1]))
    if (direct) return direct.value
    const matched = FIELD_MAP.find(x => x.keys.some(k => prefix[1].toLowerCase().includes(k)))
    return matched ? matched.cat : '其他'
  }
  const lower = (name || '').toLowerCase()
  const matched = FIELD_MAP.find(x => x.keys.some(k => lower.includes(k)))
  return matched ? matched.cat : '其他'
}

// 路径实体 + 进度 → 目标卡片数据
const buildGoal = (path, pd) => {
  const progress = pd?.progress ?? Math.round(path.completionPercentage || 0)
  const status = progress >= 100 ? 'completed' : progress > 0 ? 'in_progress' : 'pending'
  // 里程碑：由进度接口的阶段 → 周 → 任务拍平而来，勾选即调用真实完成接口
  const milestones = []
  pd?.phases?.forEach(ph => ph.weeks?.forEach(w => w.tasks?.forEach(t => {
    milestones.push({ id: t.id, text: t.title, done: t.status === 'completed' })
  })))
  // 周期：从动态描述中解析（如“周期：3个月”），无则取节点数估算
  const periodMatch = (path.description || '').match(/周期：([^|]+)/)
  const category = inferCategory(path.name)
  return {
    id: path.id,
    title: stripFieldPrefix(path.name) || '未命名目标',
    description: path.description || '暂无描述',
    category,
    deadline: periodMatch ? periodMatch[1].trim() : (milestones.length ? milestones.length + ' 个任务' : ''),
    progress,
    status,
    createdAt: pd?.createdAt || path.createdAt || '',
    color: categoryColors[category] || '#00f5d4',
    milestones
  }
}

const fetchGoals = async () => {
  loading.value = true
  try {
    const res = await getPathList()
    const list = res?.data ?? res
    const paths = Array.isArray(list) ? list : []
    // 并行拉取每条路径的真实进度（与列表页/详情页同一接口口径）
    const goalList = await Promise.all(paths.map(async (p) => {
      try {
        const pr = await getPathProgress(p.id)
        return buildGoal(p, pr?.data ?? pr)
      } catch (e) {
        return buildGoal(p, null)
      }
    }))
    goals.value = goalList
  } catch (e) {
    console.error('加载学习目标失败:', e)
    ElMessage.error('加载学习目标失败')
    goals.value = []
  } finally {
    loading.value = false
  }
}

function openAddModal() {
  isEditing.value = false
  editingId.value = null
  form.value = { title: '', category: '编程', weeks: 12 }
  showModal.value = true
  nextTick(() => titleInput.value?.focus())
}

function openEditModal(goal) {
  isEditing.value = true
  editingId.value = goal.id
  form.value = { title: goal.title, category: goal.category, weeks: 12 }
  showModal.value = true
  nextTick(() => titleInput.value?.focus())
}

function closeModal() { showModal.value = false; isEditing.value = false; editingId.value = null }

// 新建目标 = 后端生成学习路径（含 AI/模板大纲）；编辑 = 更新路径名称
async function saveGoal() {
  if (!form.value.title.trim() || saving.value) return
  saving.value = true
  try {
    if (isEditing.value && editingId.value) {
      await updateLearningPath(editingId.value, { name: form.value.title.trim() })
      ElMessage.success('目标已更新')
    } else {
      await generatePath({
        goal: form.value.title.trim(),
        targetField: form.value.category === '其他' ? '' : form.value.category,
        durationWeeks: Math.max(4, form.value.weeks || 12)
      })
      ElMessage.success('目标创建成功，学习大纲已自动生成 🎉')
    }
    closeModal()
    await fetchGoals()
  } catch (e) {
    console.error('保存目标失败:', e)
    ElMessage.error('保存失败：' + (e?.response?.data?.message || e?.message || '请稍后重试'))
  } finally {
    saving.value = false
  }
}

function confirmDelete(goal) { deletingGoal.value = goal; showDelete.value = true }

async function executeDelete() {
  if (!deletingGoal.value) return
  try {
    await deletePath(deletingGoal.value.id)
    ElMessage.success('目标已删除')
    showDelete.value = false
    deletingGoal.value = null
    await fetchGoals()
  } catch (e) {
    console.error('删除目标失败:', e)
    ElMessage.error('删除失败：' + (e?.response?.data?.message || e?.message || '请稍后重试'))
  }
}

function toggleExpand(id) { expandedId.value = expandedId.value === id ? null : id }

// 标记任务完成 = 真实写入学习记录（与学习路径详情页同一接口）
async function completeMilestone(goalId, idx) {
  const goal = goals.value.find(g => g.id === goalId)
  const task = goal?.milestones?.[idx]
  if (!goal || !task || task.done) return
  try {
    await completeTask(goalId, task.id)
    await fetchGoals()
  } catch (e) {
    console.error('标记任务完成失败:', e)
    ElMessage.error('操作失败：' + (e?.response?.data?.message || e?.message || '请稍后重试'))
  }
}

function formatDate(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  if (isNaN(d.getTime())) return String(ts).slice(0, 10)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function goBack() { if (window.history.length > 1) router.back(); else router.push('/modules') }

onMounted(fetchGoals)
</script>

<style lang="scss" scoped>
.goal-page { min-height: calc(100vh - 68px); background: #0a0a1a; position: relative; }
.bg-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-aurora { position: absolute; inset: 0;
  background: radial-gradient(ellipse at 70% 20%, rgba(0,245,212,0.06) 0%, transparent 50%), radial-gradient(ellipse at 30% 80%, rgba(123,97,255,0.05) 0%, transparent 50%), radial-gradient(ellipse at 50% 50%, rgba(0,85,255,0.04) 0%, transparent 50%);
  animation: auroraDrift 20s ease-in-out infinite;
}
@keyframes auroraDrift { 0%,100% { transform: scale(1) rotate(0deg); } 33% { transform: scale(1.08) rotate(0.8deg); } 66% { transform: scale(0.95) rotate(-0.6deg); } }
.bg-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(0,245,212,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(123,97,255,0.03) 1px, transparent 1px); background-size: 40px 40px; animation: gridPulse 8s ease-in-out infinite alternate; }
@keyframes gridPulse { 0% { opacity: 0.3; } 100% { opacity: 0.6; } }

.page-header { position: sticky; top: 0; z-index: 10; display: flex; align-items: center; gap: 16px; padding: 16px 32px; background: rgba(10,10,26,0.85); backdrop-filter: blur(20px); border-bottom: 1px solid rgba(100,100,180,0.08); }
.back-btn { display: flex; align-items: center; gap: 6px; padding: 8px 14px; background: rgba(100,100,180,0.06); border: 1px solid rgba(100,100,180,0.1); border-radius: 8px; color: #c0c0e0; font-size: 0.82rem; cursor: pointer; transition: all 0.25s ease; &:hover { border-color: rgba(0,245,212,0.2); color: #00f5d4; } }
.page-title { flex: 1; display: flex; align-items: center; gap: 10px; }
.title-icon { font-size: 1.3rem; }
.title-text { font-size: 1.05rem; font-weight: 700; background: linear-gradient(135deg, #f59e0b, #ef4444); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.add-btn { display: flex; align-items: center; gap: 6px; padding: 9px 18px; background: linear-gradient(135deg, rgba(245,158,11,0.15), rgba(239,68,68,0.1)); border: 1px solid rgba(245,158,11,0.2); border-radius: 10px; color: #f59e0b; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.25s ease; &:hover { transform: translateY(-1px); box-shadow: 0 0 20px rgba(245,158,11,0.15); } }

.content { max-width: 900px; margin: 0 auto; padding: 24px 32px 60px; position: relative; z-index: 1; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.stat-card { display: flex; align-items: center; gap: 12px; padding: 14px 18px; background: rgba(17,17,39,0.6); backdrop-filter: blur(12px); border: 1px solid rgba(100,100,180,0.1); border-radius: 12px; .stat-icon { font-size: 1.3rem; } .stat-num { font-size: 1.4rem; font-weight: 800; color: #f59e0b; font-family: 'JetBrains Mono', monospace; } .stat-label { font-size: 0.7rem; color: #9090b8; } .stat-info { display: flex; flex-direction: column; gap: 2px; } }

.filter-bar { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.filter-btn { display: flex; align-items: center; gap: 6px; padding: 6px 14px; background: rgba(17,17,39,0.6); border: 1px solid rgba(100,100,180,0.1); border-radius: 20px; font-size: 13px; color: #c0c0e0; cursor: pointer; transition: all 0.2s; &.active { border-color: #f59e0b; color: #f59e0b; background: rgba(245,158,11,0.08); } }
.filter-dot { width: 8px; height: 8px; border-radius: 50%; &.cyan { background: #00f5d4; } &.blue { background: #3a86ff; } &.gray { background: #9090b8; } &.green { background: #10b981; } }
.filter-count { font-size: 11px; padding: 0 5px; background: rgba(100,100,180,0.1); border-radius: 8px; }

.goal-list { display: flex; flex-direction: column; gap: 12px; }
.goal-card { padding: 18px 20px; background: rgba(17,17,39,0.5); backdrop-filter: blur(12px); border: 1px solid rgba(100,100,180,0.08); border-radius: 14px; cursor: pointer; transition: all 0.3s ease; &:hover { transform: translateY(-2px); border-color: rgba(245,158,11,0.12); } &.expanded { border-color: rgba(245,158,11,0.2); } }
.goal-top { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.goal-category { font-size: 0.72rem; font-weight: 600; padding: 2px 10px; border-radius: 10px; background: rgba(100,100,180,0.06); }
.goal-deadline { font-size: 0.7rem; color: #9090b8; }
.goal-actions { margin-left: auto; display: flex; gap: 4px; opacity: 0; transition: opacity 0.2s; .goal-card:hover & { opacity: 1; } }
.icon-btn { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; border-radius: 6px; cursor: pointer; font-size: 0.8rem; transition: all 0.15s; &.edit:hover { background: rgba(0,245,212,0.08); } &.delete:hover { background: rgba(239,68,68,0.08); } }
.goal-title { font-size: 1rem; font-weight: 700; color: #e8e8ff; margin: 0 0 6px; }
.goal-desc { font-size: 0.82rem; color: #c0c0e0; margin: 0 0 12px; line-height: 1.5; }
.goal-progress { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.goal-progress-bar { flex: 1; height: 6px; background: rgba(100,100,180,0.08); border-radius: 3px; overflow: hidden; }
.goal-progress-fill { height: 100%; border-radius: 3px; transition: width 0.4s ease; }
.goal-progress-text { font-size: 0.75rem; font-weight: 700; color: #9090b8; font-family: 'JetBrains Mono', monospace; min-width: 36px; text-align: right; }

.goal-milestones { margin-bottom: 12px; }
.milestone-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; &.done { opacity: 0.5; .milestone-text { text-decoration: line-through; } } }
.milestone-text { flex: 1; font-size: 0.8rem; color: #c0c0e0; }
.milestone-btn { font-size: 0.68rem; padding: 3px 10px; background: rgba(0,245,212,0.08); border: 1px solid rgba(0,245,212,0.12); border-radius: 6px; color: #00f5d4; cursor: pointer; transition: all 0.15s; &:hover { background: rgba(0,245,212,0.12); } }

.goal-footer { display: flex; justify-content: space-between; padding-top: 10px; border-top: 1px solid rgba(100,100,180,0.04); }
.goal-status { font-size: 0.72rem; font-weight: 500; }
.goal-created { font-size: 0.68rem; color: #8080a8; }

.empty-state { text-align: center; padding: 60px 20px; }
.empty-icon { font-size: 3rem; margin-bottom: 12px; }
.empty-title { font-size: 1.1rem; font-weight: 700; color: #e8e8ff; margin: 0 0 6px; }
.empty-desc { font-size: 0.85rem; color: #9090b8; margin: 0; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; z-index: 2000; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.6); backdrop-filter: blur(6px); padding: 20px; }
.modal-panel { width: 480px; max-width: 92vw; max-height: 80vh; background: rgba(12,14,30,0.97); border: 1px solid rgba(100,100,180,0.12); border-radius: 16px; overflow: hidden; display: flex; flex-direction: column; animation: modalIn 0.3s ease; }
.delete-dialog { width: 380px; max-width: 88vw; padding: 32px 28px 24px; background: rgba(12,14,30,0.97); border: 1px solid rgba(100,100,180,0.12); border-radius: 16px; text-align: center; animation: modalIn 0.3s ease; }
@keyframes modalIn { from { opacity: 0; transform: scale(0.95) translateY(10px); } to { opacity: 1; transform: scale(1) translateY(0); } }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 24px; border-bottom: 1px solid rgba(100,100,180,0.08); }
.modal-title { font-size: 1.1rem; font-weight: 700; color: #e8e8ff; }
.modal-close { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; background: rgba(100,100,180,0.06); border: none; border-radius: 8px; color: #9090b8; cursor: pointer; transition: all 0.15s; &:hover { background: rgba(100,100,180,0.12); color: #e8e8ff; } }
.modal-body { flex: 1; overflow-y: auto; padding: 24px; }
.form-group { margin-bottom: 16px; &.flex-1 { flex: 1; } }
.form-label { display: block; font-size: 0.82rem; font-weight: 600; color: #c0c0e0; margin-bottom: 6px; }
.form-tip { margin: 6px 0 0; font-size: 0.75rem; color: #9090b8; line-height: 1.5; }
.form-note { display: flex; align-items: flex-start; gap: 8px; padding: 10px 12px; background: rgba(0,245,212,0.05); border: 1px solid rgba(0,245,212,0.12); border-radius: 10px; }
.note-icon { font-size: 0.9rem; flex-shrink: 0; }
.note-text { font-size: 0.78rem; color: #a0a8c8; line-height: 1.6; }
.label-value { float: right; font-weight: 700; color: #f59e0b; font-family: 'JetBrains Mono', monospace; }
.form-input, .form-textarea, .form-select { width: 100%; padding: 10px 14px; background: rgba(0,0,0,0.2); border: 1px solid rgba(100,100,180,0.1); border-radius: 10px; color: #e8e8ff; font-size: 0.88rem; outline: none; font-family: inherit; box-sizing: border-box; transition: border-color 0.2s; &:focus { border-color: rgba(245,158,11,0.25); } }
.form-textarea { resize: vertical; min-height: 80px; line-height: 1.5; }
.form-select { cursor: pointer; option { background: #1a1a3e; } }
.form-range { width: 100%; height: 6px; -webkit-appearance: none; appearance: none; background: rgba(100,100,180,0.1); border-radius: 3px; outline: none; cursor: pointer; &::-webkit-slider-thumb { -webkit-appearance: none; width: 18px; height: 18px; border-radius: 50%; background: #f59e0b; border: none; cursor: pointer; box-shadow: 0 0 6px rgba(245,158,11,0.3); } }
.form-row { display: flex; gap: 12px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid rgba(100,100,180,0.08); }
.btn-cancel { padding: 9px 20px; background: rgba(100,100,180,0.04); border: 1px solid rgba(100,100,180,0.1); border-radius: 10px; color: #9090b8; font-size: 0.85rem; cursor: pointer; transition: all 0.15s; &:hover { border-color: rgba(100,100,180,0.2); color: #e8e8ff; } }
.btn-save { padding: 9px 22px; background: linear-gradient(135deg, #f59e0b, #ef4444); border: none; border-radius: 10px; color: #fff; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: all 0.2s; &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(245,158,11,0.2); } &:disabled { opacity: 0.4; cursor: not-allowed; } }
.btn-delete-confirm { padding: 9px 22px; background: linear-gradient(135deg, rgba(239,68,68,0.15), rgba(200,30,60,0.1)); border: 1px solid rgba(239,68,68,0.2); border-radius: 10px; color: #ef4444; font-size: 0.85rem; font-weight: 600; cursor: pointer; &:hover { box-shadow: 0 4px 16px rgba(239,68,68,0.15); } }
.delete-icon { font-size: 2.5rem; margin-bottom: 12px; }
.delete-title { font-size: 1.1rem; font-weight: 700; color: #e8e8ff; margin: 0 0 8px; }
.delete-desc { font-size: 0.85rem; color: #9090b8; margin: 0 0 24px; }
.delete-actions { display: flex; gap: 12px; justify-content: center; }

.goal-list-enter-active { transition: all 0.3s ease; }
.goal-list-leave-active { transition: all 0.25s ease; }
.goal-list-enter-from { opacity: 0; transform: translateY(12px); }
.goal-list-leave-to { opacity: 0; transform: translateX(20px); }
.modal-enter-active { transition: all 0.25s ease; }
.modal-leave-active { transition: all 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }

@media (max-width: 1024px) { .page-header { padding: 12px 20px; } .content { padding: 20px; } .stats-row { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .page-header { padding: 10px 12px; gap: 10px; flex-wrap: wrap; } .back-btn span, .add-btn span { display: none; } .content { padding: 12px; } .form-row { flex-direction: column; } .goal-actions { opacity: 1; } }
</style>
