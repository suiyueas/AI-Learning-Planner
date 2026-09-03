<template>
  <div class="auth-page">
    <canvas ref="canvasRef" class="bg-canvas"></canvas>
    <div class="auth-wrapper">
      <Transition name="card-fade" mode="out-in">
        <!-- 登录视图 -->
        <div v-if="isLoginView" key="login" class="auth-panel">
          <!-- 左侧品牌面板 -->
          <div class="brand-panel">
            <div class="brand-content">
              <div class="brand-logo">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"></path>
                  <path d="m9 12 2 2 4-4"></path>
                </svg>
              </div>
              <h1 class="brand-name">知途</h1>
              <p class="brand-slogan">AI 辅助你走好知识之路</p>
              <div class="brand-features">
                <div class="feature-item">
                  <span class="feature-icon">🎯</span>
                  <span>智能学习路径规划</span>
                </div>
                <div class="feature-item">
                  <span class="feature-icon">🧠</span>
                  <span>RAG 知识库问答</span>
                </div>
                <div class="feature-item">
                  <span class="feature-icon">📊</span>
                  <span>AI 能力诊断分析</span>
                </div>
              </div>
            </div>
            <div class="brand-footer">v1.0 · Built with Spring AI</div>
          </div>

          <!-- 右侧表单面板 -->
          <div class="form-panel">
            <div class="form-content">
              <div class="form-header">
                <h2>欢迎回来</h2>
                <p>登录你的账号继续学习</p>
              </div>
              
              <form class="auth-form" @submit.prevent="handleLogin">
                <div class="form-group" :class="{ focused: focusedField === 'username', error: usernameError, 'shake': usernameShake }">
                  <label class="form-label">用户名 / 邮箱</label>
                  <div class="input-wrapper">
                    <User :size="18" class="input-icon" />
                    <input
                      v-model="form.username"
                      type="text"
                      placeholder="请输入用户名或邮箱"
                      class="form-input"
                      @focus="focusedField = 'username'"
                      @blur="focusedField = ''; validateUsername()"
                    />
                    <span v-if="usernameError" class="validation-icon error">
                      <XCircle :size="16" />
                    </span>
                    <span v-else-if="form.username && !usernameError" class="validation-icon success">
                      <CheckCircle :size="16" />
                    </span>
                  </div>
                  <transition name="spring">
                    <span v-if="usernameError" class="form-hint error">{{ usernameError }}</span>
                  </transition>
                </div>

                <div class="form-group" :class="{ focused: focusedField === 'password', error: passwordError, 'shake': passwordShake }">
                  <label class="form-label">密码</label>
                  <div class="input-wrapper">
                    <Lock :size="18" class="input-icon" />
                    <input
                      v-model="form.password"
                      :type="showPassword ? 'text' : 'password'"
                      placeholder="请输入密码"
                      class="form-input"
                      @focus="focusedField = 'password'"
                      @blur="focusedField = ''; validatePassword()"
                    />
                    <button type="button" class="toggle-password" tabindex="-1" @click="showPassword = !showPassword">
                      <component :is="showPassword ? EyeOff : Eye" :size="18" class="eye-icon" :class="{ active: showPassword }" />
                    </button>
                  </div>
                  <transition name="spring">
                    <span v-if="passwordError" class="form-hint error">{{ passwordError }}</span>
                  </transition>
                </div>

                <div class="form-options">
                  <label class="remember-me" @click.prevent="toggleRemember">
                    <input v-model="form.remember" type="checkbox" class="remember-checkbox" />
                    <span class="checkbox-custom">
                      <svg class="check-mark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                        <polyline points="20 6 9 17 4 12"></polyline>
                      </svg>
                    </span>
                    <span class="remember-text">记住我</span>
                  </label>
                </div>

                <transition name="spring">
                  <div v-if="errorMsg" class="error-banner">
                    <AlertCircle :size="16" />
                    <span>{{ errorMsg }}</span>
                  </div>
                </transition>

                <button type="submit" class="submit-btn" :disabled="loading" @click="handleBtnClick">
                  <span v-if="loading" class="loading-spinner"></span>
                  <span v-else>登 录</span>
                  <span ref="rippleRef" class="btn-ripple"></span>
                </button>
              </form>

              <div class="auth-footer">
                <span>还没有账号？</span>
                <router-link to="/register" class="auth-link">去注册</router-link>
              </div>
            </div>
          </div>
        </div>

        <!-- 注册视图 -->
        <div v-else key="register" class="auth-panel">
          <!-- 左侧品牌面板 -->
          <div class="brand-panel">
            <div class="brand-content">
              <div class="brand-logo">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"></path>
                  <path d="m9 12 2 2 4-4"></path>
                </svg>
              </div>
              <h1 class="brand-name">知途</h1>
              <p class="brand-slogan">开启你的 AI 学习之旅</p>
              <div class="brand-features">
                <div class="feature-item">
                  <span class="feature-icon">🎯</span>
                  <span>智能学习路径规划</span>
                </div>
                <div class="feature-item">
                  <span class="feature-icon">🧠</span>
                  <span>RAG 知识库问答</span>
                </div>
                <div class="feature-item">
                  <span class="feature-icon">📊</span>
                  <span>AI 能力诊断分析</span>
                </div>
              </div>
            </div>
            <div class="brand-footer">v1.0 · Built with Spring AI</div>
          </div>

          <!-- 右侧表单面板 -->
          <div class="form-panel">
            <div class="form-content">
              <div class="form-header">
                <h2>创建账号</h2>
                <p>开始你的学习之旅</p>
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
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Lock, Eye, EyeOff, AlertCircle, Mail, XCircle, CheckCircle } from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
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
const isLoginView = computed(() => route.path === '/login')

const focusedField = ref('')
const showPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')
const usernameError = ref('')
const passwordError = ref('')
const usernameShake = ref(false)
const passwordShake = ref(false)
const emailShake = ref(false)
const confirmShake = ref(false)

const form = reactive({
  username: '',
  password: '',
  email: '',
  confirmPassword: '',
  remember: false
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
    this.baseHue = Math.random() * 40 + 140  // 绿色色调范围 (140-180)
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
        ctx.strokeStyle = `hsla(${150 + hueShift * 0.3}, 70%, 55%, ${alpha})`  // 翡翠绿连线
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
  if (field === 'username') { usernameShake.value = true; setTimeout(() => usernameShake.value = false, 300) }
  else if (field === 'password') { passwordShake.value = true; setTimeout(() => passwordShake.value = false, 300) }
  else if (field === 'email') { emailShake.value = true; setTimeout(() => emailShake.value = false, 300) }
  else if (field === 'confirm') { confirmShake.value = true; setTimeout(() => confirmShake.value = false, 300) }
}

const toggleRemember = () => { form.remember = !form.remember }

function validateUsername() {
  if (!form.username.trim()) { usernameError.value = '请输入用户名或邮箱'; triggerShake('username'); return false }
  usernameError.value = ''
  return true
}

function validatePassword() {
  if (!form.password) { passwordError.value = '请输入密码'; triggerShake('password'); return false }
  passwordError.value = ''
  return true
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

async function handleLogin() {
  errorMsg.value = ''
  const validUser = validateUsername()
  const validPass = validatePassword()
  if (!validUser || !validPass) return

  loading.value = true
  try {
    await authStore.login({
      username: form.username.trim(),
      password: form.password
    })
    router.push('/home')
  } catch (e) {
    errorMsg.value = e.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
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

watch(isLoginView, () => {
  errorMsg.value = ''
  usernameError.value = ''
  passwordError.value = ''
  Object.keys(errors).forEach(k => errors[k] = '')
})

onMounted(() => { initCanvas(); window.addEventListener('resize', resize) })
onUnmounted(() => { cancelAnimationFrame(animationId); window.removeEventListener('resize', resize) })
</script>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: $bg-base; padding: 20px; position: relative; overflow: hidden; }

.bg-canvas { position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; }

// 背景微光粒子
.auth-page::before {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba($accent-indigo, 0.1) 0%, transparent 70%);
  top: -50px;
  right: -50px;
  animation: floatOrb1 20s ease-in-out infinite;
}
.auth-page::after {
  content: '';
  position: absolute;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba($accent-violet, 0.06) 0%, transparent 70%);
  bottom: -40px;
  left: -30px;
  animation: floatOrb2 25s ease-in-out infinite;
}
@keyframes floatOrb1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(40px, 30px) scale(1.1); }
  66% { transform: translate(-20px, -15px) scale(0.95); }
}
@keyframes floatOrb2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, -25px) scale(1.08); }
}

.auth-wrapper { width: 100%; max-width: 900px; position: relative; z-index: 1; }

.auth-panel { display: grid; grid-template-columns: 1fr 1fr; background: $bg-surface; border: 1px solid $border-default; border-radius: $radius-xl; overflow: hidden; box-shadow: $shadow-lg; animation: panelSlideIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards; opacity: 0; transform: translateY(16px); }
@keyframes panelSlideIn {
  to { opacity: 1; transform: translateY(0); }
}

// 左侧品牌面板
.brand-panel { background: linear-gradient(135deg, $accent-indigo, $accent-indigo-dark); padding: 48px 40px; display: flex; flex-direction: column; justify-content: space-between; color: white; position: relative; overflow: hidden; background-size: 200% 200%; animation: gradientShift 8s ease infinite; }
@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
.brand-panel::before { content: ''; position: absolute; inset: 0; background-image: linear-gradient(rgba(255,255,255,0.05) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.05) 1px, transparent 1px); background-size: 24px 24px; pointer-events: none; }

// 品牌面板装饰圆 - 呼吸动画
.brand-panel::after {
  content: '';
  position: absolute;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
  bottom: -40px;
  right: -40px;
  animation: breatheRing 6s ease-in-out infinite;
}
@keyframes breatheRing {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.15); opacity: 0.6; }
}

.brand-logo { width: 48px; height: 48px; background: rgba(white, 0.15); border: 1px solid rgba(white, 0.1); border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 24px; transition: all 0.3s ease; }
.brand-logo:hover { background: rgba(white, 0.22); transform: scale(1.05); }
.brand-name { font-size: 32px; font-weight: 700; margin: 0 0 8px; }
.brand-slogan { font-size: 15px; opacity: 0.8; margin: 0 0 40px; }
.brand-features { display: flex; flex-direction: column; gap: 16px; }
.feature-item { display: flex; align-items: center; gap: 12px; font-size: 14px; opacity: 0.9; transition: all 0.3s ease; padding: 8px 12px; border-radius: 8px; }
.feature-item:hover { background: rgba(255, 255, 255, 0.08); opacity: 1; transform: translateX(4px); }
.feature-icon { font-size: 20px; }
.brand-footer { font-size: 12px; opacity: 0.5; }

// 右侧表单面板
.form-panel { padding: 48px 40px; display: flex; flex-direction: column; justify-content: center; }
.form-content { width: 100%; }
.form-header { margin-bottom: 32px; }
.form-header h2 { font-size: 24px; font-weight: 700; color: $text-primary; margin: 0 0 8px; font-family: $font-display; }
.form-header p { font-size: 14px; color: $text-secondary; margin: 0; }
.auth-form { display: flex; flex-direction: column; gap: 20px; }
.form-group { display: flex; flex-direction: column; gap: 6px; position: relative; }
.form-group.shake { animation: shake 0.3s ease-in-out; }
@keyframes shake { 0%, 100% { transform: translateX(0); } 20% { transform: translateX(-6px); } 40% { transform: translateX(6px); } 60% { transform: translateX(-4px); } 80% { transform: translateX(4px); } }
.form-label { font-size: 13px; font-weight: 600; color: $text-secondary; }
.input-wrapper { display: flex; align-items: center; gap: 10px; padding: 0 14px; background: $bg-base; border: 1px solid $border-default; border-radius: $radius-md; transition: all 0.3s ease; position: relative; overflow: hidden; }
// 输入框底部渐变线
.input-wrapper::after {
  content: '';
  position: absolute;
  bottom: 0; left: 14px; right: 14px; height: 2px;
  background: linear-gradient(90deg, $accent-indigo, $accent-violet);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.4s ease;
  border-radius: 1px;
}
.input-wrapper:hover { border-color: $border-medium; }
.form-group.focused .input-wrapper { border-color: $accent-indigo; box-shadow: $glow-focus; }
.form-group.focused .input-wrapper::after { transform: scaleX(1); }
.form-group.error .input-wrapper { border-color: $color-danger; box-shadow: 0 0 0 3px rgba($color-danger, 0.1); }
.input-icon { color: $text-muted; flex-shrink: 0; transition: all 0.3s ease; transform: scale(1); }
.form-group.focused .input-icon { color: $accent-indigo; transform: scale(1.1); }
.validation-icon { flex-shrink: 0; display: flex; align-items: center; }
.validation-icon.error { color: $color-danger; animation: iconPop 0.3s ease; }
.validation-icon.success { color: $color-success; animation: iconPop 0.3s ease; }
@keyframes iconPop { 0% { transform: scale(0); } 50% { transform: scale(1.2); } 100% { transform: scale(1); } }
.form-input { flex: 1; padding: 12px 0; border: none; background: transparent; font-size: 15px; color: $text-primary; outline: none; font-family: $font-sans; }
.form-input::placeholder { color: $text-placeholder; }
.toggle-password { background: none; border: none; cursor: pointer; color: $text-muted; padding: 4px; flex-shrink: 0; transition: all 0.3s ease; display: flex; align-items: center; }
.toggle-password:hover { color: $text-secondary; }
.eye-icon { transition: all 0.3s ease; }
.eye-icon.active { color: $accent-indigo; transform: rotate(180deg); }
.form-hint { font-size: 12px; padding-left: 4px; }
.form-hint.error { color: $color-danger; }
.form-options { display: flex; align-items: center; justify-content: space-between; }
.remember-me { display: flex; align-items: center; gap: 8px; cursor: pointer; user-select: none; }
.remember-checkbox { display: none; }
.checkbox-custom { width: 18px; height: 18px; border: 1px solid $border-default; border-radius: 4px; background: $bg-base; transition: all 0.2s ease; position: relative; display: flex; align-items: center; justify-content: center; }
.check-mark { width: 12px; height: 12px; color: $text-primary; opacity: 0; transform: scale(0) rotate(-45deg); transition: all 0.2s ease; }
.remember-checkbox:checked + .checkbox-custom { background: $accent-indigo; border-color: transparent; }
.remember-checkbox:checked + .checkbox-custom .check-mark { opacity: 1; transform: scale(1) rotate(0); }
.remember-text { font-size: 13px; color: $text-secondary; }
.password-strength { display: flex; align-items: center; gap: 10px; margin-top: 2px; }
.strength-bar { flex: 1; height: 4px; background: $bg-elevated; border-radius: 2px; overflow: hidden; }
.strength-fill { height: 100%; border-radius: 2px; transition: all 0.3s ease; }
.strength-text { font-size: 12px; font-weight: 600; min-width: 2em; text-align: right; }
.error-banner { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: rgba($color-danger, 0.1); border: 1px solid rgba($color-danger, 0.2); border-radius: 10px; color: $color-danger; font-size: 13px; }
.spring-enter-active { animation: springIn 0.5s cubic-bezier(0.68, -0.55, 0.27, 1.55); }
.spring-leave-active { transition: all 0.2s ease; }
.spring-leave-to { opacity: 0; transform: translateY(-10px); }
@keyframes springIn { 0% { opacity: 0; transform: scale(0.8) translateY(-10px); } 50% { transform: scale(1.05) translateY(0); } 100% { opacity: 1; transform: scale(1) translateY(0); } }
.submit-btn { position: relative; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 14px; background: $accent-indigo; color: $text-primary; border: none; border-radius: $radius-md; font-size: 16px; font-weight: 700; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 4px 20px rgba($accent-indigo, 0.25); font-family: $font-sans; letter-spacing: 2px; overflow: hidden; }
.submit-btn:hover:not(:disabled) { background: $accent-indigo-light; transform: scale(1.02) translateY(-1px); box-shadow: 0 8px 30px rgba($accent-indigo, 0.35); }
.submit-btn:active:not(:disabled) { transform: scale(0.98); }
.submit-btn:disabled { opacity: 0.7; cursor: not-allowed; }
.btn-ripple { position: absolute; border-radius: 50%; background: rgba(255, 255, 255, 0.4); pointer-events: none; }
@keyframes rippleAnim { 0% { transform: scale(0); opacity: 1; } 100% { transform: scale(2.5); opacity: 0; } }
.loading-spinner { width: 20px; height: 20px; border: 2px solid rgba(255, 255, 255, 0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.auth-footer { text-align: center; margin-top: 24px; font-size: 14px; color: $text-secondary; }
.auth-link { color: $accent-indigo; text-decoration: none; font-weight: 600; margin-left: 4px; position: relative; transition: color 0.3s ease; }
.auth-link::after { content: ''; position: absolute; bottom: -2px; left: 0; width: 0; height: 1px; background: $accent-indigo; transition: width 0.3s ease; }
.auth-link:hover::after { width: 100%; }
.auth-link:hover { color: $accent-indigo-light; }
.card-fade-enter-active { animation: cardEnter 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
.card-fade-leave-active { animation: cardLeave 0.4s ease forwards; }
.card-fade-enter-from { opacity: 0; transform: translateY(30px); }
.card-fade-leave-to { opacity: 0; transform: translateY(-30px); }
@keyframes cardEnter { from { opacity: 0; transform: translateY(40px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes cardLeave { to { opacity: 0; transform: translateY(-20px) scale(0.98); } }
@media (max-width: 768px) {
  .auth-panel { grid-template-columns: 1fr; }
  .brand-panel { padding: 32px 24px; }
  .brand-features { display: none; }
  .form-panel { padding: 32px 24px; }
  .auth-wrapper { max-width: 400px; }
}
@media (max-width: 480px) {
  .auth-page { padding: 10px; }
  .brand-panel { padding: 24px 20px; }
  .brand-name { font-size: 28px; }
  .brand-slogan { font-size: 14px; margin-bottom: 24px; }
  .form-panel { padding: 24px 20px; }
  .form-header h2 { font-size: 22px; }
}
</style>