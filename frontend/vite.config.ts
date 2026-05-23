import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/auth/, '/auth')
      },
      '/api/file': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/chat': {
        target: 'ws://localhost:8081',
        ws: true,
        rewrite: (path) => '/ws/chat'
      },
      '/ws': {
        target: 'ws://localhost:8081',
        ws: true
      },
      '/files': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
