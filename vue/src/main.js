// ============================================
// 应用入口
// ============================================

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// 样式导入
import './styles/global.scss'
import './styles/variables.scss'

// 创建应用实例
const app = createApp(App)

// 注册插件
app.use(createPinia())
app.use(router)

// 挂载应用
app.mount('#app')
