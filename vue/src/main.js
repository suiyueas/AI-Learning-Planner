// ============================================
// 应用入口
// ============================================

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// 样式导入
import './styles/global.scss'

// 创建应用实例
const app = createApp(App)

// 注册插件
app.use(createPinia())
app.use(router)

// 全局未捕获异常处理：防止白屏，输出可读错误
app.config.errorHandler = (err, instance, info) => {
  console.error('[全局错误]', err, info)
}

// 挂载应用
app.mount('#app')