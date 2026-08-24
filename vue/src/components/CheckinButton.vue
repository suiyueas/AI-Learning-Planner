<template>
  <div class="checkin-container">
    <div v-if="todayChecked" class="checked-status">
      <span class="icon">✅</span>
      <span>今日已打卡</span>
      <span class="days">连续打卡 {{ continuousDays }} 天</span>
    </div>
    <button v-else class="checkin-btn" @click="handleCheckin" :disabled="isChecking">
      <span v-if="isChecking" class="btn-spinner-small"></span>
      <span v-else>📌</span>
      <span>每日打卡</span>
      <span class="hint">连续打卡奖励：{{ nextReward }}</span>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { checkIn, getCheckinStats } from '@/api/achievement'

const emit = defineEmits(['refresh'])

const todayChecked = ref(false)
const continuousDays = ref(0)
const isChecking = ref(false)
const nextReward = ref('+10 积分')

const handleCheckin = async () => {
  if (isChecking.value) return
  isChecking.value = true
  try {
    const res = await checkIn()
    if (res.success || res.data) {
      todayChecked.value = true
      continuousDays.value = res.data?.continuousDays || continuousDays.value + 1
      ElMessage.success(`🎉 打卡成功！已连续打卡 ${continuousDays.value} 天`)
      emit('refresh')
      await loadCheckinStats()
    }
  } catch (error) {
    ElMessage.error(error.message || '打卡失败，请重试')
  } finally {
    isChecking.value = false
  }
}

const loadCheckinStats = async () => {
  try {
    const res = await getCheckinStats()
    if (res.data) {
      todayChecked.value = res.data.todayChecked || false
      continuousDays.value = res.data.continuousDays || 0
    }
  } catch (e) {
    console.warn('加载打卡状态失败:', e.message)
  }
}

onMounted(() => {
  loadCheckinStats()
})
</script>

<style scoped>
.checkin-container {
  display: inline-block;
}

.checked-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 20px;
  color: #10b981;
  font-size: 0.85rem;
  font-weight: 500;
}

.checked-status .days {
  font-size: 0.75rem;
  opacity: 0.8;
}

.checkin-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #00f5d4, #7b61ff);
  border: none;
  border-radius: 20px;
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.checkin-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 245, 212, 0.3);
}

.checkin-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.checkin-btn .hint {
  font-size: 0.7rem;
  opacity: 0.85;
  font-weight: 400;
}

.btn-spinner-small {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>