<template>
  <div class="calendar-page">
    <!-- ===== 背景层 ===== -->
    <div class="bg-layer">
      <div class="bg-aurora">
        <div class="aurora-layer a1"></div>
        <div class="aurora-layer a2"></div>
        <div class="aurora-layer a3"></div>
      </div>
      <div class="bg-grid"></div>
      <div class="bg-scanline"></div>
    </div>

    <!-- ===== 顶部导航 ===== -->
    <div class="nav-bar">
      <button class="back-btn" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" /></svg>
        <span>返回</span>
      </button>
      <h1 class="nav-title">
        <span class="nav-title-icon">📅</span>
        <span class="nav-title-text">学习日历</span>
        <span class="nav-title-sub">— 掌控每日学习节奏</span>
      </h1>
      <button class="add-task-btn" @click="openAddModal">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
        <span>添加任务</span>
      </button>
    </div>

    <!-- ===== 主内容区 ===== -->
    <div class="calendar-container">
      <!-- ===== 日历面板 ===== -->
      <div class="calendar-panel glass-card">
        <!-- 月份导航 -->
        <div class="month-nav">
          <button class="month-arrow" title="上个月" @click="prevMonth">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6" /></svg>
          </button>
          <div class="month-display">
            <span class="month-year">{{ currentYear }}</span>
            <span class="month-name">{{ monthNames[currentMonth - 1] }}</span>
          </div>
          <button class="month-arrow" title="下个月" @click="nextMonth">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6" /></svg>
          </button>
        </div>

        <!-- 今日快速定位 -->
        <button class="today-jump" :class="{ 'is-today-month': isCurrentMonth }" @click="jumpToToday">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" /></svg>
          <span>今天</span>
        </button>

        <!-- 星期行 -->
        <div class="weekday-row">
          <span v-for="(day, idx) in weekDays" :key="idx" class="weekday-cell" :class="{ 'is-weekend': idx >= 5 }">
            {{ day }}
          </span>
        </div>

        <!-- 日期网格 -->
        <div class="date-grid">
          <!-- 上月占位 -->
          <div
            v-for="blank in leadingBlanks"
            :key="'blank-' + blank"
            class="date-cell is-other-month"
          ></div>

          <!-- 当月日期 -->
          <div
            v-for="day in daysInMonth"
            :key="day"
            class="date-cell"
            :class="{
              'is-today': isToday(day),
              'is-selected': selectedDate === formatDateKey(day),
              'has-tasks': getTasksForDate(formatDateKey(day)).length > 0,
              'is-weekend': getDayOfWeek(day) >= 5,
              'is-holiday': isHoliday(day)
            }"
            @click="selectDate(day)"
          >
            <span class="date-number">{{ day }}</span>
            <!-- 真实学习记录指示器（来自月度统计 API） -->
            <span v-if="getDayStat(formatDateKey(day))?.type === 'learning'" class="study-dot" title="该日有真实学习记录"></span>
            <!-- 任务指示器 -->
            <div v-if="getTasksForDate(formatDateKey(day)).length > 0" class="task-dots">
              <span
                v-for="(_, tIdx) in getTasksForDate(formatDateKey(day)).slice(0, 3)"
                :key="tIdx"
                class="task-dot"
                :class="getDotClass(getTasksForDate(formatDateKey(day))[tIdx])"
              ></span>
              <span v-if="getTasksForDate(formatDateKey(day)).length > 3" class="task-dot-more">
                +{{ getTasksForDate(formatDateKey(day)).length - 3 }}
              </span>
            </div>
            <!-- 选中标识 -->
            <div v-if="selectedDate === formatDateKey(day)" class="select-ring"></div>
            <!-- 今日标识 -->
            <div v-if="isToday(day)" class="today-badge"></div>
          </div>

          <!-- 下月占位 -->
          <div
            v-for="blank in trailingBlanks"
            :key="'trailing-' + blank"
            class="date-cell is-other-month"
          ></div>
        </div>

        <!-- 统计条 -->
        <div class="calendar-stats">
          <div class="stat-chip">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2" /></svg>
            <span>本月任务 <strong>{{ currentMonthTotalTasks }}</strong></span>
          </div>
          <div class="stat-chip">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12" /></svg>
            <span>已完成 <strong>{{ currentMonthCompletedTasks }}</strong></span>
          </div>
          <div class="stat-chip">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
            <span>持续 <strong>{{ currentMonthStreak }}</strong> 天</span>
          </div>
          <div class="stat-chip real-stat" title="真实学习数据（来自学习记录）">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
            <span>学习时长 <strong>{{ monthStats.summary?.totalHours ?? 0 }}</strong>h</span>
          </div>
          <div class="stat-chip real-stat" title="真实学习数据（来自学习记录）">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /><circle cx="12" cy="10" r="3" /></svg>
            <span>学习天数 <strong>{{ monthLearningDays }}</strong> 天</span>
          </div>
        </div>
      </div>

      <!-- ===== 右侧任务面板 ===== -->
      <div class="tasks-panel glass-card">
        <div class="tasks-header">
          <div class="tasks-header-left">
            <span class="tasks-icon">📋</span>
            <div class="tasks-title-group">
              <h2 class="tasks-title">今日任务</h2>
              <span class="tasks-date">{{ displaySelectedDate }}</span>
            </div>
          </div>
          <span class="tasks-count">{{ selectedTasks.length }} 项</span>
        </div>

        <!-- 选中日真实学习记录（来自月度统计 API） -->
        <div v-if="selectedDayStat" class="study-day-info">
          <span class="study-day-chip">⏱️ 学习 {{ selectedDayStat.duration }} 分钟</span>
          <span class="study-day-chip">📝 完成 {{ selectedDayStat.count }} 个节点</span>
          <span class="study-day-chip real-badge">真实数据</span>
        </div>

        <!-- 任务列表 -->
        <TransitionGroup name="task-list" tag="div" class="tasks-list">
          <div
            v-for="task in selectedTasks"
            :key="task.id"
            class="task-item"
            :class="{ 'is-completed': task.status === 'completed', 'is-in-progress': task.status === 'in_progress' }"
          >
            <button class="task-checkbox" :title="toggleHint(task)" @click="toggleTaskStatus(task)">
              <span v-if="task.status === 'completed'" class="checkbox-icon completed">✅</span>
              <span v-else-if="task.status === 'in_progress'" class="checkbox-icon in-progress">🔄</span>
              <span v-else class="checkbox-icon pending">⬜</span>
            </button>
            <div class="task-content">
              <div class="task-name-row">
                <span class="task-name">{{ task.name }}</span>
                <span class="task-status-tag" :class="task.status">
                  {{ statusText[task.status] }}
                </span>
              </div>
              <div class="task-meta">
                <span class="task-category" :style="{ color: categoryColor(task.category) }">
                  {{ task.category }}
                </span>
                <span v-if="task.time" class="task-time">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
                  {{ task.time }}
                </span>
              </div>
              <div class="task-progress-track">
                <div class="task-progress-fill" :style="{ width: task.progress + '%' }" :class="progressColor(task.progress)"></div>
              </div>
            </div>
            <button class="task-delete" title="删除任务" @click="deleteTask(task.id)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
            </button>
          </div>
        </TransitionGroup>

        <!-- 空状态 -->
        <div v-if="selectedTasks.length === 0" class="tasks-empty">
          <div class="empty-icon-wrap">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="empty-icon-svg"><rect x="3" y="4" width="18" height="18" rx="2" ry="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" /></svg>
          </div>
          <p class="empty-title">今日暂无任务</p>
          <p class="empty-desc">点击上方「添加任务」开始规划你的学习</p>
        </div>
      </div>
    </div>

    <!-- ===== 添加任务弹窗 ===== -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showModal" class="modal-overlay" @click.self="closeAddModal">
          <div class="modal-panel glass-card">
            <div class="modal-header">
              <h2 class="modal-title">
                <span class="modal-title-icon">✨</span>
                添加新任务
              </h2>
              <button class="modal-close" @click="closeAddModal">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="form-group">
                <label class="form-label">任务名称</label>
                <input
                  ref="taskNameInput"
                  v-model="newTask.name"
                  class="form-input"
                  placeholder="输入学习任务名称..."
                  maxlength="60"
                  @keyup.enter="confirmAddTask"
                />
              </div>
              <div class="form-row">
                <div class="form-group flex-1">
                  <label class="form-label">日期</label>
                  <input v-model="newTask.date" type="date" class="form-input" />
                </div>
                <div class="form-group flex-1">
                  <label class="form-label">时间（可选）</label>
                  <input v-model="newTask.time" type="time" class="form-input" />
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">分类</label>
                <div class="category-select">
                  <button
                    v-for="cat in categories"
                    :key="cat.value"
                    class="category-option"
                    :class="{ active: newTask.category === cat.value }"
                    :style="{ '--cat-color': cat.color }"
                    @click="newTask.category = cat.value"
                  >
                    <span class="cat-dot" :style="{ background: cat.color }"></span>
                    {{ cat.label }}
                  </button>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn-cancel" @click="closeAddModal">取消</button>
              <button class="btn-confirm" :disabled="!newTask.name.trim()" @click="confirmAddTask">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12" /></svg>
                确认添加
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getMonthlyStats } from '@/api/achievement'

const router = useRouter()

// ========== 日期状态 ==========
const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1) // 1-based
const selectedDate = ref(formatDateKey(today.getDate(), today.getMonth() + 1, today.getFullYear()))

// ========== 月度统计（真实学习数据） ==========
const monthStats = ref({ calendar: [], summary: {} })

// 加载当前月份真实学习统计（打卡 + 学习时长/完成节点）
const loadMonthStats = async () => {
  try {
    const res = await getMonthlyStats(currentYear.value, currentMonth.value)
    const data = res?.data || {}
    monthStats.value = {
      calendar: Array.isArray(data.calendar) ? data.calendar : [],
      summary: data.summary || {}
    }
  } catch (e) {
    console.error('加载月度统计失败:', e)
    monthStats.value = { calendar: [], summary: {} }
  }
}

// 某天真实学习数据（dateKey: YYYY-MM-DD）
const getDayStat = (dateKey) => {
  return monthStats.value.calendar.find(d => d.date === dateKey) || null
}

// 本月真实学习天数（有学习记录或打卡的天）
const monthLearningDays = computed(() => {
  return monthStats.value.calendar.filter(d => d.type === 'learning').length
})

// 选中日的真实学习数据
const selectedDayStat = computed(() => {
  return getDayStat(selectedDate.value)
})

// 月份切换时重新加载月度统计
watch([currentYear, currentMonth], () => {
  loadMonthStats()
})

const monthNames = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
const weekDays = ['一', '二', '三', '四', '五', '六', '日']

// ========== 计算属性 ==========
const daysInMonth = computed(() => {
  return new Date(currentYear.value, currentMonth.value, 0).getDate()
})

const firstDayOfWeek = computed(() => {
  // 0=周日, 我们想要周一=0
  const day = new Date(currentYear.value, currentMonth.value - 1, 1).getDay()
  return day === 0 ? 6 : day - 1 // 转换：周日→6, 周一→0
})

const leadingBlanks = computed(() => firstDayOfWeek.value)
const trailingBlanks = computed(() => {
  const totalCells = leadingBlanks.value + daysInMonth.value
  const remainder = totalCells % 7
  return remainder === 0 ? 0 : 7 - remainder
})

const isCurrentMonth = computed(() => {
  return currentYear.value === today.getFullYear() && currentMonth.value === today.getMonth() + 1
})

const displaySelectedDate = computed(() => {
  const [y, m, d] = selectedDate.value.split('-').map(Number)
  return `${y}年${m}月${d}日 周${weekDayNames[new Date(y, m - 1, d).getDay()]}`
})

const weekDayNames = ['日', '一', '二', '三', '四', '五', '六']

// ========== 任务数据 ==========
const STORAGE_KEY = 'ai_learning_calendar_tasks'

// 任务状态文本
const statusText = {
  pending: '待开始',
  in_progress: '进行中',
  completed: '已完成'
}

const categories = [
  { value: '编程', label: '编程', color: '#00f5d4' },
  { value: '数学', label: '数学', color: '#7b61ff' },
  { value: '英语', label: '英语', color: '#f59e0b' },
  { value: '理论', label: '理论', color: '#3b82f6' },
  { value: '项目', label: '项目', color: '#ec4899' },
  { value: '其他', label: '其他', color: '#94a3b8' }
]

const tasks = ref([])
const showModal = ref(false)
const taskNameInput = ref(null)

const newTask = ref({
  name: '',
  date: formatDateKey(today.getDate(), today.getMonth() + 1, today.getFullYear()),
  time: '',
  category: '编程'
})

// ========== 方法：日期工具 ==========
function formatDateKey(day, month, year) {
  const m = (month || currentMonth.value).toString().padStart(2, '0')
  const y = (year || currentYear.value).toString()
  const d = day.toString().padStart(2, '0')
  return `${y}-${m}-${d}`
}

function isToday(day) {
  const dateKey = formatDateKey(day)
  return dateKey === formatDateKey(today.getDate(), today.getMonth() + 1, today.getFullYear())
}

function getDayOfWeek(day) {
  return new Date(currentYear.value, currentMonth.value - 1, day).getDay()
}

function isHoliday(day) {
  // 简单实现：标记一些常见节假日（模拟）
  const dateKey = formatDateKey(day)
  const holidays = [
    `${currentYear.value}-01-01`, // 元旦
    `${currentYear.value}-05-01`, // 劳动节
    `${currentYear.value}-10-01`, // 国庆
  ]
  // 加上周末
  const dow = getDayOfWeek(day)
  return holidays.includes(dateKey) || dow === 0 || dow === 6
}

// ========== 方法：月份导航 ==========
function prevMonth() {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

function nextMonth() {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

function jumpToToday() {
  currentYear.value = today.getFullYear()
  currentMonth.value = today.getMonth() + 1
  selectedDate.value = formatDateKey(today.getDate(), today.getMonth() + 1, today.getFullYear())
}

function selectDate(day) {
  selectedDate.value = formatDateKey(day)
}

// ========== 方法：任务管理 ==========
function loadTasks() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      tasks.value = JSON.parse(saved)
    } else {
      // 首次使用，创建模拟数据
      tasks.value = generateMockTasks()
      saveTasks()
    }
  } catch {
    tasks.value = generateMockTasks()
    saveTasks()
  }
}

function saveTasks() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks.value))
}

function generateMockTasks() {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  const pad = (n) => n.toString().padStart(2, '0')
  const dateStr = (d) => `${y}-${pad(m)}-${pad(d)}`

  const mock = [
    { id: 'mock-1', name: '学习 Vue 3 Composition API', date: dateStr(now.getDate()), time: '09:00', category: '编程', status: 'in_progress', progress: 65 },
    { id: 'mock-2', name: '完成 LeetCode 每日一题', date: dateStr(now.getDate()), time: '11:00', category: '数学', status: 'pending', progress: 0 },
    { id: 'mock-3', name: '阅读《深入理解计算机系统》第二章', date: dateStr(now.getDate()), time: '14:00', category: '理论', status: 'completed', progress: 100 },
    { id: 'mock-4', name: '英语听力训练 30分钟', date: dateStr(now.getDate()), time: '16:00', category: '英语', status: 'pending', progress: 0 },
    { id: 'mock-5', name: 'Spring AI 项目实践 - 实现RAG功能', date: dateStr(now.getDate() + 1), time: '10:00', category: '项目', status: 'pending', progress: 0 },
    { id: 'mock-6', name: '机器学习笔记整理', date: dateStr(now.getDate() + 1), time: '15:00', category: '编程', status: 'completed', progress: 100 },
    { id: 'mock-7', name: '复习线性代数 - 矩阵运算', date: dateStr(now.getDate() - 1), time: '08:30', category: '数学', status: 'completed', progress: 100 },
    { id: 'mock-8', name: 'React Hooks 深入学习', date: dateStr(now.getDate() - 1), time: '13:00', category: '编程', status: 'in_progress', progress: 40 },
    { id: 'mock-9', name: '系统设计 - 微服务架构阅读', date: dateStr(now.getDate() + 2), time: '09:00', category: '理论', status: 'pending', progress: 0 },
    { id: 'mock-10', name: '英语口语练习 - 托福话题', date: dateStr(now.getDate() + 2), time: '19:00', category: '英语', status: 'pending', progress: 0 },
  ]

  // 在当月随机多天添加一些任务让日历看起来丰富
  const extraDays = [5, 8, 12, 15, 18, 22, 25, 28]
  extraDays.forEach((d, idx) => {
    if (d <= daysInMonth.value && d !== now.getDate()) {
      mock.push({
        id: `mock-extra-${idx}`,
        name: idx % 2 === 0 ? '编程练习' : '理论学习',
        date: dateStr(d),
        time: null,
        category: idx % 2 === 0 ? '编程' : '理论',
        status: idx % 3 === 0 ? 'completed' : idx % 3 === 1 ? 'in_progress' : 'pending',
        progress: idx % 3 === 0 ? 100 : idx % 3 === 1 ? 55 : 0
      })
    }
  })

  return mock
}

function getTasksForDate(dateKey) {
  return tasks.value.filter(t => t.date === dateKey)
}

const selectedTasks = computed(() => {
  return getTasksForDate(selectedDate.value)
})

// 本月统计
const currentMonthTasks = computed(() => {
  return tasks.value.filter(t => t.date.startsWith(`${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}`))
})

const currentMonthTotalTasks = computed(() => currentMonthTasks.value.length)

const currentMonthCompletedTasks = computed(() => {
  return currentMonthTasks.value.filter(t => t.status === 'completed').length
})

const currentMonthStreak = computed(() => {
  // 计算本月连续学习天数（完成至少1个任务的天数）
  const taskDates = [...new Set(currentMonthTasks.value.map(t => t.date))].sort()
  if (taskDates.length === 0) return 0
  let streak = 0
  const todayStr = formatDateKey(today.getDate(), today.getMonth() + 1, today.getFullYear())
  // 从今天往前数
  for (let i = 0; i < taskDates.length; i++) {
    const date = new Date(todayStr)
    date.setDate(date.getDate() - i)
    const check = formatDateKey(date.getDate(), date.getMonth() + 1, date.getFullYear())
    if (taskDates.includes(check)) {
      streak++
    } else {
      break
    }
  }
  return streak
})

function getDotClass(task) {
  if (task.status === 'completed') return 'dot-completed'
  if (task.status === 'in_progress') return 'dot-in-progress'
  return 'dot-pending'
}

function categoryColor(cat) {
  const found = categories.find(c => c.value === cat)
  return found ? found.color : '#94a3b8'
}

function progressColor(progress) {
  if (progress >= 100) return 'fill-completed'
  if (progress >= 50) return 'fill-half'
  return 'fill-low'
}

function toggleHint(task) {
  if (task.status === 'completed') return '标记为未完成'
  if (task.status === 'in_progress') return '标记为已完成'
  return '标记为进行中'
}

function toggleTaskStatus(task) {
  if (task.status === 'completed') {
    task.status = 'pending'
    task.progress = 0
  } else if (task.status === 'in_progress') {
    task.status = 'completed'
    task.progress = 100
  } else {
    task.status = 'in_progress'
    task.progress = 25
  }
  saveTasks()
}

function deleteTask(id) {
  tasks.value = tasks.value.filter(t => t.id !== id)
  saveTasks()
}

// ========== 弹窗操作 ==========
function openAddModal() {
  newTask.value = {
    name: '',
    date: selectedDate.value,
    time: '',
    category: '编程'
  }
  showModal.value = true
  nextTick(() => {
    taskNameInput.value?.focus()
  })
}

function closeAddModal() {
  showModal.value = false
}

function confirmAddTask() {
  const name = newTask.value.name.trim()
  if (!name) return

  const task = {
    id: 'task-' + Date.now() + '-' + Math.random().toString(36).slice(2, 6),
    name: name,
    date: newTask.value.date,
    time: newTask.value.time || null,
    category: newTask.value.category,
    status: 'pending',
    progress: 0
  }

  tasks.value.push(task)
  saveTasks()

  // 如果添加的日期不是当前选中的日期，跳转到该日期
  if (newTask.value.date !== selectedDate.value) {
    selectedDate.value = newTask.value.date
    // 也切换月份
    const [y, m] = newTask.value.date.split('-').map(Number)
    currentYear.value = y
    currentMonth.value = m
  }

  closeAddModal()
}

// ========== 导航 ==========
function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}

// ========== 生命周期 ==========
onMounted(() => {
  loadTasks()
  loadMonthStats()
})
</script>

<style lang="scss" scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;600;700;800;900&display=swap');

// ===== 变量 =====
$bg-deep: #0a0a1a;
$cyan: #00f5d4;
$cyan-dim: rgba(0, 245, 212, 0.15);
$cyan-glow: rgba(0, 245, 212, 0.25);
$purple: #7b61ff;
$blue: #0055ff;
$text-primary: #e8e8ff;
$text-secondary: #c0c0e0;
$text-muted: #8080b0;
$card-bg: rgba(15, 18, 45, 0.55);
$border-subtle: rgba(100, 100, 180, 0.08);
$border-glass: rgba(100, 100, 180, 0.12);

// ===== 字体 =====
$font-display: 'Orbitron', 'Courier New', monospace;

// ===== 页面容器 =====
.calendar-page {
  min-height: calc(100vh - 68px);
  background: $bg-deep;
  position: relative;
  overflow: hidden;
  padding: 0;
  display: flex;
  flex-direction: column;
}

// ===== 背景 =====
.bg-layer {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.bg-aurora {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 70% 15%, rgba(0, 245, 212, 0.07) 0%, transparent 50%),
    radial-gradient(ellipse at 25% 75%, rgba(123, 97, 255, 0.05) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 45%, rgba(0, 85, 255, 0.04) 0%, transparent 50%);
  animation: auroraDrift 20s ease-in-out infinite;
}

@keyframes auroraDrift {
  0%, 100% { transform: scale(1) rotate(0deg); }
  33% { transform: scale(1.08) rotate(0.8deg); }
  66% { transform: scale(0.95) rotate(-0.6deg); }
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 245, 212, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(123, 97, 255, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  animation: gridPulse 8s ease-in-out infinite alternate;
}

@keyframes gridPulse {
  0% { opacity: 0.3; transform: scale(1); }
  100% { opacity: 0.6; transform: scale(1.02); }
}

.bg-scanline {
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(0, 245, 212, 0.008) 2px,
    rgba(0, 245, 212, 0.008) 4px
  );
  pointer-events: none;
}

// ===== 玻璃卡片 =====
.glass-card {
  background: $card-bg;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid $border-glass;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}

// ===== 导航栏 =====
.nav-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 32px;
  background: rgba(10, 10, 26, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid $border-subtle;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid $border-subtle;
  border-radius: 8px;
  color: $text-secondary;
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  flex-shrink: 0;

  &:hover {
    border-color: $cyan-dim;
    color: $cyan;
    background: rgba(0, 245, 212, 0.04);
    box-shadow: 0 0 14px rgba(0, 245, 212, 0.08);
  }
}

.nav-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.05rem;
  font-weight: 700;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  .nav-title-icon {
    font-size: 1.2rem;
    filter: drop-shadow(0 0 6px rgba(0, 245, 212, 0.3));
  }

  .nav-title-text {
    background: linear-gradient(135deg, $cyan, $blue);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .nav-title-sub {
    font-size: 0.75rem;
    font-weight: 400;
    color: $text-muted;
    -webkit-text-fill-color: $text-muted;
    display: none;

    @media (min-width: 768px) {
      display: inline;
    }
  }
}

.add-task-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  background: linear-gradient(135deg, rgba(0, 245, 212, 0.15), rgba(0, 85, 255, 0.1));
  border: 1px solid $cyan-dim;
  border-radius: 10px;
  color: $cyan;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  flex-shrink: 0;
  letter-spacing: 0.3px;

  &:hover {
    background: linear-gradient(135deg, rgba(0, 245, 212, 0.25), rgba(0, 85, 255, 0.18));
    border-color: $cyan-glow;
    box-shadow: 0 0 20px rgba(0, 245, 212, 0.15);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }

  svg {
    filter: drop-shadow(0 0 4px rgba(0, 245, 212, 0.4));
  }
}

// ===== 日历容器 =====
.calendar-container {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 32px 60px;
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
  align-items: start;
}

// ===== 日历面板 =====
.calendar-panel {
  padding: 24px 20px 20px;
  min-width: 0;
}

// ===== 月份导航 =====
.month-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-bottom: 12px;
}

.month-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid $border-subtle;
  border-radius: 10px;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    border-color: $cyan-dim;
    color: $cyan;
    background: rgba(0, 245, 212, 0.04);
    box-shadow: 0 0 12px rgba(0, 245, 212, 0.08);
    transform: scale(1.05);
  }

  &:active {
    transform: scale(0.95);
  }
}

.month-display {
  display: flex;
  align-items: baseline;
  gap: 12px;
  user-select: none;
}

.month-year {
  font-family: $font-display;
  font-size: 0.85rem;
  font-weight: 500;
  color: $text-muted;
  letter-spacing: 2px;
}

.month-name {
  font-size: 1.6rem;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: 4px;
  background: linear-gradient(135deg, $text-primary, $cyan);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

// ===== 今日跳转 =====
.today-jump {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 0 auto 16px;
  padding: 5px 14px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid transparent;
  border-radius: 20px;
  color: $text-muted;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    border-color: $cyan-dim;
    color: $cyan;
    background: rgba(0, 245, 212, 0.04);
  }

  &.is-today-month {
    border-color: $cyan-dim;
    color: $cyan;
  }
}

// ===== 星期行 =====
.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 8px;
}

.weekday-cell {
  text-align: center;
  padding: 8px 0;
  font-size: 0.8rem;
  font-weight: 600;
  color: $text-muted;
  letter-spacing: 1px;
  text-transform: uppercase;

  &.is-weekend {
    color: rgba($cyan, 0.5);
  }
}

// ===== 日期网格 =====
.date-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.date-cell {
  position: relative;
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(100, 100, 180, 0.02);
  border: 1px solid transparent;
  min-height: 52px;

  &.is-other-month {
    opacity: 0;
    pointer-events: none;
  }

  &:hover:not(.is-other-month) {
    background: rgba(0, 245, 212, 0.05);
    border-color: rgba(0, 245, 212, 0.15);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);

    .date-number {
      color: $cyan;
    }
  }

  &.is-selected {
    background: rgba(0, 245, 212, 0.08);
    border-color: $cyan;
    box-shadow: 0 0 20px rgba(0, 245, 212, 0.12), inset 0 0 20px rgba(0, 245, 212, 0.02);

    .date-number {
      color: $cyan;
      font-weight: 700;
    }

    .select-ring {
      animation: ringPulse 2s ease-in-out infinite;
    }
  }

  &.is-today {
    .date-number {
      color: #fff;
      font-weight: 800;
      text-shadow: 0 0 12px rgba(0, 245, 212, 0.5);
    }

    .today-badge {
      position: absolute;
      bottom: 4px;
      left: 50%;
      transform: translateX(-50%);
      width: 4px;
      height: 4px;
      background: $cyan;
      border-radius: 50%;
      box-shadow: 0 0 6px $cyan;
    }
  }

  &.is-weekend:not(.is-other-month) {
    .date-number {
      color: rgba($cyan, 0.5);
    }
  }

  &.has-tasks:not(.is-other-month) {
    .date-number {
      &::after {
        content: '';
        position: absolute;
        bottom: 2px;
        left: 50%;
        transform: translateX(-50%);
        width: 3px;
        height: 3px;
        background: $cyan;
        border-radius: 50%;
        opacity: 0.4;
      }
    }
  }
}

.date-number {
  font-family: $font-display;
  font-size: 0.95rem;
  font-weight: 600;
  color: $text-secondary;
  transition: all 0.2s ease;
  position: relative;
  z-index: 1;
}

// 选中光环
.select-ring {
  position: absolute;
  inset: -2px;
  border-radius: 14px;
  border: 2px solid $cyan;
  opacity: 0.6;
  pointer-events: none;
}

@keyframes ringPulse {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 0.3; transform: scale(1.04); }
}

// ===== 任务指示器 =====
.task-dots {
  display: flex;
  align-items: center;
  gap: 2px;
  position: relative;
  z-index: 1;
  margin-top: 1px;
}

.task-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;

  &.dot-completed {
    background: $cyan;
    box-shadow: 0 0 4px rgba(0, 245, 212, 0.4);
  }

  &.dot-in-progress {
    background: $purple;
    box-shadow: 0 0 4px rgba(123, 97, 255, 0.4);
  }

  &.dot-pending {
    background: $text-muted;
  }
}

.task-dot-more {
  font-size: 0.55rem;
  color: $text-muted;
  line-height: 1;
}

// ===== 真实学习记录指示器 =====
.study-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: $cyan;
  box-shadow: 0 0 6px rgba(0, 245, 212, 0.6);
  position: relative;
  z-index: 1;
  margin-top: 1px;
  flex-shrink: 0;
}

// ===== 真实学习统计 chip =====
.real-stat {
  border-color: rgba(0, 245, 212, 0.25) !important;
  background: rgba(0, 245, 212, 0.04) !important;

  strong {
    color: $cyan;
  }
}

// ===== 选中日真实学习记录 =====
.study-day-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 14px;
  background: rgba(0, 245, 212, 0.04);
  border: 1px solid rgba(0, 245, 212, 0.15);
  border-radius: 10px;
}

.study-day-chip {
  font-size: 0.75rem;
  color: $text-secondary;

  &.real-badge {
    font-size: 0.65rem;
    padding: 2px 8px;
    border-radius: 8px;
    background: rgba(0, 245, 212, 0.1);
    color: $cyan;
  }
}

// ===== 统计条 =====
.calendar-stats {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid $border-subtle;
  flex-wrap: wrap;
}

.stat-chip {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid $border-subtle;
  border-radius: 20px;
  font-size: 0.72rem;
  color: $text-muted;
  transition: all 0.2s ease;

  &:hover {
    border-color: $cyan-dim;
    color: $text-secondary;
  }

  svg {
    color: $cyan;
    opacity: 0.6;
  }

  strong {
    color: $cyan;
    font-weight: 700;
    font-family: $font-display;
    font-size: 0.85rem;
    margin-left: 1px;
  }
}

// ===== 任务面板 =====
.tasks-panel {
  padding: 20px;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 108px;
  max-height: calc(100vh - 140px);
  overflow-y: auto;
}

.tasks-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid $border-subtle;
}

.tasks-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tasks-icon {
  font-size: 1.3rem;
  filter: drop-shadow(0 0 6px rgba(0, 245, 212, 0.2));
}

.tasks-title-group {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.tasks-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: $text-primary;
}

.tasks-date {
  font-size: 0.7rem;
  color: $text-muted;
  font-weight: 400;
}

.tasks-count {
  font-family: $font-display;
  font-size: 0.8rem;
  color: $cyan;
  padding: 3px 10px;
  background: rgba(0, 245, 212, 0.06);
  border: 1px solid $cyan-dim;
  border-radius: 12px;
  white-space: nowrap;
}

// ===== 任务列表 =====
.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 60px;
}

.task-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: rgba(100, 100, 180, 0.03);
  border: 1px solid $border-subtle;
  border-radius: 12px;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: transparent;
    border-radius: 0 2px 2px 0;
    transition: all 0.3s ease;
  }

  &:hover {
    border-color: rgba(100, 100, 180, 0.2);
    background: rgba(100, 100, 180, 0.06);
    transform: translateX(2px);

    .task-delete {
      opacity: 1;
      transform: translateX(0);
    }
  }

  &.is-completed {
    border-color: rgba(0, 245, 212, 0.1);
    background: rgba(0, 245, 212, 0.02);

    &::before {
      background: $cyan;
      box-shadow: 0 0 8px rgba(0, 245, 212, 0.3);
    }

    .task-name {
      text-decoration: line-through;
      color: $text-muted;
    }
  }

  &.is-in-progress {
    &::before {
      background: $purple;
      box-shadow: 0 0 8px rgba(123, 97, 255, 0.3);
    }
  }
}

.task-checkbox {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  margin-top: 1px;

  &:hover {
    background: rgba(100, 100, 180, 0.08);
    transform: scale(1.1);
  }

  &:active {
    transform: scale(0.95);
  }
}

.checkbox-icon {
  font-size: 1rem;
  transition: all 0.3s ease;

  &.completed {
    filter: drop-shadow(0 0 4px rgba(0, 245, 212, 0.4));
  }

  &.in-progress {
    animation: spinSlow 3s linear infinite;
  }
}

@keyframes spinSlow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.task-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.task-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: $text-primary;
  transition: color 0.3s ease;
}

.task-status-tag {
  font-size: 0.65rem;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  letter-spacing: 0.3px;

  &.pending {
    background: rgba(148, 163, 184, 0.1);
    color: $text-muted;
    border: 1px solid rgba(148, 163, 184, 0.15);
  }

  &.in_progress {
    background: rgba(123, 97, 255, 0.1);
    color: $purple;
    border: 1px solid rgba(123, 97, 255, 0.2);
  }

  &.completed {
    background: rgba(0, 245, 212, 0.08);
    color: $cyan;
    border: 1px solid rgba(0, 245, 212, 0.15);
  }
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.task-category {
  font-size: 0.7rem;
  font-weight: 500;
  opacity: 0.8;
}

.task-time {
  font-size: 0.7rem;
  color: $text-muted;
  display: flex;
  align-items: center;
  gap: 3px;

  svg {
    opacity: 0.5;
  }
}

// 进度条
.task-progress-track {
  height: 3px;
  background: rgba(100, 100, 180, 0.08);
  border-radius: 2px;
  overflow: hidden;
  margin-top: 2px;
}

.task-progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);

  &.fill-completed {
    background: linear-gradient(90deg, $cyan, #10b981);
    box-shadow: 0 0 6px rgba(0, 245, 212, 0.3);
  }

  &.fill-half {
    background: linear-gradient(90deg, $purple, $cyan);
    box-shadow: 0 0 6px rgba(123, 97, 255, 0.2);
  }

  &.fill-low {
    background: linear-gradient(90deg, rgba(148, 163, 184, 0.3), $purple);
  }
}

// 删除按钮
.task-delete {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: 8px;
  color: rgba(239, 68, 68, 0.3);
  opacity: 0;
  transform: translateX(6px);
  transition: all 0.2s ease;
  margin-top: 1px;

  &:hover {
    color: #ef4444;
    background: rgba(239, 68, 68, 0.08);
    box-shadow: 0 0 8px rgba(239, 68, 68, 0.1);
  }
}

// ===== 空状态 =====
.tasks-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.empty-icon-wrap {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid $border-subtle;
  border-radius: 16px;
  margin-bottom: 12px;
}

.empty-icon-svg {
  color: $text-muted;
  opacity: 0.4;
}

.empty-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: $text-secondary;
  margin-bottom: 4px;
}

.empty-desc {
  font-size: 0.8rem;
  color: $text-muted;
}

// ===== 任务列表动画 =====
.task-list-enter-active {
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.task-list-leave-active {
  transition: all 0.25s ease;
}

.task-list-enter-from {
  opacity: 0;
  transform: translateX(-20px) scale(0.95);
}

.task-list-leave-to {
  opacity: 0;
  transform: translateX(20px) scale(0.95);
}

.task-list-move {
  transition: transform 0.35s ease;
}

// ===== 弹窗遮罩 =====
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  padding: 20px;
}

.modal-panel {
  width: 100%;
  max-width: 480px;
  padding: 0;
  overflow: hidden;
  animation: modalIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.1rem;
  font-weight: 700;
  color: $text-primary;
}

.modal-title-icon {
  font-size: 1.2rem;
}

.modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid $border-subtle;
  border-radius: 8px;
  color: $text-muted;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: $text-primary;
    border-color: rgba(100, 100, 180, 0.2);
    background: rgba(100, 100, 180, 0.1);
  }
}

.modal-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;

  &.flex-1 {
    flex: 1;
  }
}

.form-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: $text-secondary;
  letter-spacing: 0.5px;
}

.form-input {
  padding: 10px 14px;
  background: rgba(100, 100, 180, 0.06);
  border: 1px solid $border-subtle;
  border-radius: 10px;
  color: $text-primary;
  font-size: 0.88rem;
  outline: none;
  transition: all 0.25s ease;
  font-family: inherit;

  &::placeholder {
    color: $text-muted;
    font-weight: 400;
  }

  &:focus {
    border-color: $cyan-dim;
    background: rgba(0, 245, 212, 0.03);
    box-shadow: 0 0 12px rgba(0, 245, 212, 0.06);
  }

  &[type="date"],
  &[type="time"] {
    color-scheme: dark;
    cursor: pointer;

    &::-webkit-calendar-picker-indicator {
      filter: invert(0.7) sepia(1) hue-rotate(160deg) saturate(2);
      cursor: pointer;
    }
  }
}

.form-row {
  display: flex;
  gap: 12px;
}

// 分类选择
.category-select {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-option {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid $border-subtle;
  border-radius: 20px;
  color: $text-secondary;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s ease;

  .cat-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  &:hover {
    border-color: rgba(100, 100, 180, 0.2);
    background: rgba(100, 100, 180, 0.08);
  }

  &.active {
    border-color: var(--cat-color, $cyan);
    background: rgba(0, 245, 212, 0.06);
    color: var(--cat-color, $cyan);
    box-shadow: 0 0 10px rgba(var(--cat-color), 0.08);
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px 20px;
  border-top: 1px solid $border-subtle;
}

.btn-cancel {
  padding: 9px 20px;
  background: rgba(100, 100, 180, 0.04);
  border: 1px solid $border-subtle;
  border-radius: 10px;
  color: $text-muted;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: $text-secondary;
    border-color: rgba(100, 100, 180, 0.2);
    background: rgba(100, 100, 180, 0.08);
  }
}

.btn-confirm {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 22px;
  background: linear-gradient(135deg, $cyan, $blue);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(0, 245, 212, 0.25);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  svg {
    filter: drop-shadow(0 0 4px rgba(255, 255, 255, 0.3));
  }
}

// ===== 弹窗过渡 =====
.modal-enter-active {
  transition: all 0.3s ease;

  .modal-panel {
    animation: modalIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  }
}

.modal-leave-active {
  transition: all 0.2s ease;

  .modal-panel {
    animation: modalOut 0.2s ease forwards;
  }
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

@keyframes modalOut {
  to {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
}

// ===== 响应式 =====
@media (max-width: 1024px) {
  .nav-bar {
    padding: 12px 20px;
  }

  .calendar-container {
    grid-template-columns: 1fr;
    padding: 16px 20px 40px;
  }

  .tasks-panel {
    position: static;
    max-height: none;
  }

  .nav-title-sub {
    display: none;
  }
}

@media (max-width: 640px) {
  .nav-bar {
    padding: 10px 12px;
    gap: 10px;
    flex-wrap: wrap;
  }

  .back-btn span {
    display: none;
  }

  .add-task-btn span {
    display: none;
  }

  .add-task-btn {
    padding: 9px 12px;
  }

  .nav-title {
    font-size: 0.9rem;
    min-width: 0;
  }

  .nav-title-icon {
    font-size: 1rem;
  }

  .calendar-container {
    padding: 12px 12px 30px;
    gap: 16px;
  }

  .calendar-panel {
    padding: 16px 12px;
  }

  .month-name {
    font-size: 1.3rem;
  }

  .month-year {
    font-size: 0.75rem;
  }

  .date-cell {
    min-height: 42px;
    border-radius: 8px;
  }

  .date-number {
    font-size: 0.82rem;
  }

  .task-dot {
    width: 4px;
    height: 4px;
  }

  .weekday-cell {
    font-size: 0.7rem;
    padding: 6px 0;
  }

  .calendar-stats {
    gap: 8px;
  }

  .stat-chip {
    font-size: 0.65rem;
    padding: 4px 10px;
  }

  .tasks-panel {
    padding: 16px;
  }

  .modal-panel {
    max-width: 100%;
    margin: 0 10px;
  }

  .modal-body {
    padding: 16px;
  }

  .form-row {
    flex-direction: column;
  }

  .category-select {
    gap: 6px;
  }

  .category-option {
    padding: 5px 10px;
    font-size: 0.75rem;
  }
}
</style>
