<template>
  <div class="avatar-upload" @click="triggerUpload">
    <!-- 头像展示 -->
    <div class="avatar-wrapper" :class="{ 'has-avatar': hasAvatar }" :style="avatarStyle">
      <img v-if="hasAvatar && !previewUrl" :src="avatarUrl" :alt="name" class="avatar-img" />
      <img v-else-if="previewUrl" :src="previewUrl" alt="预览" class="avatar-img" />
      <span v-else class="avatar-placeholder">{{ initial }}</span>

      <!-- 悬浮遮罩 -->
      <div class="avatar-overlay">
        <span class="overlay-icon">📷</span>
        <span class="overlay-text">更换头像</span>
      </div>
    </div>

    <!-- 隐藏的文件选择器 -->
    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      class="file-input"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadAvatar as uploadAvatarApi } from '@/api/upload'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  size: { type: Number, default: 100 }
})

const emit = defineEmits(['avatar-updated'])

const authStore = useAuthStore()
const fileInput = ref(null)
const previewUrl = ref('')

const avatarUrl = computed(() => authStore.user.avatarUrl)
const hasAvatar = computed(() => authStore.hasAvatar)
const name = computed(() => authStore.displayName)
const initial = computed(() => authStore.displayAvatar)

const avatarStyle = computed(() => ({
  width: props.size + 'px',
  height: props.size + 'px'
}))

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  // 检查文件大小（最大5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像文件大小不能超过 5MB')
    return
  }

  // 检查文件类型（svg 可内嵌脚本，存在存储型 XSS 风险，明令禁止）
  const isSvg = file.type === 'image/svg+xml' || file.name.toLowerCase().endsWith('.svg')
  if (isSvg) {
    ElMessage.warning('不支持 SVG 格式的头像')
    return
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片格式的文件')
    return
  }

  // 预览
  const reader = new FileReader()
  reader.onload = (ev) => {
    previewUrl.value = ev.target?.result
  }
  reader.readAsDataURL(file)

  try {
    const res = await uploadAvatarApi(file)
    if (res.code === 200 && res.data?.avatarUrl) {
      authStore.updateUser({ avatarUrl: res.data.avatarUrl })
      previewUrl.value = ''
      ElMessage.success('头像上传成功')
      emit('avatar-updated', res.data.avatarUrl)
    } else {
      throw new Error(res.message || '上传失败')
    }
  } catch (e) {
    previewUrl.value = ''
    ElMessage.error(e.message || '头像上传失败')
  }

  // 清空input，允许重复选择同一文件
  fileInput.value.value = ''
}
</script>

<style lang="scss" scoped>
.avatar-upload {
  display: inline-block;
  cursor: pointer;
}

.avatar-wrapper {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;

  &:hover .avatar-overlay {
    opacity: 1;
  }

  &.has-avatar {
    border: 2px solid transparent;
    background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary)) border-box;
    -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
    mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
  }
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-placeholder {
  color: #fff;
  font-size: 1.8rem;
  font-weight: 700;
  line-height: 1;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.25s ease;
  border-radius: 50%;
}

.overlay-icon {
  font-size: 1.2rem;
}

.overlay-text {
  font-size: 0.7rem;
  color: #fff;
  font-weight: 500;
}

.file-input {
  display: none;
}
</style>
