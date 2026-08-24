<template>
  <div class="auth-page">
    <canvas ref="canvasRef" class="bg-canvas"></canvas>
    <div class="bg-gradient"></div>
    <div class="bg-aurora">
      <div class="aurora-layer aurora-1"></div>
      <div class="aurora-layer aurora-2"></div>
      <div class="aurora-layer aurora-3"></div>
    </div>

    <div class="auth-container">
      <div class="auth-card">
        <div class="card-glow"></div>
        <div class="auth-header">
          <div class="logo-wrap breathing-glow">
            <Brain :size="32" class="logo-icon" />
          </div>
          <h1 class="auth-title">AI学习规划师</h1>
          <p class="auth-subtitle">创建账号</p>
        </div>

        <form class="auth-form" @submit.prevent="handleRegister">
          <div class="form-group" :class="{ focused: focusedField === 'username', error: errors.username, 'shake': usernameShake }">
            <label class="form-label">用户名</label>
            <div class="input-wrapper">
              <User :size="18" class="input-icon" />
              <input
                v-model="form.username"
                type="text"
                placeholder="3-50个字符，字母数字下划线"
                class="form-input"
                @focus="focusedField = 'username'"
                @blur="focusedField = ''; validateField('username')"
              />
              <span v-if="errors.username" class="validation-icon error">
                <XCircle :size="16" />
              </span>
              <span v-else-if="form.username && !errors.username && validateFieldNoError('username')" class="validation-icon success">
                <CheckCircle :size="16" />
              </span>
            </div>
            <div class="input-light-bar"></div>
            <transition name="spring">
              <span v-if="errors.username" class="form-hint error">{{ errors.username }}</span>
            </transition>
          </div>

          <div class="form-group" :class="{ focused: focusedField === 'email', error: errors.email, 'shake': emailShake }">
            <label class="form-label">邮箱</label>
            <div class="input-wrapper">
              <Mail :size="18" class="input-icon" />
              <input
                v-model="form.email"
                type="email"
                placeholder="请输入邮箱地址"
                class="form-input"
                @focus="focusedField = 'email'"
                @blur="focusedField = ''; validateField('email')"
              />
              <span v-if="errors.email" class="validation-icon error">
                <XCircle :size="16" />
              </span>
              <span v-else-if="form.email && !errors.email && validateFieldNoError('email')" class="validation-icon success">
                <CheckCircle :size="16" />
              </span>
            </div>
            <div class="input-light-bar"></div>
            <transition name="spring">
              <span v-if="errors.email" class="form-hint error">{{ errors.email }}</span>
            </transition>
          </div>

          <div class="form-group" :class="{ focused: focusedField === 'password', error: errors.password, 'shake': passwordShake }">
            <label class="form-label">密码</label>
            <div class="input-wrapper">
              <Lock :size="18" class="input-icon" />
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="至少8位，包含字母和数字"
                class="form-input"
                @focus="focusedField = 'password'"
                @blur="focusedField = ''; validateField('password')"
              />
              <button type="button" class="toggle-password" tabindex="-1" @click="showPassword = !showPassword">
                <component :is="showPassword ? EyeOff : Eye" :size="18" class="eye-icon" :class="{ active: showPassword }" />
              </button>
            </div>
            <div class="input-light-bar"></div>
            <div v-if="form.password" class="password-strength">
              <div class="strength-bar">
                <div class="strength-fill" :style="{ width: strength.percent + '%', background: strength.color }"></div>
              </div>
              <span class="strength-text" :style="{ color: strength.color }">{{ strength.label }}</span>
            </div>
            <transition name="spring">
              <span v-if="errors.password" class="form-hint error">{{ errors.password }}</span>
            </transition>
          </div>

          <div class="form-group" :class="{ focused: focusedField === 'confirm', error: errors.confirmPassword, 'shake': confirmShake }">
            <label class="form-label">确认密码</label>
            <div class="input-wrapper">
              <Lock :size="18" class="input-icon" />
              <input
                v-model="form.confirmPassword"
                :type="showPassword ? 'text' : 'password'"
                placeholder="再次输入密码"
                class="form-input"
                @focus="focusedField = 'confirm'"
                @blur="focusedField = ''; validateField('confirmPassword')"
              />
              <span v-if="errors.confirmPassword" class="validation-icon error">
                <XCircle :size="16" />
              </span>
              <span v-else-if="form.confirmPassword && !errors.confirmPassword && form.password === form.confirmPassword" class="validation-icon success">
                <CheckCircle :size="16" />
              </span>
            </div>
            <div class="input-light-bar"></div>
            <transition name="spring">
              <span v-if="errors.confirmPassword" class="form-hint error">{{ errors.confirmPassword }}</span>
            </transition>
          </div>

          <transition name="spring">
            <div v-if="errorMsg" class="error-banner">
              <AlertCircle :size="16" />
              <span>{{ errorMsg }}</span>
            </div>
          </transition>

          <button type="submit" class="submit-btn" :disabled="loading" @click="handleBtnClick">
            <span v-if="loading" class="loading-spinner"></span>
            <span v-else>注 册</span>
            <span ref="rippleRef" class="btn-ripple"></span>
          </button>
        </form>

        <div class="auth-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="auth-link">去登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Brain, User, Mail, Lock, Eye, EyeOff, AlertCircle, XCircle, CheckCircle } from 'lucide-vue-next'

const router = useRouter()
const authStore = useAuthStore()

const canvasRef = ref(null)
const rippleRef = ref(null)
let ctx = null
let width = 0, height = 0
let particles = []
let animationId = null
let hueShift = 0

const PARTICLE_COUNT = 100
const CONNECT_DIST = 150

const focusedField = ref('')
const showPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')
const usernameShake = ref(false)
const passwordShake = ref(false)
const emailShake = ref(false)
const confirmShake = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const errors = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const strength = computed(() => {
  const pwd = form.password
  if (!pwd) return { percent: 0, label: '', color: 'transparent' }

  let score = 0
  if (pwd.length >= 8) score += 25
  if (pwd.length >= 12) score += 15
  if (/[A-Z]/.test(pwd)) score += 15
  if (/[a-z]/.test(pwd)) score += 15
  if (/\d/.test(pwd)) score += 15
  if (/[^A-Za-z0-9]/.test(pwd)) score += 15

  if (score < 30) return { percent: 30, label: '弱', color: '#ff4060' }
  if (score < 60) return { percent: 60, label: '中等', color: '#f59e0b' }
  return { percent: 100, label: '强', color: '#10b981' }
})

class Particle {
  constructor() {
    this.x = Math.random() * width
    this.y = Math.random() * height
    this.vx = (Math.random() - 0.5) * 0.4
    this.vy = (Math.random() - 0.5) * 0.4
    this.radius = Math.random() * 2.5 + 1
    this.baseHue = Math.random() * 60 + 160
  }
  update() {
    this.x += this.vx
    this.y += this.vy
    if (this.x < 0 || this.x > width) this.vx *= -1
    if (this.y < 0 || this.y > height) this.vy *= -1
  }
  draw(ctx, hue) {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2)
    const gradient = ctx.createRadialGradient(this.x, this.y, 0, this.x, this.y, this.radius)
    gradient.addColorStop(0, `hsla(${this.baseHue + hue}, 90%, 65%, 0.8)`)
    gradient.addColorStop(1, `hsla(${this.baseHue + hue}, 90%, 65%, 0.2)`)
    ctx.fillStyle = gradient
    ctx.fill()
  }
}

const initCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.parentElement.getBoundingClientRect()
  width = canvas.width = rect.width
  height = canvas.height = rect.height
  ctx = canvas.getContext('2d')
  particles = Array.from({ length: PARTICLE_COUNT }, () => new Particle())
  draw()
}

const draw = () => {
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)
  hueShift += 0.2
  if (hueShift > 360) hueShift = 0

  particles.forEach(p => p.update())
  particles.forEach(p => p.draw(ctx, hueShift))

  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < CONNECT_DIST) {
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        const alpha = (1 - dist / CONNECT_DIST) * 0.4
        ctx.strokeStyle = `hsla(${185 + hueShift * 0.5}, 80%, 60%, ${alpha})`
        ctx.lineWidth = 0.8
        ctx.stroke()
      }
    }
  }
  animationId = requestAnimationFrame(draw)
}

const resize = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.parentElement.getBoundingClientRect()
  width = canvas.width = rect.width
  height = canvas.height = rect.height
}

const handleBtnClick = (e) => {
  const btn = e.currentTarget
  const ripple = document.createElement('span')
  const rect = btn.getBoundingClientRect()
  const size = Math.max(rect.width, rect.height)
  const x = e.clientX - rect.left - size / 2
  const y = e.clientY - rect.top - size / 2
  ripple.style.cssText = `
    position: absolute;
    border-radius: 50%;
    background: rgba(255,255,255,0.35);
    width: ${size}px;
    height: ${size}px;
    left: ${x}px;
    top: ${y}px;
    pointer-events: none;
    animation: rippleAnim 0.8s ease-out forwards;
    transform: scale(0);
  `
  btn.appendChild(ripple)
  setTimeout(() => ripple.remove(), 800)
}

const triggerShake = (field) => {
  if (field === 'username') {
    usernameShake.value = true
    setTimeout(() => usernameShake.value = false, 300)
  } else if (field === 'password') {
    passwordShake.value = true
    setTimeout(() => passwordShake.value = false, 300)
  } else if (field === 'email') {
    emailShake.value = true
    setTimeout(() => emailShake.value = false, 300)
  } else if (field === 'confirm') {
    confirmShake.value = true
    setTimeout(() => confirmShake.value = false, 300)
  }
}

function validateField(field) {
  switch (field) {
    case 'username':
      if (!form.username.trim()) { errors.username = '请输入用户名'; triggerShake('username'); return false }
      if (form.username.length < 3) { errors.username = '用户名至少3个字符'; triggerShake('username'); return false }
      if (form.username.length > 50) { errors.username = '用户名不超过50个字符'; triggerShake('username'); return false }
      if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(form.username)) { errors.username = '仅支持字母、数字、下划线和中文'; triggerShake('username'); return false }
      errors.username = ''
      return true

    case 'email':
      if (!form.email.trim()) { errors.email = '请输入邮箱'; triggerShake('email'); return false }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) { errors.email = '邮箱格式不正确'; triggerShake('email'); return false }
      errors.email = ''
      return true

    case 'password':
      if (!form.password) { errors.password = '请输入密码'; triggerShake('password'); return false }
      if (form.password.length < 8) { errors.password = '密码至少8位'; triggerShake('password'); return false }
      if (!/(?=.*[A-Za-z])(?=.*\d)/.test(form.password)) { errors.password = '密码必须包含字母和数字'; triggerShake('password'); return false }
      errors.password = ''
      return true

    case 'confirmPassword':
      if (!form.confirmPassword) { errors.confirmPassword = '请确认密码'; triggerShake('confirm'); return false }
      if (form.password !== form.confirmPassword) { errors.confirmPassword = '两次密码不一致'; triggerShake('confirm'); return false }
      errors.confirmPassword = ''
      return true
  }
  return true
}

function validateFieldNoError(field) {
  switch (field) {
    case 'username':
      if (!form.username.trim()) return false
      if (form.username.length < 3) return false
      if (form.username.length > 50) return false
      if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(form.username)) return false
      return true
    case 'email':
      if (!form.email.trim()) return false
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return false
      return true
    default:
      return false
  }
}

function validateAll() {
  const v1 = validateField('username')
  const v2 = validateField('email')
  const v3 = validateField('password')
  const v4 = validateField('confirmPassword')
  return v1 && v2 && v3 && v4
}

async function handleRegister() {
  errorMsg.value = ''
  if (!validateAll()) return

  loading.value = true
  try {
    await authStore.register({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword
    })
    router.push('/login')
  } catch (e) {
    errorMsg.value = e.message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initCanvas()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
@use '../styles/variables' as *;
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-primary;
  position: relative;
  overflow: hidden;
}

.bg-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.bg-gradient {
  position: fixed;
  inset: 0;
  background: linear-gradient(135deg, rgba(74, 144, 249, 0.03) 0%, rgba(123, 104, 238, 0.03) 50%, rgba($accent-primary, 0.03) 100%);
  z-index: 0;
  pointer-events: none;
  animation: gradientFlow 15s ease-in-out infinite;
}

@keyframes gradientFlow {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.bg-aurora {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.aurora-layer {
  position: absolute;
  border-radius: 50%;
  filter: blur(140px);
  opacity: 0.8;
}

.aurora-1 {
  width: 600px;
  height: 600px;
  top: -150px;
  right: -100px;
  background: radial-gradient(circle, rgba($accent-primary, 0.12) 0%, transparent 70%);
  animation: auroraFloat 8s ease-in-out infinite;
}

.aurora-2 {
  width: 500px;
  height: 500px;
  bottom: -120px;
  left: -80px;
  background: radial-gradient(circle, rgba(123, 97, 255, 0.10) 0%, transparent 70%);
  animation: auroraFloat 8s ease-in-out infinite 2s;
}

.aurora-3 {
  width: 300px;
  height: 300px;
  top: 40%;
  left: 30%;
  background: radial-gradient(circle, rgba(58, 134, 255, 0.06) 0%, transparent 70%);
  animation: auroraFloat 8s ease-in-out infinite 4s;
}

@keyframes auroraFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -20px) scale(1.05); }
  66% { transform: translate(-20px, 15px) scale(0.95); }
}

.auth-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.auth-card {
  position: relative;
  background: rgba($accent-secondary, 0.06);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba($accent-secondary, 0.1);
  border-radius: 20px;
  padding: 40px 32px;
  box-shadow: 0 16px 64px rgba(0, 0, 0, 0.5), 0 0 80px rgba($accent-primary, 0.04);
  overflow: hidden;
  animation: cardSlideIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0;
  transform: translateY(20px);
}

@keyframes cardSlideIn {
  to { opacity: 1; transform: translateY(0); }
}

.card-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.03), transparent);
  animation: cardSweep 8s ease-in-out infinite;
  pointer-events: none;
}

@keyframes cardSweep {
  0%, 100% { left: -100%; }
  50% { left: 100%; }
}

.auth-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo-wrap {
  width: 68px;
  height: 68px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, rgba($accent-primary, 0.15), rgba(123, 97, 255, 0.15));
  border: 1px solid rgba($accent-primary, 0.2);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 30px rgba($accent-primary, 0.1);
}

.logo-icon {
  color: $accent-primary;
  filter: drop-shadow(0 0 8px rgba($accent-primary, 0.4));
}

.breathing-glow {
  animation: breathingGlow 3s ease-in-out infinite;
}

@keyframes breathingGlow {
  0%, 100% { box-shadow: 0 0 20px rgba($accent-primary, 0.1), 0 0 40px rgba($accent-primary, 0.05); }
  50% { box-shadow: 0 0 30px rgba($accent-primary, 0.2), 0 0 60px rgba($accent-primary, 0.1); }
}

.auth-title {
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 8px;
  font-family: 'Inter', -apple-system, system-ui, sans-serif;
  animation: titleGlow 3s ease-in-out infinite;
}

@keyframes titleGlow {
  0%, 100% { text-shadow: 0 0 10px rgba($accent-primary, 0.3); }
  50% { text-shadow: 0 0 20px rgba($accent-primary, 0.5), 0 0 30px rgba(123, 97, 255, 0.3); }
}

.auth-subtitle {
  font-size: 15px;
  color: #a0a0c8;
  margin: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  position: relative;
}

.form-group.shake {
  animation: shake 0.3s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: #a0a0c8;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  background: rgba($accent-secondary, 0.06);
  border: 1px solid rgba($accent-secondary, 0.1);
  border-radius: 12px;
  transition: all 0.3s ease;
  position: relative;
}

.input-wrapper:hover {
  border-color: rgba($accent-secondary, 0.18);
}

.form-group.focused .input-wrapper {
  border-color: $accent-primary;
  box-shadow: 0 0 0 3px rgba($accent-primary, 0.1), 0 0 20px rgba($accent-primary, 0.05);
}

.form-group.error .input-wrapper {
  border-color: #ff4060;
  box-shadow: 0 0 0 3px rgba(255, 64, 96, 0.1);
}

.input-icon {
  color: $text-muted;
  flex-shrink: 0;
  transition: all 0.3s ease;
  transform: scale(1);
}

.form-group.focused .input-icon {
  color: $accent-primary;
  transform: scale(1.1);
}

.validation-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.validation-icon.error {
  color: #ff4060;
  animation: iconPop 0.3s ease;
}

.validation-icon.success {
  color: $accent-emerald;
  animation: iconPop 0.3s ease;
}

@keyframes iconPop {
  0% { transform: scale(0); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.form-input {
  flex: 1;
  padding: 12px 0;
  border: none;
  background: transparent;
  font-size: 15px;
  color: $text-primary;
  outline: none;
  font-family: 'Inter', -apple-system, system-ui, sans-serif;
}

.form-input::placeholder {
  color: #8888a8;
}

.input-light-bar {
  position: absolute;
  bottom: 0;
  left: 14px;
  right: 14px;
  height: 2px;
  background: $accent-primary;
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.4s ease;
  border-radius: 1px;
}

.form-group.focused .input-light-bar {
  transform: scaleX(1);
}

.toggle-password {
  background: none;
  border: none;
  cursor: pointer;
  color: #8888a8;
  padding: 4px;
  flex-shrink: 0;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
}

.toggle-password:hover {
  color: #a0a0c8;
}

.eye-icon {
  transition: all 0.3s ease;
}

.eye-icon.active {
  color: $accent-primary;
  transform: rotate(180deg);
}

.form-hint {
  font-size: 12px;
  padding-left: 4px;
}

.form-hint.error {
  color: #ff4060;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 2px;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background: rgba($accent-secondary, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.strength-text {
  font-size: 12px;
  font-weight: 600;
  min-width: 2em;
  text-align: right;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(255, 64, 96, 0.1);
  border: 1px solid rgba(255, 64, 96, 0.2);
  border-radius: 10px;
  color: #ff4060;
  font-size: 13px;
}

.spring-enter-active {
  animation: springIn 0.5s cubic-bezier(0.68, -0.55, 0.27, 1.55);
}

.spring-leave-active {
  transition: all 0.2s ease;
}

.spring-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@keyframes springIn {
  0% { opacity: 0; transform: scale(0.8) translateY(-10px); }
  50% { transform: scale(1.05) translateY(0); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}

.submit-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px;
  background: linear-gradient(135deg, $accent-primary, $accent-purple);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba($accent-primary, 0.25);
  font-family: 'Inter', -apple-system, system-ui, sans-serif;
  letter-spacing: 2px;
  overflow: hidden;
}

.submit-btn:hover:not(:disabled) {
  transform: scale(1.02) translateY(-1px);
  box-shadow: 0 8px 30px rgba($accent-primary, 0.35), 0 0 40px rgba(123, 97, 255, 0.15);
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-ripple {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  pointer-events: none;
}

@keyframes rippleAnim {
  0% { transform: scale(0); opacity: 1; }
  100% { transform: scale(2.5); opacity: 0; }
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #a0a0c8;
}

.auth-link {
  color: $accent-primary;
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
  position: relative;
  transition: color 0.3s ease;
}

.auth-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 1px;
  background: $accent-primary;
  transition: width 0.3s ease;
}

.auth-link:hover::after {
  width: 100%;
}

.auth-link:hover {
  color: #3affd4;
}

@media (max-width: 480px) {
  .auth-card {
    padding: 32px 20px;
  }
  .auth-title {
    font-size: 24px;
  }
}
</style>