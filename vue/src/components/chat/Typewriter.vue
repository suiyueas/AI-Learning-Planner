<template>
  <div class="typewriter" :class="{ 'is-typing': isTyping }">
    <div class="typewriter-content" v-html="displayedContent"></div>
    <span v-if="isTyping" class="cursor">|</span>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  speed: {
    type: Number,
    default: 30
  },
  autoStart: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['complete'])

const displayedContent = ref('')
const isTyping = ref(false)
let currentIndex = 0
let timer = null

// 开始打字动画
const startTyping = () => {
  if (!props.content) return

  isTyping.value = true
  currentIndex = 0
  displayedContent.value = ''

  timer = setInterval(() => {
    if (currentIndex < props.content.length) {
      // 跳过HTML标签
      if (props.content[currentIndex] === '<') {
        const closingIndex = props.content.indexOf('>', currentIndex)
        if (closingIndex !== -1) {
          displayedContent.value += props.content.substring(currentIndex, closingIndex + 1)
          currentIndex = closingIndex + 1
        } else {
          displayedContent.value += props.content[currentIndex]
          currentIndex++
        }
      } else {
        displayedContent.value += props.content[currentIndex]
        currentIndex++
      }
    } else {
      stopTyping()
      emit('complete')
    }
  }, props.speed)
}

// 停止打字动画
const stopTyping = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  isTyping.value = false
}

// 跳过打字，直接显示全部内容
const skipTyping = () => {
  stopTyping()
  displayedContent.value = props.content
  emit('complete')
}

// 监听内容变化
watch(() => props.content, (newContent) => {
  if (newContent && props.autoStart) {
    startTyping()
  }
})

onMounted(() => {
  if (props.content && props.autoStart) {
    startTyping()
  }
})

onUnmounted(() => {
  stopTyping()
})

defineExpose({
  startTyping,
  stopTyping,
  skipTyping
})
</script>

<style lang="scss" scoped>
.typewriter {
  display: inline;
  line-height: 1.6;
}

.typewriter-content {
  display: inline;
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 1.2em;
  background: #00f5d4;
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 0.8s infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}
</style>
