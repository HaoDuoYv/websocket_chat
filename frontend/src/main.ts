import { createApp } from 'vue'
import { createPinia } from 'pinia'
import axios from 'axios'
import App from './App.vue'
import router from './router'
import './style.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

// JWT 请求拦截器
axios.interceptors.request.use((config) => {
  const userData = localStorage.getItem('user')
  if (userData) {
    try {
      const user = JSON.parse(userData)
      if (user.token) {
        config.headers.Authorization = `Bearer ${user.token}`
      }
    } catch {
      // ignore
    }
  }
  return config
})

// 401/403 响应拦截器
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || ''
    const isAdminPath = url.includes('/admin/')

    if (error.response?.status === 401) {
      if (!url.includes('/auth/login') && !url.includes('/auth/register') && !isAdminPath) {
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
    }

    // 封禁用户被拒绝访问
    if (error.response?.status === 403 && !isAdminPath) {
      const message = error.response?.data?.message || '账号已被封禁'
      localStorage.removeItem('user')
      window.location.href = '/login?banned=' + encodeURIComponent(message)
    }

    return Promise.reject(error)
  }
)

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.mount('#app')
