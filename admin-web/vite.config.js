import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/* 后端地址：默认 8080；联调时可用 XH_API_TARGET 指向另一个实例，交付态不受影响 */
const API_TARGET = process.env.XH_API_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '127.0.0.1',
    port: 9100,
    strictPort: true,
    proxy: {
      '/api': {
        target: API_TARGET,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/admin')
      },
      // 开发态（9100）也要能收 WebSocket 提醒；交付态由 nginx 的 location /ws/ 代理
      '/ws': {
        target: API_TARGET,
        ws: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})
