import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // 自动导入 Vue、Vue Router、Pinia 等 API
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: false, // 禁用生成 .d.ts 文件
    }),
    // 自动注册 Element Plus 组件
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      dts: false, // 禁用生成 .d.ts 文件
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        // 注意：不要在这里注入 @use 语句，会导致循环引用
        // 变量已通过 CSS 变量导出，可直接使用 var(--xxx)
        silenceDeprecations: ['legacy-js-api', 'import'],
      },
    },
  },
  server: {
    port: 3000,
    open: true,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // 构建分包策略
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
        },
      },
    },
    // 使用esbuild压缩（不需要terser）
    minify: 'esbuild',
  },
})